# ADR-0002 · Multi-tenancy desde el Sprint 0

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura
- **Modifica:** la secuencia del `Backlog_Ejecutable`, que sitúa `EN-1206` en el Sprint 17

## Contexto

El backlog vigente coloca todo el núcleo multi-tenant en el Sprint 17: `US-1204` (crear tenant),
`US-1205` (roles), `EN-1206` (aislamiento), `US-1207` (marca por tienda).

Entre el Sprint 0 y el 16 se crean, según el propio plan: productos, variantes, activos, tablas de
tallas, stock, tiendas, favoritos, sesiones de prueba, órdenes, reservas, eventos, pagos.

Si esas tablas nacen sin `tenant_id`, en el Sprint 17 hay que:

1. Añadir la columna a ~20 tablas.
2. Rellenarla con datos que ya existen y que no siempre permiten inferir a qué tenant pertenecen.
3. Reescribir todos los repositorios y todas las consultas.
4. Añadir políticas RLS y reprobar todo.
5. Auditar cada endpoint uno a uno buscando fugas.

Eso no es un sprint: es un proyecto de meses con riesgo alto de introducir exactamente el fallo que se
quería evitar. El riesgo `R-11` está clasificado como **crítico** con razón: un fallo de aislamiento
entre empresas termina un negocio SaaS de un día para otro.

## Decisión

Se separa **multi-tenancy** en tres cosas distintas, con tres momentos distintos:

| Dimensión | Qué es | Cuándo |
|---|---|---|
| **Modelo y contexto** | `tenant_id` en toda tabla de negocio, `TenantContext` derivado del token, repositorio base que inyecta el filtro | **Sprint 0** (parte de `EN-0002`) |
| **Refuerzo en base de datos** | Row Level Security con `FORCE` en PostgreSQL | Sprint 6 (con `EN-1202`) |
| **Verificación y funcionalidad** | Suite negativa completa, gestión de tenants, roles, marca, cuotas | Sprint 17 (`EN-1206`, `US-1204`+) |

### Lo que se hace en el Sprint 0

1. Toda tabla de negocio nace con `tenant_id uuid NOT NULL`.
2. Existe `TenantContext`, derivado **siempre** del contexto autenticado, nunca del cliente (RN-009).
3. El repositorio base aplica el filtro de tenant. **Nadie escribe el `WHERE tenant_id` a mano.**
4. En Fase 1 hay un único tenant real, con su UUID fijo. La columna existe y se usa igual.
5. Un script de CI (`AUT-22`) verifica que **toda migración nueva** que cree una tabla de negocio
   incluye `tenant_id NOT NULL`. Sin ese script, el modelo se degrada solo con el tiempo.

### Estrategia de aislamiento

Base compartida, esquema compartido, `tenant_id` + RLS, con **tres capas independientes**:

```
Capa 1 · Aplicación  TenantContext + repositorio base con filtro automático
Capa 2 · Base datos  RLS con FORCE ROW LEVEL SECURITY + current_setting('app.tenant_id')
Capa 3 · Pruebas     Suite negativa generada desde OpenAPI: token de A + recurso de B ⇒ 404
```

Detalle completo en [10-multitenancy.md](../10-multitenancy.md).

### Un matiz importante

**El usuario consumidor no pertenece a un tenant.** Se registra una vez en la plataforma y puede
comprar en muchas tiendas, como en cualquier marketplace. Lo que pertenece al tenant es la
*interacción* de ese usuario con su catálogo (`user_tenant_interaction`).

Consecuencia: una empresa **nunca** ve la lista de usuarios de la plataforma, solo agregados de
quienes interactuaron con ella, con umbral mínimo de 30 para evitar reidentificación.

## Consecuencias

**Positivas**
- El Sprint 17 pasa de "migrar todo" a "activar y verificar".
- El aislamiento se puede probar desde el Sprint 1, cuando el sistema es pequeño y los errores baratos.
- Falla cerrado: sin contexto de tenant, cero filas.
- Una sola migración de esquema para todos los clientes.

**Negativas**
- Una columna que en Fase 1 siempre vale lo mismo. Costo: despreciable.
- RLS impone una pequeña sobrecarga por consulta. Aceptable, y es la red de seguridad ante un error de
  la aplicación.
- El modelo compartido significa que un fallo de aislamiento afecta a todos. Se compensa con tres
  capas y con `EN-1507` (pentest con el aislamiento en el alcance explícito).

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Base de datos por tenant | Aislamiento máximo, pero operar N bases con un equipo de 1–4 personas es inviable. Se reserva para clientes empresariales que lo exijan y lo paguen |
| Esquema por tenant | Migraciones × N esquemas; límite práctico alrededor de 500 tenants; complejidad operativa alta |
| Mantener `EN-1206` en el Sprint 17 sin preparación | Es la alternativa del backlog actual, y es la que convierte un sprint en un proyecto de meses con riesgo crítico |
