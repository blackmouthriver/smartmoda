-- 0002 · Organizacion comercial y catalogo
--
-- Toda tabla de negocio nace con tenant_id NOT NULL y politica RLS. No es una convencion
-- que se recuerde: tools/check_migrations.py lo verifica en CI y rompe el build (AUT-22).

-- ---------------------------------------------------------------- helper de politicas
--
-- Las cuatro politicas de cada tabla son identicas salvo el nombre. Escribirlas a mano en
-- veinte tablas garantiza que alguna se quede sin WITH CHECK, que es el error silencioso:
-- se puede leer bien pero insertar filas en otro tenant.
create or replace function app.apply_tenant_rls(target regclass)
returns void
language plpgsql
as $$
declare
    t text := target::text;
begin
    execute format('alter table %s enable row level security', t);
    execute format('alter table %s force row level security', t);
    execute format(
        'create policy tenant_isolation on %s using (tenant_id = app.current_tenant_id()) '
        || 'with check (tenant_id = app.current_tenant_id())', t);
end;
$$;

comment on function app.apply_tenant_rls(regclass) is
  'RN-009 y RN-025. Aplica aislamiento por tenant con USING y WITH CHECK a la vez.';

-- ---------------------------------------------------------------- marca
create table if not exists brand (
    id          uuid primary key default uuid_generate_v4(),
    tenant_id   uuid        not null references tenant(id) on delete cascade,
    name        text        not null,
    slug        citext      not null,
    logo_url    text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (tenant_id, slug)
);
select app.apply_tenant_rls('brand');

-- ---------------------------------------------------------------- tienda o sucursal
--
-- US-0410: la jerarquia completa es empresa -> marca -> tienda -> seccion -> ubicacion,
-- pero un negocio pequeno no esta obligado a llenarla. Por eso brand_id es opcional.
create table if not exists store (
    id             uuid primary key default uuid_generate_v4(),
    tenant_id      uuid        not null references tenant(id) on delete cascade,
    brand_id       uuid        references brand(id) on delete set null,
    code           text        not null,
    name           text        not null,
    address        text,
    city           text,
    latitude       numeric(9,6),
    longitude      numeric(9,6),
    opening_hours  jsonb,
    allows_reserve boolean     not null default false,
    allows_pickup  boolean     not null default false,
    is_active      boolean     not null default true,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (tenant_id, code)
);
select app.apply_tenant_rls('store');

-- ---------------------------------------------------------------- taxonomia
--
-- US-0413: cada empresa define sus categorias y colores. tenant_id nullable significa
-- "categoria canonica de la plataforma", compartida por todos.
create table if not exists category (
    id            uuid primary key default uuid_generate_v4(),
    tenant_id     uuid references tenant(id) on delete cascade,
    parent_id     uuid references category(id) on delete cascade,
    name          text not null,
    canonical_key text,
    level         int  not null check (level between 1 and 3),
    created_at    timestamptz not null default now()
);
alter table category enable row level security;
alter table category force row level security;
-- La taxonomia canonica (tenant_id null) la ve todo el mundo; la propia, solo su dueno.
create policy category_isolation on category
    using (tenant_id is null or tenant_id = app.current_tenant_id())
    with check (tenant_id = app.current_tenant_id());

create table if not exists color (
    id           uuid primary key default uuid_generate_v4(),
    tenant_id    uuid references tenant(id) on delete cascade,
    name         text not null,
    hex          char(7) not null check (hex ~ '^#[0-9A-Fa-f]{6}$'),
    canonical_id uuid references color(id),
    created_at   timestamptz not null default now()
);
alter table color enable row level security;
alter table color force row level security;
create policy color_isolation on color
    using (tenant_id is null or tenant_id = app.current_tenant_id())
    with check (tenant_id = app.current_tenant_id());

-- ---------------------------------------------------------------- producto
create table if not exists product (
    id             uuid primary key default uuid_generate_v4(),
    tenant_id      uuid        not null references tenant(id) on delete cascade,
    brand_id       uuid        not null references brand(id) on delete cascade,
    category_id    uuid        not null references category(id),
    reference_code text        not null,
    name           text        not null,
    description    text,
    gender         text        check (gender in ('MEN','WOMEN','UNISEX','KIDS')),
    material       text,
    season         text,
    attributes     jsonb       not null default '{}',
    status         text        not null default 'DRAFT'
        check (status in ('DRAFT','PUBLISHED','UNPUBLISHED','ARCHIVED')),
    published_at   timestamptz,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (tenant_id, brand_id, reference_code)
);
select app.apply_tenant_rls('product');

create index if not exists product_catalog_idx
    on product (tenant_id, category_id, status)
    where status = 'PUBLISHED';

-- ---------------------------------------------------------------- variante
create table if not exists product_variant (
    id            uuid primary key default uuid_generate_v4(),
    tenant_id     uuid        not null references tenant(id) on delete cascade,
    product_id    uuid        not null references product(id) on delete cascade,
    sku           text        not null,
    color_id      uuid        references color(id),
    size_label    text        not null,
    size_system   char(2),
    barcode       text,
    price         numeric(14,2) not null check (price >= 0),
    compare_price numeric(14,2),
    currency      char(3)     not null,
    is_active     boolean     not null default true,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (tenant_id, sku)
);
select app.apply_tenant_rls('product_variant');

-- ---------------------------------------------------------------- stock por tienda
--
-- RN-003: stock y precio se revalidan antes de reservar o pagar. Que `available` sea una
-- columna generada evita que alguien calcule on_hand - reserved a mano y se equivoque.
create table if not exists stock (
    tenant_id  uuid        not null references tenant(id) on delete cascade,
    variant_id uuid        not null references product_variant(id) on delete cascade,
    store_id   uuid        not null references store(id) on delete cascade,
    on_hand    int         not null default 0 check (on_hand >= 0),
    reserved   int         not null default 0 check (reserved >= 0),
    available  int         generated always as (on_hand - reserved) stored,
    source     text        not null default 'MANUAL'
        check (source in ('MANUAL','IMPORT','ERP','POS','RECONCILIATION')),
    updated_at timestamptz not null default now(),
    primary key (variant_id, store_id)
);
select app.apply_tenant_rls('stock');

create index if not exists stock_available_idx
    on stock (tenant_id, store_id)
    where available > 0;
