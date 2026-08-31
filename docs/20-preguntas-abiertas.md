# 20 · Preguntas abiertas y entrevista

Ofreciste hacer una entrevista. En lugar de bloquear el trabajo, dejé toda la documentación completa
bajo supuestos explícitos y reuní aquí lo que necesito confirmar. Está ordenado por **cuánto cambia
el trabajo si la respuesta es distinta**.

---

## Bloque A · Bloqueantes (afectan al Sprint 0–1)

### A1 · Capacidad del equipo — `D-12`, riesgo `R-15`

> **¿Cuántas personas van a trabajar en esto y con qué dedicación?**

Es la pregunta más importante del proyecto. Cambia el calendario por un factor de tres:

| Escenario | Alcance completo | Fase 1 recortada |
|---|---|---|
| 1 persona | ~3 años | ~7 meses |
| 2 personas | ~19 meses | ~4 meses |
| 4 personas | ~11 meses | ~2,5 meses |

**Supuesto que usé:** una persona con alcance recortado en Fase 1. Todo el doc 17 está calculado así.

### ~~A2 · Arquitectura~~ — **RESUELTA el 31-ago-2026**

El sponsor decide **microservicios**, con la razón de poder integrar aplicaciones externas en el
futuro. `ADR-0001` (monolito modular) queda **rechazada** y reemplazada por
[ADR-0012](adr/ADR-0012-microservicios-acotados.md).

Precisión técnica que quedó registrada: la capacidad de integrar sistemas externos viene del contrato
OpenAPI, los webhooks y los adaptadores — no del estilo arquitectónico interno. Aun así, la decisión
se ejecuta, acotada a **5 servicios como máximo** y con aparición diferida, que es lo que la hace
viable con una sola persona.

**Costo asumido:** +20-30% de esfuerzo (~2 meses de calendario) y Fase 1 deja de ser estrictamente de
inversión cero (USD 0-25/mes). Riesgo `R-29`.

### ~~A3 · Perfil corporal~~ — **RESUELTA el 31-ago-2026**

Tu aclaración de alcance la cerró: la web no es probador de consumidor y el espejo es anónimo, así
que "solo local" ya no rompe nada. Queda:

- Perfil corporal **cifrado en el dispositivo**, obligatorio (`EN-0207`, Sprint 2).
- Sincronización E2E **opcional y apagada por defecto**, solo para cambiar de teléfono (`US-0208`,
  baja de `Must` a `Should`).

Ver [ADR-0003 revisado](adr/ADR-0003-almacenamiento-perfil-corporal.md). **Ahorro: ~13 SP** y menos
riesgo regulatorio.

### A4 · Nombre — [ADR-0010](adr/ADR-0010-nombre-y-marca.md)

> **¿SmartModa, SMART IA u otro? ¿Se hizo la búsqueda marcaria?**

Bloquea el `applicationId`, que es **irreversible** una vez publicada la app en Play Store.

**Supuesto que usé:** `com.synaptia.smartmoda` solo para compilaciones internas, sin publicar nada.

### A5 · Menores de edad — `D-08`

> **¿Se atiende a menores de 18 años, o se excluyen del producto?**

El backlog dice excluirlos de los flujos corporales hasta tener análisis jurídico. Si el negocio los
necesita (ropa infantil es una categoría grande), hay que resolver consentimiento parental antes de
diseñar el registro.

**Supuesto que usé:** excluidos de flujos corporales (RN-022).

---

## Bloque B · Producto y mercado

### B1 · Cliente inicial
> ¿Hay ya un comercio dispuesto a ser el piloto? ¿Cuántos SKU tiene? ¿Cuenta con tablas de tallas y
> fotos utilizables?

Cambia todo: sin tablas de tallas, el motor del `EN-0501` es teoría. Sin fotos limpias, el probador se
ve mal y el usuario culpa al producto (`R-21`).

### B2 · Modelo de negocio
> ¿El consumidor es gratuito siempre? ¿El SaaS se cobra por SKU, por sucursal, por pruebas o por
> usuarios?

Afecta al modelo de cuotas (`US-1603`) y a qué se mide desde el primer día.

### B3 · Categorías del MVP
> ¿Todas las prendas, o empezamos por las que mejor funcionan en 2D?

Camisas, camisetas, chaquetas y vestidos se comportan mucho mejor en overlay 2D que pantalones o
prendas ajustadas. Empezar por las viables mejora la primera impresión.

**Supuesto que usé:** todas las categorías en el modelo de datos, pero el activo 2D se valida por
categoría antes de habilitarla.

### B4 · Geografía
> ¿Colombia primero, o LATAM y España desde el inicio?

El documento de color menciona "Colombia con salida a LATAM y España". Afecta a sistemas de tallas
(CO/EU/US), moneda, idioma y marco legal (Ley 1581 vs. RGPD, que son distintos y el segundo es más
estricto).

**Supuesto que usé:** Colombia primero, arquitectura preparada para más.

### B5 · Marketplace o herramienta
> ¿SmartModa es un marketplace donde el usuario compra, o una herramienta que cada tienda integra en su
> propio canal?

El documento de color asume marketplace multimarca. El backlog tiene ambos: carrito propio (`US-0905`)
y handoff al retailer (`US-0904`). Son modelos de negocio distintos y afectan a la marca, al pago y a
la relación con el comercio.

**Supuesto que usé:** ambos, con el handoff primero (más barato y menos comprometido).

---

## Bloque C · Técnicas

### C1 · iOS
> ¿Cuándo? ¿O basta con la web para usuarios de iPhone?

**Supuesto que usé:** sin iOS nativo y **sin web de consumidor**, así que hoy iOS queda sin ninguna vía de acceso. Reintroducir un probador web reducido cuesta ~8-13 SP reutilizando la pila del espejo (riesgo R-28, [ADR-0005](adr/ADR-0005-canales-y-plataformas.md)).

### C2 · Activos 3D
> ¿Hay presupuesto para modelado 3D de prendas? ¿O el comercio los provee?

Es el mayor costo no técnico del proyecto y determina la viabilidad de la Fase 2 (`R-01`). El spike
`SP-0606` debe responderlo con un número.

### C3 · ERP de los comercios
> ¿Qué sistemas usan los comercios objetivo? ¿Tienen API?

Determina si `EN-0406` son tres adaptadores o quince.

**Supuesto que usé:** importación por archivo primero (`US-0412`), API después.

### ~~C4 · Espejo inteligente~~ — **RESPONDIDAS el 31-ago-2026**

| Sub-pregunta | Respuesta | Dónde quedó |
|---|---|---|
| C4.1 Atribución | **Escaneo y manual, ambas** | `US-1409` CA-4 a CA-8 |
| C4.2 Hardware | Sin definir → recomendación dada | [13 § 7](13-stack-tecnologico.md) |
| C4.3 Umbral | "Lo más preciso posible" → convertido en umbrales por confianza | `US-1406` CA-4 y CA-5, `SP-1405` CA-3 |
| C4.4 Ubicación | **Cerca del vestier** | `EN-1410` CA-1 y CA-8, `EN-1411` CA-7 y CA-8 |
| C4.5 Comercio piloto | **Abierta** | — |

**Queda una tarea previa a cualquier compra:** medir el espacio real junto al vestier (distancia,
ancho, altura, encuadre). Es lo que más veces bloquea este tipo de instalación.

<details><summary>Enunciado original de las preguntas</summary>


Tras la aclaración del 31-ago-2026, el espejo pasa de 29 SP a **97 SP** y se convierte en una línea de
producto con gate propio ([ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md)). Abre cuatro
preguntas que no tenía:

**C4.1 · Atribución de la venta.**
¿El comercio puede modificar su punto de venta para escanear un código emitido por el espejo?

- **Sí** ⇒ atribución **exacta** de qué se probó y qué se compró. Es el dato fuerte.
- **No** ⇒ correlación agregada por SKU, talla y ventana temporal. Es útil para tendencias, pero el
  tablero debe etiquetarla como *estimada* (riesgo `R-26`).

Es la pregunta que más afecta al valor comercial del espejo.

**C4.2 · Hardware.**
¿Qué pantallas y qué equipos? El pipeline de visión en tiempo real necesita GPU; un panel comercial de
gama baja no lo sostiene. Sin esta respuesta no puedo fijar la matriz de hardware soportado ni el
objetivo de FPS.

**C4.3 · Umbral de precisión.**
¿Qué error de medida hace que una recomendación de talla siga siendo útil para el negocio? Es el
criterio del gate `SP-1405`, y debe fijarlo producto, no ingeniería. Si el error real lo supera, el
espejo sigue siendo un buen probador visual pero **no recomienda talla** (RN-001).

**C4.4 · Ubicación física.**
¿El espejo va en el vestier (privado) o en la sala de ventas (público)? Cambia por completo el
análisis de consentimiento, la aceptación social y el diseño de la señalización (`EN-1410`, riesgo
`R-24`).

**C4.5 · Comercio piloto.** ¿Hay alguno interesado? Si aparece, conviene adelantar `SP-1405` a Fase 2:
es barato saber pronto si el espejo puede recomendar talla, antes de comprometerlo comercialmente.

</details>

### C5 · Privacidad diferencial
> Si el volumen de datos crece, ¿se invierte en privacidad diferencial para los datos exportables?

El umbral de 30 usuarios reduce el riesgo de reidentificación pero no lo elimina (`R-20`, doc 14 § 6).

---

## Bloque D · Diseño

### D1 · Tipografía
> El documento de color no la define. Propuse serif editorial para titulares + sans neutra para
> interfaz (doc 16 § 9). ¿Hay una preferencia de marca?

### D2 · Modo oscuro
> ¿Desde el MVP o después?

El sistema de color ya lo define completo, así que el costo marginal es bajo si se hace desde el
principio y alto si se retrofitea.

**Supuesto que usé:** desde el MVP.

### D3 · Alcance de la personalización por tenant
> ¿Hasta dónde puede cambiar una tienda el aspecto?

Propuse: acentos, tipografía, radios, densidad, logo y movimiento; **no** semánticos, ni navegación,
ni carrito, ni pago (RN-027). Si un cliente grande exige más, es una conversación comercial con
consecuencias técnicas.

---

## Estado

Todas las preguntas de los bloques A, B, C y D fueron respondidas por el sponsor el **31-ago-2026**.

| Pregunta | Respuesta | Dónde quedó |
|---|---|---|
| A1 Capacidad | **1 persona** | [17 § 1](17-plan-de-trabajo.md) — Fase 1 recortada, ~8–9 meses |
| A2 Arquitectura | **Microservicios** | [ADR-0012](adr/ADR-0012-microservicios-acotados.md) — acotados a 5, aparición diferida |
| A3 Perfil corporal | **Solo local, cifrado** | [ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md) |
| A4 Nombre | **SmartModa** (de trabajo) | [ADR-0010](adr/ADR-0010-nombre-y-marca.md) — falta búsqueda marcaria |
| A5 Menores | **Sí, todas las edades** | [ADR-0013](adr/ADR-0013-menores-y-ropa-intima.md) — categoría sí, cámara no |
| B1 Comercio piloto | **Aún no hay** | Riesgo `R-32` |
| B2 Monetización | **Publicidad + licencia por negocio/sucursal** | [ADR-0014](adr/ADR-0014-monetizacion-publicidad.md) |
| B3 Categorías MVP | **Lo que funcione bien en 2D** | `SP-0619` mide qué categorías |
| B4 Geografía | **Colombia primero** | Ley 1581 |
| B5 Modelo | **Marketplace**, carrito también en el espejo | `US-1412` |
| C1 iOS | **Diferido** hasta ser sostenible | [ADR-0005](adr/ADR-0005-canales-y-plataformas.md), `R-28` |
| C2 Activos 3D | **Evaluar gratis / pagar / construir** | `SP-0619` |
| C3 ERP | Sin definir, **arquitectura preparada** | `integration-service` |
| C4 Espejo | Atribución doble, hardware sugerido, umbrales, junto al vestier | [ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md) |
| D1 Tipografía | **Confirmada** la propuesta | [16 § 9](16-design-system.md) |
| D2 Modo oscuro | Desde el MVP | [16 § 6](16-design-system.md) |
| D3 Personalización | **Total**, con activar/desactivar funciones | `US-1703`, `US-1705` |

## Lo único que sigue pendiente

| # | Pendiente | Bloquea | Dueño |
|---|---|---|---|
| 1 | **Concepto jurídico** sobre imagen de menores en establecimiento comercial | Despliegue del espejo (Fase 3) | Legal |
| 2 | **Búsqueda marcaria** de `SmartModa` en la SIC | **Publicar** la app, no desarrollarla | Fundadores |
| 3 | **Conseguir un comercio piloto** | Validación de talla, activos y espejo | Sponsor |
| 4 | **Medir el espacio físico** junto al vestier | Compra de hardware del espejo | Sponsor |
| 5 | **Umbral de detección de indicios de menor** en el espejo | `SP-1405` | PO |
| 6 | **Modelo de tallas de sujetador** (banda + copa) | Categoría íntima | Catálogo |

Ninguno bloquea el **Sprint 0**. Se puede empezar.

## Siguiente paso

El análisis funcional y la arquitectura están cerrados. Lo que sigue es **ejecutar el Sprint 0**:
contrato de producto, arquitectura de `core-api` con `tenant_id`, repositorio, CI y mapa de datos
([17 § 3](17-plan-de-trabajo.md)).

Los seis pendientes de arriba se resuelven en paralelo, sin detener el desarrollo.
