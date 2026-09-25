-- Pruebas del Auth Hook de fuerza bruta · EN-1510
--
-- Comprueban la implementacion SQL contra los MISMOS casos que BruteForcePolicyTest.kt.
-- Esa correspondencia es lo que mantiene honesta la duplicacion: si alguien cambia un umbral
-- en un lado y no en el otro, esta prueba falla.
--
-- Se ejecutan con: python tools/run_sql_tests.py   (levanta Postgres en Docker)

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------- utilidades
create or replace function app.test_reset(p_hash text)
returns void language sql as $$
    delete from auth_attempt where account_hash = p_hash;
$$;

create or replace function app.test_seed(p_hash text, p_failures int, p_spread_seconds int default 60)
returns void language plpgsql as $$
declare i int;
begin
    for i in 1..p_failures loop
        insert into auth_attempt (account_hash, succeeded, attempted_at)
        values (p_hash, false, now() - make_interval(secs => p_spread_seconds - i));
    end loop;
end;
$$;

create or replace function app.test_assert(p_name text, p_actual text, p_expected text)
returns void language plpgsql as $$
begin
    if p_actual is distinct from p_expected then
        raise exception 'FALLA | % | esperado=% obtenido=%', p_name, p_expected, p_actual;
    end if;
    raise notice 'ok   | %', p_name;
end;
$$;

-- ---------------------------------------------------------------- casos
do $$
declare
    h text := 'cuenta-de-prueba';
    g text;
begin
    -- sin intentos previos
    perform app.test_reset(h);
    g := app.bf_evaluate(h)->>'gate';
    perform app.test_assert('sin intentos previos se permite entrar', g, 'ALLOW');

    -- primer fallo: no espera
    perform app.test_reset(h);
    perform app.test_seed(h, 1);
    perform app.test_assert('el primer fallo no hace esperar',
        (app.bf_evaluate(h)->>'millis'), '0');

    -- retroceso exponencial
    perform app.test_assert('backoff 2 fallos', app.bf_backoff_millis(2)::text, '1000');
    perform app.test_assert('backoff 3 fallos', app.bf_backoff_millis(3)::text, '2000');
    perform app.test_assert('backoff 4 fallos', app.bf_backoff_millis(4)::text, '4000');
    perform app.test_assert('backoff 5 fallos', app.bf_backoff_millis(5)::text, '8000');
    perform app.test_assert('el backoff tiene tope', app.bf_backoff_millis(50)::text, '30000');

    -- desafio a partir del quinto
    perform app.test_reset(h);
    perform app.test_seed(h, 5);
    perform app.test_assert('cinco fallos exigen desafio',
        app.bf_evaluate(h)->>'gate', 'CHALLENGE');

    perform app.test_reset(h);
    perform app.test_seed(h, 9);
    perform app.test_assert('nueve fallos siguen en desafio',
        app.bf_evaluate(h)->>'gate', 'CHALLENGE');

    -- bloqueo al decimo
    perform app.test_reset(h);
    perform app.test_seed(h, 10);
    perform app.test_assert('diez fallos bloquean la cuenta',
        app.bf_evaluate(h)->>'gate', 'LOCKED');

    -- a 14 minutos los fallos siguen en la ventana: el bloqueo sigue vigente
    perform app.test_reset(h);
    insert into auth_attempt (account_hash, succeeded, attempted_at)
    select h, false, now() - interval '14 minutes' from generate_series(1, 10);
    perform app.test_assert('dentro de la ventana el bloqueo sigue vigente',
        app.bf_evaluate(h)->>'gate', 'LOCKED');

    -- a 16 minutos ya salieron: el bloqueo se levanta solo, sin trabajo programado
    perform app.test_reset(h);
    insert into auth_attempt (account_hash, succeeded, attempted_at)
    select h, false, now() - interval '16 minutes' from generate_series(1, 10);
    perform app.test_assert('el bloqueo se levanta al salir los fallos de la ventana',
        app.bf_evaluate(h)->>'gate', 'ALLOW');

    -- un acceso correcto limpia los fallos
    perform app.test_reset(h);
    perform app.test_seed(h, 7, 120);
    insert into auth_attempt (account_hash, succeeded, attempted_at)
    values (h, true, now() - interval '5 seconds');
    perform app.test_assert('un acceso correcto limpia los fallos previos',
        app.bf_evaluate(h)->>'gate', 'ALLOW');

    -- fuera de la ventana no cuenta
    perform app.test_reset(h);
    insert into auth_attempt (account_hash, succeeded, attempted_at)
    select h, false, now() - interval '2 hours' from generate_series(1, 12);
    perform app.test_assert('los fallos fuera de la ventana no cuentan',
        app.bf_evaluate(h)->>'gate', 'ALLOW');

    -- aislamiento entre cuentas
    perform app.test_reset(h);
    perform app.test_reset('otra-cuenta');
    perform app.test_seed(h, 10);
    perform app.test_assert('los fallos de una cuenta no afectan a otra',
        app.bf_evaluate('otra-cuenta')->>'gate', 'ALLOW');

    perform app.test_reset(h);
    perform app.test_reset('otra-cuenta');
    raise notice '---';
    raise notice 'Todas las pruebas del hook pasaron.';
end;
$$;

-- ---------------------------------------------------------------- constantes compartidas
--
-- Las cifras de app.bf_config deben coincidir con BruteForcePolicy.kt. Si divergen, las dos
-- implementaciones dejan de ser la misma regla y el hook protege algo distinto de lo probado.
do $$
declare cfg record;
begin
    select * into cfg from app.bf_config;
    perform app.test_assert('umbral de bloqueo coincide con Kotlin', cfg.lockout_threshold::text, '10');
    perform app.test_assert('ventana coincide con Kotlin', extract(epoch from cfg.window_size)::int::text, '900');
    perform app.test_assert('duracion de bloqueo coincide con Kotlin', extract(epoch from cfg.lockout_duration)::int::text, '900');
    perform app.test_assert('tope de backoff coincide con Kotlin', cfg.max_backoff_millis::text, '30000');
    perform app.test_assert('umbral de desafio coincide con Kotlin', cfg.challenge_threshold::text, '5');
end;
$$;
