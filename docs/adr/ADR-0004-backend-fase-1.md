# ADR-0004 · Backend de Fase 1: Supabase detrás de una capa Repository

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura
- **Refina:** `D-06` del `Backlog_Ejecutable` ("Firebase/Room en MVP y capa Repository para migrar")

## Contexto

Requisito explícito: **Fase 1 con inversión cero**. La decisión previa `D-06` propone Firebase en el
MVP con migración a Spring Boot + PostgreSQL en Fase 2, protegida por una capa Repository.

La capa Repository es correcta y no se discute. Lo que sí conviene revisar es **cuál** BaaS, porque la
diferencia en costo de migración es grande.

| Aspecto | Firebase (Firestore) | Supabase (PostgreSQL) |
|---|---|---|
| Modelo de datos | Documentos, sin esquema | Relacional con esquema |
| Migración a Fase 2 | **Reescritura del modelo**: documentos → tablas, consultas → SQL, sin uniones ni transacciones multi-entidad | `pg_dump` / `pg_restore` |
| `tenant_id` + RLS (ADR-0002) | Reglas de seguridad propietarias, distintas a RLS | **RLS real de PostgreSQL**, la misma que en Fase 2 |
| Consultas del catálogo | Sin uniones; hay que desnormalizar y mantener consistencia a mano | SQL normal |
| Transacciones | Limitadas | Completas |
| Auth | Excelente y muy maduro | Bueno |
| Almacenamiento y funciones | Sí | Sí |
| Pruebas locales en CI | Emulator Suite, muy bueno (`AUT-04`) | Contenedor local de Postgres |
| Riesgo de proyecto pausado por inactividad | No | Sí en el nivel gratuito — hay que vigilarlo |

El punto decisivo es que **el modelo de datos del doc 11 es relacional**: tablas de tallas versionadas
con restricciones de integridad, stock por tienda con columnas generadas, auditoría particionada,
`tenant_id` con RLS. Construir eso sobre un almacén de documentos y luego migrarlo a PostgreSQL es
hacer el modelo dos veces.

## Decisión

**Supabase (PostgreSQL gestionado) en Fase 1, detrás de una capa Repository, con `tenant_id` y RLS
desde el primer día.**

```
Feature (ViewModel)
      │
      ▼
UseCase (dominio puro, Kotlin)
      │
      ▼
Repository  ← interfaz definida en el dominio
      │
   ┌──┴───────────────┐
   ▼                  ▼
Local (Room+SQLCipher)  Remote
                          │
                Fase 1 ──► SupabaseDataSource
                Fase 2 ──► VistiaApiDataSource   (cambia solo esta clase)
```

Componentes de Fase 1:

| Necesidad | Supabase | Nota |
|---|---|---|
| Base de datos | PostgreSQL gestionado | El mismo esquema del doc 11, incluidas las políticas RLS |
| Autenticación | Supabase Auth (JWT) | Migrable a Spring Security manteniendo el `sub` |
| Almacenamiento | Supabase Storage (compatible S3) | Migrable a cualquier S3 |
| Funciones | Edge Functions | Solo para lo mínimo; la lógica de negocio vive en `shared-domain` |
| Local | Room + SQLCipher | Offline-first (RNF-07) + cifrado local (EN-0207) |

**Firebase se conserva para lo que hace mejor y no ata el modelo de datos:**
Crashlytics, Performance Monitoring, App Distribution, Cloud Messaging y Test Lab. Son servicios
laterales, sustituibles sin tocar el dominio.

## Consecuencias

**Positivas**
- Fase 1 con inversión cero, igual que con Firebase.
- La migración a Fase 2 es un volcado de base de datos, no una reescritura del modelo.
- RLS y `tenant_id` se prueban desde el Sprint 1 (ADR-0002), con la misma tecnología de producción.
- El SQL que se escribe en Fase 1 sigue siendo válido en Fase 2.
- Las pruebas de integración usan PostgreSQL real desde el principio (doc 15 § 3), sin la brecha
  H2-vs-producción.

**Negativas**
- Menos maduro que Firebase en algunas áreas, sobre todo en el ecosistema de Android.
- El nivel gratuito pausa proyectos inactivos: hay que mantener actividad o aceptar el arranque en
  frío. Se mitiga con un ping programado desde CI.
- Supabase Auth es menos completo que Firebase Auth (por ejemplo en federación). Si se necesita más,
  se puede combinar: Firebase Auth para identidad + Supabase para datos, validando el JWT de Firebase
  en las políticas RLS.
- Se depende igualmente de un proveedor gratuito (riesgo `R-14`). La diferencia es que aquí la salida
  es trivial porque el estándar subyacente es PostgreSQL.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Firebase/Firestore (decisión `D-06`) | Migración = reescritura del modelo. Con un modelo tan relacional como este, es hacer el trabajo dos veces |
| Backend propio desde el Sprint 0 | Rompe el requisito de inversión cero y consume Sprints 0–2 en infraestructura en lugar de en producto |
| Solo local, sin backend en Fase 1 | Imposible: el catálogo es multi-usuario por definición |
| PostgreSQL autogestionado en un VPS gratuito | Sin copias de seguridad gestionadas, sin actualizaciones, sin SLA. El tiempo de operación no es gratis |

## Plan de salida (obligatorio, se escribe en el Sprint 0)

1. `pg_dump` del esquema y los datos.
2. `pg_restore` en el PostgreSQL gestionado de Fase 2.
3. Migrar identidades: los usuarios de Supabase Auth conservan su `sub` como `app_user.id`.
4. Cambiar la implementación de `RemoteDataSource` en el cliente. **Una clase.**
5. Mover los activos entre buckets compatibles S3.
6. Corte con doble escritura durante un release, y retorno posible durante 48 h.

Que este plan esté escrito **antes** de necesitarlo es lo que hace que la dependencia del proveedor
gratuito sea una decisión y no una trampa.
