# SmartModa — Probador virtual y marketplace de moda con IA

> **Estado:** documentación viva · **Baseline:** 31-ago-2026
> **Empresa (provisional):** Synaptia Technologies S.A.S. · **Producto (provisional):** SmartModa
> **Decisión de marca pendiente:** ver [ADR-0010](docs/adr/ADR-0010-nombre-y-marca.md)

Aplicación **móvil + web** que permite a una persona conocer **cómo le queda una prenda antes de comprarla**, mediante:

1. **Medidas corporales** capturadas manual o automáticamente (foto / cámara).
2. **Avatar paramétrico** que se asemeja a su cuerpo (y opcionalmente a su rostro).
3. **Probador virtual** progresivo: overlay 2D → foto de cuerpo completo → AR en tiempo real por cámara.
4. **Recomendación de talla explicable** contra tablas versionadas por marca.
5. **Stylist IA** que sugiere looks con stock real y sugiere tiendas donde encontrarlos.

Para el negocio es un **marketplace multi-tenant** donde empresas, almacenes y marcas gestionan
inventario, sucursales, activos, precios y analítica predictiva de **prueba → compra**.

Ingresos por **publicidad contextual** y **licencia por negocio o sucursal**
([ADR-0014](docs/adr/ADR-0014-monetizacion-publicidad.md)).

---

## Índice de documentación

| # | Documento | Qué responde |
|---|-----------|--------------|
| 01 | [Contexto y visión](docs/01-contexto-y-vision.md) | Problema, propuesta de valor, alcance por fases, modelo de negocio |
| 02 | [Glosario](docs/02-glosario.md) | Lenguaje ubicuo del dominio |
| 03 | [Actores, roles y permisos](docs/03-actores-roles-permisos.md) | Matriz RBAC completa, super admin, admin de tienda |
| 04 | [Requisitos funcionales](docs/04-requisitos-funcionales.md) | RF-xxx trazables a épicas e historias |
| 05 | [Requisitos no funcionales](docs/05-requisitos-no-funcionales.md) | RNF-xx verificables con umbral y método de prueba |
| 06 | [Reglas de negocio](docs/06-reglas-de-negocio.md) | RN-xxx invariantes del dominio |
| 07 | [Backlog consolidado y análisis de brechas](docs/07-backlog-consolidado.md) | Las 111 historias existentes + qué faltaba |
| 08 | [Historias de usuario nuevas](docs/08-historias-nuevas.md) | 52 HU nuevas con criterios Gherkin |
| 09 | [Arquitectura de solución](docs/09-arquitectura.md) | Vistas C4, 5 microservicios con aparición diferida |
| 10 | [Multi-tenancy](docs/10-multitenancy.md) | Aislamiento, personalización, cuotas, onboarding |
| 11 | [Modelo de datos](docs/11-modelo-de-datos.md) | Entidades, DDL de referencia, RLS, particionado |
| 12 | [Seguridad y privacidad](docs/12-seguridad-y-privacidad.md) | Fuerza bruta, biometría, auditoría, cumplimiento Ley 1581 |
| 13 | [Stack tecnológico](docs/13-stack-tecnologico.md) | Elecciones por capa, costo de Fase 1, hardware del espejo, plan de salida |
| 14 | [Analítica y modelos de IA](docs/14-analitica-y-ml.md) | Eventos, warehouse, predicción prueba→compra |
| 15 | [Estrategia de pruebas](docs/15-estrategia-de-pruebas.md) | Unitarias, interfaz, flujo, contrato, carga, IA |
| 16 | [Sistema de diseño](docs/16-design-system.md) | Paleta OKLCH, tipografía, tokens, splash, theming por tenant |
| 17 | [Plan de trabajo](docs/17-plan-de-trabajo.md) | Sprints, hitos, gates y calendario real con 1 persona |
| 18 | [ADR — decisiones de arquitectura](docs/adr/) | Decisiones irreversibles, con contexto y consecuencias |
| 19 | [Riesgos](docs/19-riesgos.md) | Registro con nivel, mitigación y dueño |
| 20 | [Preguntas abiertas / entrevista](docs/20-preguntas-abiertas.md) | Lo que necesito confirmar contigo |
| 21 | [Mapa de datos](docs/21-mapa-de-datos.md) | Inventario verificable: clasificación, almacén, retención y base legal |

### Paquete de importación

| Archivo | Qué es |
|---|---|
| [azure-devops/README.md](azure-devops/README.md) | Cómo cargar el backlog en Azure Boards y cómo subir el código a Azure Repos + GitHub |
| [azure-devops/SmartModa_AzureDevOps.xlsx](azure-devops/SmartModa_AzureDevOps.xlsx) | 11 hojas: épicas, historias, criterios, RF, RNF, sprints, backlog, reglas, riesgos, ADR |
| [azure-devops/SmartModa_ExcelTeam.xlsx](azure-devops/SmartModa_ExcelTeam.xlsx) | Formato de árbol para publicar desde Excel con la pestaña Team |
| `azure-devops/csv/` | 3 CSV listos para Boards > Import Work Items |
| [azure-devops/import_ado.py](azure-devops/import_ado.py) | Importador por API con jerarquía Epic → Feature → User Story |

---

## Insumos de origen

| Archivo | Contenido | Estado |
|---|---|---|
| `HISTORIAS_Y_HABILITADORES_MVP_IA_MODA_COMPLETO (1).xlsx` | 35 HU + 6 habilitadores, 8 sprints, enfoque MVP consumidor | Absorbido |
| `Backlog_Ejecutable.xlsx` | 111 ítems, 17 épicas, 24 sprints, 335 criterios, riesgos, RNF | **Fuente de verdad del backlog** |
| `Sistema de color - Marketplace de moda.doc` | Paleta OKLCH v1, WCAG AA, modo oscuro, reglas de uso | Absorbido en doc 16 |

> Los dos Excel se solapan: el primero es un subconjunto del segundo con distinta numeración.
> **Regla de trazabilidad:** se usa la numeración `US-/EN-/SP-xxxx` del `Backlog_Ejecutable`.
> El mapeo `H-00x → US-xxxx` está en [07-backlog-consolidado](docs/07-backlog-consolidado.md#mapeo-de-ids).

---

## Alcance de los canales (confirmado 31-ago-2026)

| Canal | Para quién | Qué hace | Fase |
|---|---|---|---|
| **App Android** | Persona consumidora | Medidas, avatar, probador, talla, Stylist. **El perfil corporal vive aquí y solo aquí** | 1 |
| **Web** | Administración de tienda y de plataforma | Catálogo, inventario, roles, analítica, super administración. **No es probador de consumidor** | 2 |
| **App de administración** | Admin de tienda en movimiento | El mismo APK, módulo por rol | 2 |
| **Espejo inteligente** | Cliente **anónimo** en tienda física | Mide automáticamente por cámara, muestra la prenda en tiempo real, selección táctil. **No guarda perfil**: lo que recoge alimenta de forma anónima la predicción y el contraste probado-vs-comprado | 3 |

## Decisiones tomadas (31-ago-2026)

| Decisión | Elección | ADR |
|---|---|---|
| Nombre de trabajo | **SmartModa** (marca sin validar aún) | [0010](docs/adr/ADR-0010-nombre-y-marca.md) |
| Equipo | **1 persona** ⇒ Fase 1 recortada, ~8–9 meses | [17](docs/17-plan-de-trabajo.md) |
| Arquitectura | **Microservicios acotados: máx. 5, aparición diferida** | [0012](docs/adr/ADR-0012-microservicios-acotados.md) |
| Perfil corporal | **Solo en el móvil, cifrado.** Sincronización E2E opcional | [0003](docs/adr/ADR-0003-almacenamiento-perfil-corporal.md) |
| Multi-tenancy | `tenant_id` desde el Sprint 0 | [0002](docs/adr/ADR-0002-estrategia-multitenant.md) |
| Probador | 2D primero, AR con gate de evidencia | [0006](docs/adr/ADR-0006-probador-progresivo.md) |
| Talla | Determinista y explicable, no ML | [0007](docs/adr/ADR-0007-recomendacion-de-talla-determinista.md) |
| Espejo | Anónimo, mide sin perfil, alimenta la predicción | [0011](docs/adr/ADR-0011-espejo-inteligente-anonimo.md) |
| Menores y ropa íntima | Categorías sí; cámara sobre menores no; íntima solo en avatar | [0013](docs/adr/ADR-0013-menores-y-ropa-intima.md) |
| Monetización | Publicidad **contextual** + licencia por negocio/sucursal | [0014](docs/adr/ADR-0014-monetizacion-publicidad.md) |
| Modelo | **Marketplace**, con carrito también en el espejo | [0014](docs/adr/ADR-0014-monetizacion-publicidad.md) |
| Geografía | Colombia primero | — |
| iOS | Diferido hasta que sea sostenible | [0005](docs/adr/ADR-0005-canales-y-plataformas.md) |

## Los 3 riesgos que vigilaría cada semana

1. **Microservicios con una sola persona** (`R-29`). Es la decisión que más calendario cuesta: ~+2
   meses sobre el plan. Señal de revisión: si al cerrar el Sprint 3 más del 25% del esfuerzo se fue en
   infraestructura, hay que replantearlo.
2. **Imágenes de menores en el espejo** (`R-31`). Una tienda familiar con un espejo que mide cuerpos
   es el primer escenario que revisa una autoridad de protección de datos. Requiere concepto jurídico
   escrito antes de desplegar.
3. **Sin comercio piloto** (`R-32`). Sin él no hay dónde validar talla, activos ni espejo. Conseguirlo
   es tarea de Fase 1, no de Fase 2.
