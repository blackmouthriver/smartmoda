# ADR-0012 · Microservicios acotados, que aparecen con su funcionalidad

- **Estado:** Aceptada por el sponsor
- **Fecha:** 2026-08-31
- **Decide:** Sponsor (decisión de arquitectura tomada por el dueño del producto)
- **Reemplaza a:** [ADR-0001](ADR-0001-monolito-modular-vs-microservicios.md), que proponía monolito modular

## Contexto

`ADR-0001` recomendaba monolito modular. El sponsor decide **microservicios**, con esta razón:

> *"me inclino por microservicios, ya que es posible que se requiera en un futuro integrar
> aplicaciones"*

### Una precisión técnica sobre esa razón

La capacidad de integrar aplicaciones externas **no proviene del estilo arquitectónico interno**.
Proviene de tres cosas, y las tres existen igual en un monolito modular:

| Capacidad de integración | De dónde sale realmente |
|---|---|
| Que un ERP o POS lea y escriba nuestro catálogo | Contrato **OpenAPI** público y versionado (`EN-0006`) |
| Que reaccionemos a eventos de un sistema ajeno | **Webhooks** entrantes idempotentes (`EN-0407`, `EN-0408`) |
| Que consumamos sistemas heterogéneos sin contaminar el dominio | **Adaptadores** con anti-corruption layer (`EN-0406`) |
| Que un tercero se suscriba a lo que pasa aquí | Webhooks salientes |

Un cliente externo llama a `POST /v1/products`. Le da exactamente igual si detrás hay un proceso o
quince. Lo que microservicios sí aportan es **despliegue y escalado independientes**, que es un
beneficio real pero distinto del que se buscaba.

Dicho esto: **es una decisión del sponsor y se ejecuta.** Lo que sigue es cómo hacerla viable con una
sola persona, que es la restricción que de verdad aprieta.

## Decisión

**Arquitectura de microservicios, con dos reglas que la hacen ejecutable:**

### Regla 1 · Cinco servicios como máximo, nunca más

| Servicio | Responsabilidad | Por qué merece ser un servicio |
|---|---|---|
| **`core-api`** | Identity, Tenancy, Catalog, Inventory, Sizing, Commerce, Audit | Todo esto comparte transacciones y exige consistencia **inmediata**: stock, precio, reserva (RN-003). Separarlo genera consistencia eventual donde el negocio no la tolera |
| **`asset-service`** | Procesamiento de imágenes, pipeline de activos 2D/3D | Trabajo intensivo en CPU/GPU, dirigido por cola, ciclo de vida propio. Aquí vive la generación de activos 3D |
| **`integration-service`** | Adaptadores ERP/POS, webhooks entrantes y salientes, conciliación | **Es el servicio que materializa el objetivo declarado**: integrar aplicaciones sin tocar el núcleo |
| **`ai-service`** | Gateway de IA, Stylist, ranking, evaluación de modelos | Los modelos y prompts cambian semanalmente; el catálogo no. Ciclo de despliegue distinto |
| **`analytics-service`** | Ingesta de eventos, warehouse, modelos predictivos | Perfil de carga opuesto al OLTP: escritura masiva frente a lectura transaccional |

**`core-api` no se fragmenta más.** Partir catálogo, inventario y comercio en servicios separados
convierte "reservar una prenda" en una saga distribuida con compensación, para un beneficio que con
este volumen no existe. Es el error que hunde proyectos de microservicios con equipos pequeños.

### Regla 2 · Un servicio aparece cuando aparece su funcionalidad

No se despliegan cinco servicios el primer día para tener cinco cosas que operar sin tener cinco cosas
que hacer.

| Fase | Servicios desplegados | Por qué |
|---|---|---|
| **1 · MVP** | **Ninguno propio.** Supabase hace de backend | No hay nada que un servicio propio aporte todavía, y desplegarlo rompería la inversión cero ([ADR-0004](ADR-0004-backend-fase-1.md)) |
| **2 · AR + backend** | `core-api`, `asset-service`, `integration-service` | `core-api` sustituye a Supabase (`EN-1201`, Sprint 6). Llegan el pipeline de activos y los primeros comercios con ERP |
| **3 · SaaS + IA** | Los cinco | Llegan Stylist y analítica predictiva |

> **Corrección del 31-ago-2026.** La primera versión de esta tabla situaba `core-api` en la Fase 1,
> lo que contradecía a [ADR-0004](ADR-0004-backend-fase-1.md) (Supabase en Fase 1) y al propio
> backlog, que sitúa `EN-1201` en el Sprint 6. Se corrige aquí: **en la Fase 1 no se despliega ningún
> servicio propio.** La app móvil habla con Supabase a través de la capa Repository, y en el Sprint 6
> esa capa cambia de implementación sin tocar el dominio. Es exactamente el propósito del patrón.

Esto no es un monolito disfrazado: desde el Sprint 0 hay contratos de API, eventos asíncronos,
identidad y despliegue por servicio. Simplemente **no se crea un servicio antes de que tenga trabajo
que hacer**, del mismo modo que no se crea una tabla vacía por si acaso.

### Reglas de construcción

1. **Contrato antes que código.** `contracts/openapi.yaml` por servicio, versionado. Los clientes se
   generan; nadie escribe un DTO a mano (`AUT-05`).
2. **Sin base de datos compartida.** Cada servicio con su esquema. `core-api` no lee las tablas de
   `analytics-service` ni al revés.
3. **Comunicación asíncrona por defecto.** Síncrona solo cuando el usuario está esperando la respuesta.
4. **Idempotencia obligatoria** en todo endpoint con efecto de negocio (RN-010).
5. **`tenant_id` derivado del token en todos los servicios** (RN-009, [ADR-0002](ADR-0002-estrategia-multitenant.md)).
   Un fallo de aislamiento en cualquiera de los cinco es un fallo de la plataforma.
6. **Trazas distribuidas desde el primer servicio.** `trace_id` propagado. Sin esto, depurar dos
   servicios ya es adivinar.
7. **Un pipeline plantilla, reutilizado.** El coste de operación crece con el número de pipelines
   *distintos*, no de servicios. Una plantilla de CI/CD compartida es lo que hace esto sostenible con
   una persona.
8. **Dominio compartido en `shared-domain`** (Kotlin puro): tipos, políticas de talla, `TenantId`.
   Es una biblioteca versionada, no un servicio.

## Consecuencias

**Positivas**
- `integration-service` da un lugar limpio y aislado a lo que motivó la decisión: cada ERP nuevo es un
  adaptador dentro de un servicio que se despliega solo, sin tocar el núcleo.
- `asset-service` permite escalar el procesamiento de activos 3D sin tocar la API.
- `ai-service` aísla el componente de costo variable y de cambio semanal.
- Escalado y despliegue independientes cuando el volumen lo pida.
- Las fronteras están donde el dominio ya las tenía marcadas ([09 § 3](../09-arquitectura.md)).

**Negativas — hay que asumirlas con los ojos abiertos**

| Costo | Magnitud estimada |
|---|---|
| **Esfuerzo adicional** | +20–30% a partir de la Fase 2, que es cuando aparecen los servicios. La Fase 1 no se ve afectada, pero el plan global pasa de ~7 a **~8–9 meses** por el trabajo de plataforma que hay que preparar |
| **Costo de infraestructura** | La Fase 1 **sigue siendo de inversión cero** gracias a la corrección de arriba. El sobrecosto aparece en la Fase 2: tres servicios en lugar de uno, estimado USD 80–200/mes |
| **Operación** | Trazas distribuidas, contratos entre servicios y despliegue coordinado, con una sola persona a cargo |
| **Depuración** | Un error de "talla no recomendada" puede cruzar dos servicios |
| **Fronteras equivocadas** | Mover una frontera entre servicios es un proyecto, no un refactor. Por eso solo cinco, y en las costuras ya identificadas |

**Riesgo nuevo:** `R-29` — microservicios operados por una sola persona. Mitigación: máximo cinco
servicios, aparición diferida, plantilla única de CI/CD y `core-api` sin fragmentar.

## Lo que sigue igual

`ADR-0002` (multi-tenancy desde el Sprint 0), `ADR-0004` (Supabase en Fase 1),
`ADR-0006` (probador progresivo), `ADR-0007` (talla determinista), `ADR-0008` (auditoría),
`ADR-0009` (gateway de IA) no cambian. Todos eran independientes del estilo arquitectónico, que es
precisamente la señal de que estaban bien planteados.

`ADR-0008` gana importancia: con cinco servicios, la auditoría transversal es la única forma de
reconstruir qué pasó.

## Señal de revisión

Si al final del Sprint 3 el tiempo dedicado a infraestructura y despliegue supera el 25% del esfuerzo
total, esta decisión se revisa. No para volver atrás por principio, sino porque con una persona ese
porcentaje es la diferencia entre entregar el MVP y no entregarlo.
