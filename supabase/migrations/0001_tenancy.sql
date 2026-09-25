-- 0001 · Tenancy y el contexto de aislamiento
--
-- ADR-0002: el tenant_id existe desde el Sprint 0 aunque en la Fase 1 haya un solo tenant.
-- Anadirlo despues obliga a migrar todas las tablas y reescribir los repositorios.
--
-- IMPORTANTE sobre la Fase 1. La app movil habla con Supabase DIRECTAMENTE usando la clave
-- anon, que es publica y va dentro del APK. Eso significa que aqui el RLS no es una red de
-- seguridad por si la aplicacion se equivoca: ES la seguridad. Cualquiera puede extraer la
-- clave del APK y consultar la base. Lo unico que lo separa de ver datos ajenos son estas
-- politicas. En la Fase 2, con core-api de por medio, el RLS vuelve a ser la segunda capa.

create extension if not exists "uuid-ossp";
create extension if not exists "citext";

create schema if not exists app;

-- ---------------------------------------------------------------- contexto de tenant
--
-- RN-009: el tenant efectivo se deriva del contexto autenticado, nunca de un parametro
-- que envie el cliente. Esta funcion resuelve las dos formas en que llega segun la fase:
--   Fase 1  Supabase  -> claim tenant_id dentro del JWT
--   Fase 2  core-api  -> SET LOCAL app.tenant_id al abrir la transaccion
-- Si no hay ninguno, devuelve NULL y las politicas no casan con nada: falla cerrado.
create or replace function app.current_tenant_id()
returns uuid
language sql
stable
as $$
  select coalesce(
    nullif(current_setting('app.tenant_id', true), '')::uuid,
    (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'tenant_id')::uuid
  );
$$;

comment on function app.current_tenant_id() is
  'RN-009. Tenant derivado del contexto autenticado. Nunca de un parametro del cliente.';

-- ---------------------------------------------------------------- tenant
create table if not exists tenant (
    id            uuid primary key default uuid_generate_v4(),
    legal_name    text        not null,
    display_name  text        not null,
    tax_id        text        not null,
    country_code  char(2)     not null,
    currency      char(3)     not null,
    subdomain     citext      not null unique,
    status        text        not null default 'ACTIVE'
        check (status in ('PROVISIONING','PROVISIONING_FAILED','ACTIVE',
                          'SUSPENDED','PENDING_DELETION','PURGED')),
    purge_after   timestamptz,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (country_code, tax_id)
);

-- El propio tenant se filtra por su id, no por una columna tenant_id.
alter table tenant enable row level security;
alter table tenant force row level security;

-- FORCE es indispensable: sin el, el rol propietario de la tabla ignora la politica,
-- y ese es justamente el rol con el que suelen correr las aplicaciones mal configuradas.

create policy tenant_self_read on tenant
    for select
    using (id = app.current_tenant_id());

-- ---------------------------------------------------------------- disparador de updated_at
create or replace function app.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger tenant_touch before update on tenant
    for each row execute function app.touch_updated_at();
