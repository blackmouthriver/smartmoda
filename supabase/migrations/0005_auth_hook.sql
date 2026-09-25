-- 0005 · Auth Hook de verificacion de contrasena
--
-- EN-1510. Conecta BruteForcePolicy al flujo real de autenticacion.
--
-- POR QUE AQUI Y NO EN EL CLIENTE. Una defensa aplicada en la aplicacion no defiende de nada:
-- quien ataca llama a la API directamente y nunca ejecuta el cliente. Esta funcion corre
-- DENTRO de Supabase Auth, en cada verificacion de contrasena, antes de conceder la sesion.
--
-- DEUDA CONOCIDA. Esto duplica en SQL la logica de shared-domain/auth/BruteForcePolicy.kt.
-- Dos implementaciones de la misma regla pueden divergir, y eso es un riesgo real. Se asume
-- por tres razones:
--   1. es la unica forma de tener la defensa en el servidor durante la Fase 1
--   2. la logica es pequena y sus constantes estan declaradas en un solo sitio (app.bf_config)
--   3. supabase/tests/ compara ambas con los mismos casos, y CI lo ejecuta
-- En el Sprint 6, con core-api, esta funcion se retira y vuelve a haber una sola.

-- ---------------------------------------------------------------- constantes
--
-- Una vista y no constantes repartidas por el cuerpo de la funcion: asi el valor que usa la
-- SQL y el que usa Kotlin se pueden comparar en una prueba, en vez de confiar en que alguien
-- actualice los dos.
create or replace view app.bf_config as
select
    10                      as lockout_threshold,
    interval '15 minutes'   as window_size,
    interval '15 minutes'   as lockout_duration,
    30000                   as max_backoff_millis,
    5                       as challenge_threshold;

comment on view app.bf_config is
  'EN-1510. Debe coincidir con BruteForcePolicy.kt. supabase/tests/ lo verifica.';

-- ---------------------------------------------------------------- retroceso
create or replace function app.bf_backoff_millis(consecutive_failures int)
returns bigint
language sql
immutable
as $$
    -- 0, 1, 2, 4, 8... segundos con tope. El primer fallo no espera: equivocarse de tecla una
    -- vez es normal, y castigarlo estorba al usuario legitimo sin frenar a un atacante.
    select case
        when consecutive_failures <= 1 then 0
        else least(
            (1000::bigint << least(consecutive_failures - 2, 20)),
            (select max_backoff_millis from app.bf_config)
        )
    end;
$$;

-- ---------------------------------------------------------------- decision
--
-- Devuelve el estado de la cuenta segun su historial reciente. Se separa de la funcion del
-- hook para poder probarla sin fabricar el evento completo de Supabase.
create or replace function app.bf_evaluate(p_account_hash text, p_now timestamptz default now())
returns jsonb
language plpgsql
stable
as $$
declare
    cfg            record;
    consecutive    int;
    last_failure   timestamptz;
    locked_until   timestamptz;
begin
    select * into cfg from app.bf_config;

    -- Fallos consecutivos desde el ultimo acceso correcto, dentro de la ventana.
    -- Un acceso correcto limpia la cuenta: el usuario demostro ser quien dice.
    select count(*), max(attempted_at)
      into consecutive, last_failure
      from auth_attempt a
     where a.account_hash = p_account_hash
       and a.attempted_at > p_now - cfg.window_size
       and a.attempted_at > coalesce(
             (select max(s.attempted_at) from auth_attempt s
               where s.account_hash = p_account_hash
                 and s.succeeded
                 and s.attempted_at > p_now - cfg.window_size),
             '-infinity'::timestamptz)
       and not a.succeeded;

    if consecutive = 0 then
        return jsonb_build_object('gate', 'ALLOW', 'failures', 0);
    end if;

    if consecutive >= cfg.lockout_threshold then
        -- El bloqueo se levanta solo: window_size y lockout_duration son iguales, asi que
        -- cuando expira el bloqueo los fallos ya salieron de la ventana y `consecutive` cae
        -- a cero por la propia consulta. No hace falta una rama de expiracion, y si se
        -- escribiera seria codigo inalcanzable.
        --
        -- `until` se devuelve igualmente porque es informacion util para el usuario: le dice
        -- cuando puede reintentar en lugar de dejarlo adivinando.
        locked_until := last_failure + cfg.lockout_duration;
        return jsonb_build_object(
            'gate', 'LOCKED', 'failures', consecutive,
            'until', to_char(locked_until, 'YYYY-MM-DD"T"HH24:MI:SSOF'));
    end if;

    if consecutive >= cfg.challenge_threshold then
        return jsonb_build_object('gate', 'CHALLENGE', 'failures', consecutive);
    end if;

    return jsonb_build_object(
        'gate', 'DELAY', 'failures', consecutive,
        'millis', app.bf_backoff_millis(consecutive));
end;
$$;

-- ---------------------------------------------------------------- hook
--
-- Contrato de Supabase: recibe { user_id, valid } y devuelve { decision, message }.
-- Se registra en Authentication > Hooks > Password Verification Attempt.
create or replace function app.password_verification_hook(event jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, app
as $$
declare
    v_user_id  uuid   := (event->>'user_id')::uuid;
    v_valid    boolean := coalesce((event->>'valid')::boolean, false);
    v_hash     text;
    v_decision jsonb;
    v_gate     text;
begin
    -- Hash del identificador, nunca el correo. Esta tabla la podria leer quien logre una
    -- inyeccion, y una lista de correos registrados ya es informacion aprovechable.
    v_hash := encode(digest(v_user_id::text, 'sha256'), 'hex');

    insert into auth_attempt (account_hash, succeeded) values (v_hash, v_valid);

    -- Un acceso correcto pasa siempre: si la contrasena es buena, el usuario es quien dice.
    if v_valid then
        return jsonb_build_object('decision', 'continue');
    end if;

    v_decision := app.bf_evaluate(v_hash);
    v_gate := v_decision->>'gate';

    if v_gate = 'LOCKED' then
        return jsonb_build_object(
            'decision', 'reject',
            -- Mensaje deliberadamente igual al de credencial incorrecta. Decir "cuenta
            -- bloqueada" confirmaria que la cuenta existe, que es la fuga que la capa 6 de
            -- docs/12 evita: las respuestas deben ser indistinguibles.
            'message', 'No pudimos iniciar sesion. Revisa tus datos e intenta de nuevo.');
    end if;

    return jsonb_build_object('decision', 'continue');
end;
$$;

comment on function app.password_verification_hook(jsonb) is
  'EN-1510. Registrar en Authentication > Hooks > Password Verification Attempt.';

-- ---------------------------------------------------------------- permisos
--
-- El hook lo invoca supabase_auth_admin, no el usuario. Se le concede lo justo y se revoca
-- de todos los demas: una funcion SECURITY DEFINER ejecutable por cualquiera seria una via
-- para escribir en auth_attempt a voluntad y falsear el historial.
grant usage on schema app to supabase_auth_admin;
grant execute on function app.password_verification_hook(jsonb) to supabase_auth_admin;
grant select, insert on table auth_attempt to supabase_auth_admin;
grant usage, select on sequence auth_attempt_id_seq to supabase_auth_admin;
grant select on app.bf_config to supabase_auth_admin;

revoke execute on function app.password_verification_hook(jsonb) from authenticated, anon, public;
