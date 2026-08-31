# 07 · Backlog consolidado y análisis de brechas

## 1. Qué encontré en tus archivos

### `HISTORIAS_Y_HABILITADORES_MVP_IA_MODA_COMPLETO (1).xlsx`
- 35 historias (`H-001`…`H-035`) + 6 habilitadores (`HB-001`…`HB-006`).
- 8 sprints, 8 fases. Enfoque exclusivo en la **app de consumidor**.
- **Todas tienen criterios de aceptación** en Gherkin. Están bien escritos, pero son cortos:
  la mayoría tiene 2 escenarios y ninguno cubre caminos de error ni casos límite.

### `Backlog_Ejecutable.xlsx`
- 111 ítems (`US-`/`EN-`/`SP-`), 17 épicas, 24 sprints, 745 puntos, 335 criterios de aceptación.
- Riesgos, decisiones, reglas de negocio, RNF, mapa de automatización y trazabilidad de fuentes.
- **Es un backlog notablemente maduro.** Es la fuente de verdad.

### Relación entre ambos
El primero es un **subconjunto conceptual** del segundo con numeración distinta. No son dos backlogs:
son dos versiones del mismo. Mantener los dos garantiza divergencia.

> **Decisión:** se archiva `HISTORIAS_Y_HABILITADORES...xlsx` como insumo histórico.
> La numeración vigente es la de `Backlog_Ejecutable`.

### `Sistema de color - Marketplace de moda.doc`
Sistema de color v1 en OKLCH, WCAG AA verificado, con modo oscuro y reglas de uso. Es un documento
sólido y bien razonado (la elección de neutro cálido en hue 70° para no enfriar la piel en las fotos
editoriales es exactamente el tipo de decisión que un marketplace de moda necesita). Se absorbe
íntegro en [16-design-system.md](16-design-system.md) y se le añade lo que le falta: **tipografía,
escala de espaciado, tokens ejecutables y estrategia de theming por tenant**.

---

## 2. Mapeo de IDs

| HU original | Equivalente vigente | Nota |
|---|---|---|
| H-001 | US-0101 | Onboarding orientado a valor |
| H-002 | US-0102 | Registro con correo |
| H-003 | US-0104 | Inicio, cierre y recuperación |
| H-004 | US-0601 | Explicación + captura guiada |
| H-005, H-006 | US-0601 | Frontal y lateral son escenarios de la misma historia |
| H-007 | US-0202 | Captura manual de medidas |
| H-008 | US-0105 + **US-0307 (nueva)** | El control de visualización del rostro merece historia propia |
| H-009 | US-0301 | Avatar paramétrico |
| H-010 | US-0201, US-0202 | |
| H-011 | US-0205 | |
| H-012 | US-0204 | Cuestionario de estilo |
| H-013, H-030, H-031 | US-0401 | Navegación es parte de la historia de catálogo |
| H-014 | EN-0707 | Ranking híbrido |
| H-015, H-021, H-022 | US-0703 | Outfits con stock real |
| H-016 | US-0401, US-0402 | |
| H-017 | US-0502 | |
| H-018 | US-0603 | Overlay 2D |
| H-019 | US-0503 | Confianza y explicación |
| H-020 | US-0701 | Stylist conversacional |
| H-023 | US-0801 | |
| H-024, H-025 | US-0802 | |
| H-026 | US-1002 | |
| H-027, H-029 | US-0105 | Consentimiento versionado |
| H-028 | US-0106 | |
| H-032 | US-0605 | Compartir prueba |
| H-033 | US-0201 | |
| H-034 | **US-1006 (nueva)** | Cupones no existía en el backlog vigente |
| H-035 | **US-1007 (nueva)** | Suscripción del consumidor |
| HB-001 | EN-0002, EN-0206 | |
| HB-002 | SP-0606, EN-0705 | |
| HB-003 | EN-0501, US-1102 | |
| HB-004 | EN-0705 | |
| HB-005 | EN-1501, EN-1505 | |
| HB-006 | EN-1302, EN-1601 | |

---

## 3. Análisis de brechas: lo que pediste y no estaba

Revisé tu enunciado contra los 111 ítems. Estos son los vacíos reales:

| # | Brecha | Gravedad | Resuelto por |
|---|---|---|---|
| 1 | **Super administrador de plataforma** no existe como rol ni como conjunto de historias. `US-1204` crea tenants pero no dice quién ni con qué controles. | **Crítica** | EP-17, US-1701 a US-1708 |
| 2 | **Auditoría de acciones por rol** aparece en RNF-04 como una frase, sin historia, sin modelo, sin consulta ni retención. Pediste auditorías explícitamente. | **Crítica** | EP-18, EN-1801 a EN-1804 |
| 3 | **Defensa contra fuerza bruta** solo estaba implícita. No hay historia, ni umbrales, ni prueba. | **Crítica** | EN-1510, EN-1511, EN-1512 |
| 4 | **Aplicación web** está descartada por `D-04` ("web de consumidor fuera del MVP") pero tú la pides para administración *y* como probador. El portal admin (`US-1101`+) existe pero nunca se declara su stack ni su responsividad. | **Alta** | EP-19, EN-1901 a US-1905 |
| 5 | **Jerarquía empresa → marca → tienda → sección → piso** no existe. `US-0404` da stock por tienda, nada más. Pediste sucursales y secciones según el tamaño del negocio. | **Alta** | US-0410, US-0411 |
| 6 | **Analítica predictiva prueba vs. compra** — el corazón de tu diferenciador — solo aparece como `US-1303` (embudo genérico). No hay modelo predictivo, ni ranking de más probadas, ni alerta de anomalía. | **Alta** | US-1306 a US-1310, EN-1308 |
| 7 | **Personalización visual por tenant** (`US-1207`) es una línea. Pediste editor de colores, tamaños, tipografía, estilos y **animaciones**, con configuración de funciones por tienda. | **Alta** | US-1703, US-1704, US-1705 |
| 8 | **Importación masiva de catálogo**. Un almacén con 5.000 SKU no los carga uno a uno. Solo existía el CRUD unitario. | **Alta** | US-0412 |
| 9 | **Taxonomía configurable** (categorías, colores, materiales propios del comercio) no está modelada. | **Media** | US-0413 |
| 10 | **Guardado local cifrado del perfil**. `EN-0206` dice "persistencia local" sin decir cifrada, y no resuelve el dispositivo nuevo. | **Alta** | EN-0207, US-0208, ADR-0003 |
| 11 | **Splash animado**. Estaba en `H-001` del primer Excel, se perdió en el backlog vigente. | **Baja** | EN-1608 |
| 12 | **Personalización del rostro del avatar**. Estaba en `H-008`, se perdió. Es además el punto biométrico más delicado del producto. | **Media** | US-0307 |
| 13 | **Espejo inteligente con medición automática anónima**. El backlog tenía el kiosco (`EP-14`) pero no la medición automática, ni la selección táctil, ni la agregación anónima, ni el contraste probado-vs-comprado — que es su propósito declarado. | **Alta** | SP-1405, US-1406, US-1407, EN-1408, US-1409, EN-1410, EN-1411 |
| 14 | **Cupones y suscripción del consumidor**. Estaban en `H-034`/`H-035`, se perdieron. | **Baja** | US-1006, US-1007 |
| 15 | **Portabilidad de datos del usuario** (derecho de exportación). El backlog cubre borrado, no exportación. | **Media** | US-1514 |
| 16 | **Cifrado en reposo y bóveda de claves** aparece en RNF-04 sin historia que lo implemente. | **Alta** | EN-1513 |

Además, dos observaciones de fondo que no son brechas de alcance sino de **secuencia**:

- **`tenant_id` llega en el Sprint 17.** Cualquier tabla creada entre el Sprint 0 y el 16 sin columna
  de tenant obliga a una migración de datos y una reescritura de repositorios.
  Ver [ADR-0002](adr/ADR-0002-estrategia-multitenant.md).
- **La web administrativa llega en el Sprint 7 pero el catálogo del consumidor en el Sprint 3.**
  Durante 4 sprints alguien tiene que cargar el catálogo a mano. Propongo adelantar un
  **cargador de catálogo mínimo** al Sprint 3.

---

## 4. Épicas: 17 existentes + 3 nuevas

| ID | Épica | Origen |
|---|---|---|
| EP-00 | Fundación de producto y automatización | Existente |
| EP-01 | Identidad, onboarding y consentimiento | Existente |
| EP-02 | Perfil corporal y preferencias | Existente |
| EP-03 | Avatar y gemelo digital | Existente |
| EP-04 | Catálogo, prendas e inventario | Existente (ampliada) |
| EP-05 | Recomendación de talla | Existente |
| EP-06 | Probador virtual 2D y AR | Existente (ampliada) |
| EP-07 | IA Stylist y recomendación de outfits | Existente |
| EP-08 | Favoritos, closet e historial | Existente |
| EP-09 | Compra omnicanal | Existente |
| EP-10 | Engagement y fidelización | Existente (ampliada) |
| EP-11 | Portal administrativo de tienda | Existente |
| EP-12 | SaaS multi-tenant | Existente |
| EP-13 | Analítica B2B y experimentación | Existente (ampliada) |
| EP-14 | Espejo inteligente y modo kiosco | Existente |
| EP-15 | Seguridad, privacidad, calidad y accesibilidad | Existente (ampliada) |
| EP-16 | Operación, soporte y monetización | Existente |
| **EP-17** | **Super administración de plataforma** | **Nueva** — objetivo: operar N empresas sin tocar código ni base de datos. KPI: aprovisionar un tenant nuevo en < 15 min sin intervención de ingeniería |
| **EP-18** | **Auditoría y trazabilidad de acciones** | **Nueva** — objetivo: poder responder "quién hizo qué, cuándo y desde dónde" para cualquier acción administrativa. KPI: 100% de acciones administrativas con registro íntegro |
| **EP-19** | **Plataforma web y paridad de canales** | **Nueva** — objetivo: que web y móvil sean el mismo producto con el mismo sistema de diseño. KPI: 0 divergencias de token entre plataformas |

---

## 5. Backlog vigente (111 ítems)

Se conserva íntegro. Resumen por sprint y épica:

| Sprint | Fase | Objetivo | Ítems | SP |
|---|---|---|---|---|
| 0 | 1 - MVP | Fundación y decisiones irreversibles | 6 | 24 |
| 1 | 1 - MVP | Onboarding seguro | 6 | 26 |
| 2 | 1 - MVP | Perfil corporal útil | 6 | 27 |
| 3 | 1 - MVP | Catálogo que vende | 6 | 28 |
| 4 | 1 - MVP | Talla explicable | 5 | 31 |
| 5 | 1 - MVP | MVP beta completo | 7 | 41 |
| 6 | 2 - AR+backend | Backend escalable | 4 | 23 |
| 7 | 2 | Administración autónoma | 4 | 29 |
| 8 | 2 | Base 3D validada | 4 | 31 |
| 9 | 2 | AR sobre el cuerpo | 4 | 34 |
| 10 | 2 | AR estable y con fallback | 4 | 26 |
| 11 | 2 | Medición asistida experimental | 4 | 34 |
| 12 | 2 | Conversión omnicanal | 4 | 26 |
| 13 | 2 | Piloto de tienda | 5 | 28 |
| 14 | 3 - SaaS+IA | IA Stylist contextual | 5 | 31 |
| 15 | 3 | Ranking inteligente | 5 | 37 |
| 16 | 3 | Compra dentro del flujo | 4 | 34 |
| 17 | 3 | Núcleo multi-tenant | 4 | 37 |
| 18 | 3 | Producto SaaS vendible | 4 | 29 |
| 19 | 3 | Analítica para decisiones | 4 | 32 |
| 20 | 3 | Integración ERP robusta | 4 | 29 |
| 21 | 3 | Espejo inteligente | 4 | 29 |
| 22 | 3 | I+D avanzada con evidencia | 4 | 42 |
| 23 | 3 | Escala y lanzamiento | 4 | 37 |
| | | **Total** | **111** | **745** |

El detalle ítem a ítem, con dependencias y criterios, permanece en `Backlog_Ejecutable.xlsx`
(hojas `Historias`, `Criterios`, `Dependencias`).

---

## 6. Nuevo total

| | Ítems | Puntos |
|---|---|---|
| Backlog vigente | 111 | 745 |
| Historias nuevas (doc 08) | 44 | 343 |
| **Total** | **155** | **1088** |

El impacto sobre el calendario y las opciones para absorberlo están en
[17-plan-de-trabajo.md](17-plan-de-trabajo.md).
