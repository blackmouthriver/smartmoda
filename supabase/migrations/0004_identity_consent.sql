-- 0004 · Identidad, consentimiento y defensa de acceso
--
-- Sprint 1. US-0102, US-0104, US-0105 y EN-1510.
--
-- Nota de modelo: el usuario consumidor NO pertenece a un tenant. Se registra una vez en la
-- plataforma y puede comprar en muchas tiendas, como en cualquier marketplace (doc 10 §3).
-- Lo que pertenece al tenant es su INTERACCION con ese catalogo. Consecuencia practica: una
-- empresa nunca ve la lista de usuarios de la plataforma, solo agregados de quienes
-- interactuaron con ella.

-- ---------------------------------------------------------------- perfil de cuenta
--
-- Extiende auth.users de Supabase con lo que es del negocio. Las credenciales viven en
-- auth.users y no se tocan desde aqui.
create table if not exists app_user (
    id                uuid primary key references auth.users(id) on delete cascade,
    display_name      text,
    locale            text        not null default 'es-CO',
    unit_system       text        not null default 'METRIC'
        check (unit_system in ('METRIC','IMPERIAL')),
    -- RN-022 y ADR-0013. Se declara, no se infiere: no hay forma fiable de deducir la edad
    -- y suponerla mal es exactamente el error que no se puede permitir.
    is_adult          boolean     not null,
    mfa_enrolled_at   timestamptz,
    status            text        not null default 'ACTIVE'
        check (status in ('ACTIVE','SUSPENDED','PENDING_DELETION')),
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    deleted_at        timestamptz
);

alter table app_user enable row level security;
alter table app_user force row level security;

-- Cada quien ve y edita lo suyo. Ningun rol administrativo aparece aqui: los datos de un
-- usuario final no son visibles para una empresa.
create policy app_user_self on app_user
    using (id = auth.uid())
    with check (id = auth.uid());

create trigger app_user_touch before update on app_user
    for each row execute function app.touch_updated_at();

-- ---------------------------------------------------------------- consentimiento
--
-- US-0105. Granular por finalidad, versionado y revocable.
--
-- Una fila por otorgamiento, nunca un UPDATE: el historial ES el registro. Si se sobrescribiera,
-- se perderia la prueba de que en su momento hubo un consentimiento valido, y uno que no se
-- puede demostrar no existe a efectos practicos.
create table if not exists consent (
    id             uuid primary key default uuid_generate_v4(),
    user_id        uuid        not null references app_user(id) on delete cascade,
    purpose        text        not null
        check (purpose in ('BODY_IMAGE','BODY_MEASUREMENT','FACE_IMAGE',
                           'MARKETING','OPTIONAL_ANALYTICS','SHARE_ASSETS')),
    policy_version text        not null,
    granted        boolean     not null,
    granted_at     timestamptz not null default now(),
    revoked_at     timestamptz,
    -- Evidencia: texto literal mostrado, IP en hash y dispositivo. Sin esto el consentimiento
    -- no es demostrable. La IP va en hash porque para probar el consentimiento basta con poder
    -- comparar, no con conservar la direccion.
    evidence       jsonb       not null,
    created_at     timestamptz not null default now()
);

alter table consent enable row level security;
alter table consent force row level security;

create policy consent_self_read on consent
    for select using (user_id = auth.uid());

-- Solo insercion. Revocar es insertar una fila nueva, no editar la anterior.
create policy consent_self_insert on consent
    for insert with check (user_id = auth.uid());

create index if not exists consent_lookup_idx
    on consent (user_id, purpose, granted_at desc);

-- RN-032 aplicado al consentimiento: el registro no se edita ni se borra mientras dure su
-- retencion legal, ni siquiera por su titular. Borrar la prueba del consentimiento es
-- perjudicar a quien tendria que demostrarlo.
create rule consent_no_update as on update to consent do instead nothing;
create rule consent_no_delete as on delete to consent do instead nothing;

-- ---------------------------------------------------------------- borrado tras revocar
--
-- RN-021: revocar obliga a borrar o anonimizar lo derivado de esa finalidad en 72 horas.
-- La cola hace el plazo verificable: se puede consultar que hay pendiente y cuanto lleva.
create table if not exists erasure_task (
    id           uuid primary key default uuid_generate_v4(),
    user_id      uuid        not null references app_user(id) on delete cascade,
    purpose      text        not null,
    dataset      text        not null,
    due_at       timestamptz not null,
    completed_at timestamptz,
    created_at   timestamptz not null default now()
);

alter table erasure_task enable row level security;
alter table erasure_task force row level security;
create policy erasure_task_self on erasure_task
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create index if not exists erasure_pending_idx
    on erasure_task (due_at)
    where completed_at is null;

-- ---------------------------------------------------------------- intentos de acceso
--
-- EN-1510. La politica vive en shared-domain (BruteForcePolicy) para que la misma regla
-- gobierne Supabase ahora y core-api despues. Aqui esta solo el almacen.
--
-- IMPORTANTE: mientras esta tabla no este conectada a un Auth Hook de Supabase, quien
-- protege el endpoint son los limites propios de Supabase Auth, no esto. Ver supabase/README.md.
create table if not exists auth_attempt (
    id           bigserial primary key,
    -- Hash del identificador, no el correo. Esta tabla la puede leer un atacante que logre
    -- una inyeccion, y una lista de correos registrados ya es informacion aprovechable.
    account_hash text        not null,
    succeeded    boolean     not null,
    ip_hash      text,
    attempted_at timestamptz not null default now()
);

alter table auth_attempt enable row level security;
alter table auth_attempt force row level security;

-- Nadie la lee desde el cliente. Solo la usa el hook del servidor, que corre con permisos
-- elevados. Sin politica de SELECT, un cliente no obtiene nada.
create policy auth_attempt_no_client_read on auth_attempt
    for select using (false);

create index if not exists auth_attempt_window_idx
    on auth_attempt (account_hash, attempted_at desc);

-- Los intentos solo interesan dentro de la ventana de 15 minutos; conservarlos mas seria
-- guardar un historial de accesos sin finalidad. Se purgan a las 24 horas.
create or replace function app.purge_old_auth_attempts()
returns void
language sql
as $$
    delete from auth_attempt where attempted_at < now() - interval '24 hours';
$$;
