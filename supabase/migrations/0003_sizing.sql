-- 0003 · Tablas de talla versionadas
--
-- RN-001: una recomendacion se calcula siempre contra una tabla versionada por marca y
-- categoria. Si no existe, se informa sin inventar.
--
-- RN-015: una tabla NO se edita. Se publica una version nueva y la anterior se conserva,
-- para que una recomendacion emitida hace seis meses se pueda reproducir exactamente.
-- Por eso los rangos cuelgan de size_chart_version y no de size_chart.

create table if not exists size_chart (
    id          uuid primary key default uuid_generate_v4(),
    tenant_id   uuid        not null references tenant(id) on delete cascade,
    brand_id    uuid        not null references brand(id) on delete cascade,
    category_id uuid        not null references category(id),
    gender      text        not null check (gender in ('MEN','WOMEN','UNISEX','KIDS')),
    size_system char(2)     not null,
    name        text        not null,
    created_at  timestamptz not null default now()
);
select app.apply_tenant_rls('size_chart');

create table if not exists size_chart_version (
    id             uuid primary key default uuid_generate_v4(),
    tenant_id      uuid        not null references tenant(id) on delete cascade,
    size_chart_id  uuid        not null references size_chart(id) on delete cascade,
    version        int         not null check (version > 0),
    -- RN-015: de donde salio la tabla. Sin esto no se puede auditar una recomendacion
    -- erronea, que es el riesgo R-03.
    source         text        not null,
    effective_from timestamptz not null default now(),
    is_active      boolean     not null default true,
    created_at     timestamptz not null default now(),
    unique (size_chart_id, version)
);
select app.apply_tenant_rls('size_chart_version');

-- Una sola version activa por tabla: dos activas harian la recomendacion no determinista.
create unique index if not exists size_chart_one_active_idx
    on size_chart_version (size_chart_id)
    where is_active;

create table if not exists size_chart_entry (
    id              uuid primary key default uuid_generate_v4(),
    tenant_id       uuid        not null references tenant(id) on delete cascade,
    version_id      uuid        not null references size_chart_version(id) on delete cascade,
    size_label      text        not null,
    sort_order      int         not null,
    measurement_key text        not null
        check (measurement_key in ('HEIGHT','CHEST','WAIST','HIP','INSEAM',
                                   'SHOULDER','SLEEVE','NECK','THIGH','FOOT_LENGTH')),
    -- En milimetros enteros, igual que el dominio. Los decimales flotantes producen
    -- comparaciones inestables justo en el borde de un rango, que es donde se decide
    -- una talla.
    min_mm          int         not null check (min_mm >= 0),
    max_mm          int         not null,
    created_at      timestamptz not null default now(),
    check (min_mm < max_mm),
    unique (version_id, size_label, measurement_key)
);
select app.apply_tenant_rls('size_chart_entry');

create index if not exists size_chart_entry_lookup_idx
    on size_chart_entry (version_id, sort_order);

-- ---------------------------------------------------------------- lectura publica
--
-- El catalogo y sus tallas son datos publicos: un usuario sin cuenta debe poder navegar.
-- Se expone una vista de SOLO LECTURA con lo publicado, en lugar de relajar el RLS de las
-- tablas base. Relajarlo abriria tambien la escritura, y ese es el error que se paga caro.
create or replace view public_catalog
with (security_invoker = true)
as
select
    v.id            as variant_id,
    v.sku,
    v.size_label,
    v.price,
    v.currency,
    p.id            as product_id,
    p.name          as product_name,
    p.description,
    p.gender,
    p.material,
    p.category_id,
    b.name          as brand_name,
    p.tenant_id
from product_variant v
join product p on p.id = v.product_id
join brand   b on b.id = p.brand_id
where p.status = 'PUBLISHED'
  and v.is_active;

comment on view public_catalog is
  'Catalogo publicado, solo lectura. No relaja el RLS de las tablas base.';
