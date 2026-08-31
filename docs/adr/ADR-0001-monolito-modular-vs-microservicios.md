# ADR-0001 · Monolito modular antes que microservicios

- **Estado:** **RECHAZADA** el 31-ago-2026 — el sponsor decide microservicios.
  **Reemplazada por [ADR-0012](ADR-0012-microservicios-acotados.md).**
  Se conserva porque el análisis de costos de los microservicios sigue siendo válido y es la base de
  las restricciones que ADR-0012 impone (máximo 5 servicios, `core-api` sin fragmentar).
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + Sponsor
- **Reemplaza a:** la premisa de microservicios del enunciado original y de `PROPUESTA.pptx`

## Contexto

El enunciado propone microservicios, con la nota de que "se puede discutir finalmente la
arquitectura". El `Backlog_Ejecutable` describe en `EN-1201` un único backend Spring Boot, así que ya
hay una tensión entre la intención declarada y el plan real.

Restricciones que importan:

- Equipo entre 1 y 4 personas (riesgo `R-08`).
- Fase 1 con **inversión cero**.
- El dominio todavía no está estabilizado: las fronteras entre catálogo, inventario y tallaje se
  están descubriendo, no están dadas.
- Operaciones que requieren consistencia **inmediata**: stock, precio, reserva. RN-003 exige
  revalidar antes de reservar o pagar.

## Qué cuestan realmente los microservicios

Lo que se paga desde el primer día, tenga o no el problema que los justifica:

| Costo | Manifestación concreta aquí |
|---|---|
| Consistencia distribuida | "Reservar una prenda" toca inventario, comercio y catálogo. Con servicios separados es una saga con compensación en lugar de una transacción |
| Latencia | Cada frontera es una llamada de red. El detalle de producto necesita catálogo + inventario + tallas + activos: 4 saltos donde había 1 consulta |
| Operación | N pipelines, N despliegues, N tableros, N conjuntos de alertas — con un equipo que no tiene una persona dedicada a plataforma |
| Depuración | Un error de "talla no recomendada" se persigue por tres servicios y sus trazas |
| Costo de infraestructura | N contenedores mínimos ≠ inversión cero |
| Fronteras equivocadas | Es el costo mayor: mover una frontera dentro de un monolito es refactorizar; entre servicios es un proyecto |

El beneficio real de los microservicios —escalado y despliegue independientes por perfil de carga—
no aplica todavía: en Fase 1 no hay carga que separar, y en Fase 2 sigue sin haberla.

## Decisión

**Monolito modular con costuras explícitas, y extracción selectiva cuando un disparador medible lo
justifique.**

1. Un despliegue de backend, organizado en módulos por bounded context
   ([09 § 3](../09-arquitectura.md)).
2. Cada módulo con arquitectura hexagonal interna y un paquete `api` público.
3. **Prohibida** la dependencia entre `domain` de módulos distintos, y prohibido el acceso directo a
   las tablas de otro módulo. Verificado por **ArchUnit en CI** (`AUT-23`), no por revisión humana.
4. Comunicación asíncrona entre módulos por eventos de dominio en un bus interno, con la misma forma
   que tendría si fuera una cola.
5. Extracción de un servicio solo con disparador medible:

| Servicio candidato | Disparador |
|---|---|
| Styling / IA | El costo o la latencia de IA obliga a escalar y desplegar por separado |
| Analytics | La ingesta de eventos degrada la latencia del OLTP |
| Assets / media | El procesamiento de imagen y 3D compite por CPU con la API |

El resto —identity, tenancy, catalog, inventory, sizing, commerce, audit— se queda junto: comparten
transacciones y necesitan consistencia inmediata.

## Consecuencias

**Positivas**
- Fase 1 con inversión cero: un contenedor, o incluso solo el BaaS.
- Las fronteras se pueden corregir mientras el dominio se estabiliza.
- Transacciones reales donde el negocio las necesita (stock, reserva, pago).
- Un pipeline, un tablero, una alerta: operable por una persona.
- Depuración con una sola traza.

**Negativas**
- Todo escala junto. Aceptable hasta el volumen previsto en Fase 3.
- Sin disciplina, el monolito modular degrada a monolito enredado. **Por eso los gates de ArchUnit son
  obligatorios y bloquean el merge.** Sin ellos, esta decisión no se sostiene.
- Un despliegue afecta a todos los módulos. Se mitiga con feature flags (`EN-0007`) y despliegue
  progresivo.

**Coste de reversión:** bajo, **si y solo si** las costuras se mantienen. Extraer un módulo que ya se
comunica solo por su `api` y por eventos es mover un paquete y cambiar un adaptador. Ese es
exactamente el propósito de la disciplina estructural.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Microservicios desde el inicio | Costo operativo y de consistencia sin el problema que lo justifica; fronteras aún no descubiertas |
| Monolito sin módulos | Barato hoy, imposible de dividir mañana. No permite ninguna evolución |
| Serverless por función | Encaja con inversión cero, pero fragmenta el dominio en cientos de funciones sin modelo compartido, y el arranque en frío castiga la latencia del catálogo |

## Si prefieres microservicios de todos modos

Es tu decisión y es defendible si el objetivo es demostrar la arquitectura ante un tercero. En ese
caso mi recomendación mínima:

- Empezar con **tres** servicios, no diez: `core` (identity + tenancy + catalog + inventory + sizing +
  commerce), `styling` y `analytics`.
- Presupuestar desde el inicio: gateway, descubrimiento, trazas distribuidas, contratos entre
  servicios y una persona con tiempo dedicado a plataforma.
- Aceptar que la Fase 1 **no será de inversión cero**.
