# 17 · Plan de trabajo

## 0. Punto de partida

El `Backlog_Ejecutable` fija una línea base de **24 sprints de dos semanas, del 3-ago-2026 al
2-jul-2027**, con 745 puntos. Hoy es **31-ago-2026**, que en esa línea base es el inicio del Sprint 2.

Si el trabajo no ha comenzado, lo primero es **rebaselinar las fechas**, no arrastrar un retraso
heredado de un calendario que nunca arrancó. Las fechas de abajo son relativas al sprint, no
absolutas, precisamente por eso.

Con las 52 historias nuevas el backlog pasa a **163 ítems / 1.143 puntos**, un **+53%**.
Eso no significa +53% de calendario: parte se absorbe, parte se recorta y parte se pospone.
Este documento dice exactamente cuál es cuál.

> **Decisiones del sponsor del 31-ago-2026 que afectan a este plan:**
> **una sola persona** (escenario A confirmado) y **microservicios**
> ([ADR-0012](adr/ADR-0012-microservicios-acotados.md), +20–30% de esfuerzo).

---

## 1. El problema real: capacidad

El riesgo `R-08` lo identifica con precisión: *"Una sola persona intenta ejecutar los 24 sprints"*.
Hagamos la aritmética sin adornos.

| Escenario | Velocidad realista | 1.143 SP | Calendario |
|---|---|---|---|
| **A · 1 persona** (móvil + backend + web + QA) | 10–14 SP/sprint | ~85 sprints | **~3,2 años** |
| **B · 2 personas** (1 móvil, 1 backend/web) | 22–28 SP/sprint | ~44 sprints | **~20 meses** |
| **C · 4 personas** (2 móvil, 1 backend, 1 web/QA) | 40–50 SP/sprint | ~24 sprints | **~11 meses** |
| **D · Plan del backlog** | ~31 SP/sprint promedio | 24 sprints (745 SP) | 11 meses |

El plan original supone tácitamente el escenario C. Con una persona, el calendario de 24 sprints es
inalcanzable por un factor de tres. Esto no es una crítica al plan: es la información que hay que
tener antes de comprometer una fecha con alguien.

### El escenario real: A, confirmado

El sponsor confirma **una sola persona**. Con el alcance completo, el calendario es de años. La única
salida es recortar la Fase 1 a lo estrictamente Must y validar la hipótesis antes de seguir.

```
Fase 1 recortada (Must only, ~156 SP originales + 29 SP nuevos críticos)
   ≈ 185 SP  ÷  12 SP/sprint  ≈  16 sprints
   × 1,25 por el sobrecosto de microservicios
   ≈  19–20 sprints  ≈  8–9 meses con una persona
```

**Ocho a nueve meses hasta un MVP beta real**, con seguridad y privacidad correctas. Con una segunda
persona a partir del Sprint 4, baja a **~5 meses**.

El sobrecosto de microservicios (`R-29`) es la diferencia entre 7 y 9 meses. Es el precio de la
decisión, y está tomado con la información sobre la mesa.

> **Nota del 31-ago-2026.** La aclaración de alcance del avatar **no cambia la Fase 1**: el avatar y
> las medidas siguen siendo del canal móvil, que era el plan. Lo que cambia está en Fase 3, donde el
> espejo pasa de una línea de 29 SP a una de **97 SP** con su propio gate.

Lo que **no** se recorta bajo ninguna circunstancia: seguridad, aislamiento multi-tenant, auditoría y
consentimiento. Son los que no se pueden reparar retroactivamente sin reescribir.

### Señal de revisión del Sprint 3

Si al cerrar el Sprint 3 más del **25% del esfuerzo** se ha ido en infraestructura y despliegue en
lugar de en producto, `ADR-0012` se revisa. No por principio, sino porque con una persona ese
porcentaje es la diferencia entre entregar el MVP y no entregarlo.

---

## 2. Reubicación de las historias nuevas

| Ítem | Sprint | Razón del adelanto |
|---|---|---|
| **Modelo con `tenant_id`** (parte de `EN-0002`) | **0** | Añadirlo después es migrar todos los datos y reescribir los repositorios ([ADR-0002](adr/ADR-0002-estrategia-multitenant.md)) |
| `EN-1510` Fuerza bruta | **1** | Un endpoint de autenticación sin límite de tasa es explotable el día que la beta se hace pública |
| `EN-0207` Cifrado local del perfil | **2** | Cifrar después de persistir en claro obliga a migrar el dato más sensible del producto |
| `US-0410` Jerarquía organizacional | **3** | El modelo de tienda/sección debe existir antes de que el catálogo lo asuma |
| `EN-1608` Splash | **5** | Cierre visual del MVP beta |
| `EN-1901` Web administrativa | **6** | Ya estaba implícita en el backlog; ahora explícita y estimada |
| `EN-1903` Tokens compartidos | **6** | Antes de escribir la primera pantalla web, no después |
| `EN-1801` Auditoría | **7** | Debe existir **antes** del portal administrativo, no como parche posterior |
| `US-0412` Importación masiva | **7** | Sin ella el portal no es usable por un almacén real |
| `US-0413` Taxonomía configurable | **7** | Junto con el CRUD, no después |
| `EN-1511` MFA administrativo | **7** | El portal administrativo no debe nacer sin MFA |
| `EN-1513` Cifrado en reposo | **8** | Con el backend propio ya en pie |
| `US-1904` App móvil de administración | **9** | Después de que la web valide los flujos |
| `US-0411` Localizador en tienda | **12** | Con la conversión omnicanal |
| `US-1514` Portabilidad, `EN-1512` Sesiones | **13** | Con el piloto de tienda |
| `US-0307` Rostro del avatar | **11** | Con la calibración del avatar |
| `US-0208` Recuperación de perfil | **11** | Con el avatar ya persistente |
| `US-1306`–`US-1310` Analítica predictiva | **19** | Necesitan histórico real; antes del Sprint 19 no hay datos que modelar |
| `EN-1308` Modelo predictivo | **19–20** | Idem |
| `US-1701`–`US-1708` Super administración | **17** | Junto al núcleo multi-tenant, con el modelo ya listo desde el Sprint 0 |
| `US-1802`, `US-1803`, `EN-1804` Auditoría avanzada | **17–18** | Sobre el servicio base del Sprint 7 |
| `SP-1405` a `EN-1411` Espejo anónimo | **21 (sprint doble)** | Sustituyen a `US-0618`, retirado. Adelantar `SP-1405` a Fase 2 si aparece un comercio interesado: es el gate que decide si el espejo puede recomendar talla |
| `US-1006`, `US-1007` Cupones y suscripción | **18** | Con la monetización |
| `US-0107` Selector de contexto | **7** | Con el acceso administrativo |
| `US-1905` Paridad de sesión | **18** | Cuando existan ambos canales maduros |

---

## 3. Fase 1 revisada — MVP (Sprints 0–5)

Objetivo: **una persona puede medirse, ver la talla explicada y verse la prenda encima. Y el sistema
es seguro y privado desde el día uno.**

| Sprint | Objetivo | Ítems | SP |
|---|---|---|---|
| **0** | Fundación · decisiones irreversibles | `US-0001`, `EN-0002` (**+ tenant_id + costuras de módulo**), `EN-0003`, `EN-0004`, `EN-0005`, `EN-1501` | 26 |
| **1** | Onboarding seguro | `US-0101`, `US-0102`, `US-0104`, `US-0105`, `EN-1502`, **`EN-1510`** | 32 |
| **2** | Perfil corporal útil y cifrado | `US-0201`, `US-0202`, `US-0203`, `US-0204`, `EN-0206`, **`EN-0207`** | 32 |
| **3** | Catálogo que vende | `US-0401`, `US-0402`, `US-0403`, `US-0404`, `US-0801`, `EN-0405`, **`US-0410`** | 36 |
| **4** | Talla explicable | `EN-0501`, `US-0502`, `US-0503`, `US-0601`, `EN-0602` | 31 |
| **5** | MVP beta completo | `US-0603`, `US-0604`, `US-0605`, `US-0802`, `EN-1503`, `EN-1601`, `US-0106`, **`EN-1608`** | 44 |
| | **Total Fase 1** | **38 ítems** | **201 SP** |

### Si hay que recortar (una sola persona)

Diferibles sin comprometer la hipótesis: `US-0103` (Google), `US-0205` (preferencias de color),
`US-0604` (ajuste manual del overlay), `US-0605` (compartir), `US-0802` (closet).
**≈ 23 SP menos ⇒ 178 SP.**

Innegociables aunque duela: `US-0105` consentimiento, `EN-1501` mapa de datos, `EN-1502` reglas de
seguridad, `EN-1510` fuerza bruta, `EN-0207` cifrado local, `EN-1503` accesibilidad, `US-0106`
eliminación de cuenta, `tenant_id` en el modelo.

### Criterio de salida de la Fase 1 (gate)

- [ ] Un usuario real completa: registro → consentimiento → medidas → talla → prueba 2D, sin ayuda
- [ ] Crash-free ≥ 99,5% sobre ≥ 50 usuarios de beta
- [ ] Arranque en frío p95 ≤ 3 s en el dispositivo de gama media de referencia
- [ ] 0 hallazgos críticos o altos de seguridad
- [ ] 0 fotos corporales enviadas al servidor, verificado con capturador de tráfico
- [ ] Aceptación de la talla recomendada ≥ 60% en la beta (el objetivo de 75% es de piloto)
- [ ] Suite de fuerza bruta en verde
- [ ] Perfil corporal cifrado, verificado por prueba negativa
- [ ] Costo de infraestructura = USD 0

**Decisión de fase:** si la aceptación de talla está por debajo del 50%, el problema es el motor de
tallas o las tablas, no el probador. Arreglarlo antes de invertir en AR.

---

## 4. Fase 2 — AR y backend propio (Sprints 6–13)

| Sprint | Objetivo | Añadidos |
|---|---|---|
| 6 | Backend escalable | `EN-1901` web administrativa, `EN-1903` tokens |
| 7 | Administración autónoma | **`EN-1801` auditoría**, `US-0412` importación, `US-0413` taxonomía, `EN-1511` MFA, `US-0107` contexto |
| 8 | Base 3D validada | `EN-1513` cifrado en reposo |
| 9 | AR sobre el cuerpo | `US-1904` app de administración |
| 10 | AR estable con fallback | — |
| 11 | Medición asistida experimental | `US-0307` rostro, `US-0208` recuperación de perfil |
| 12 | Conversión omnicanal | `US-0411` localizador en tienda |
| 13 | Piloto de tienda | `US-1514` portabilidad, `EN-1512` sesiones |

**Sprint 7 es el más cargado del proyecto** (~29 SP originales + ~40 SP nuevos). Recomendación:
partirlo en dos sprints. El portal administrativo con auditoría, MFA, importación y taxonomía es un
producto completo, no un incremento.

### Gates de la Fase 2

| Gate | Ubicación | Criterio |
|---|---|---|
| **Go/no-go de activos 3D** | Fin del Sprint 8 | `SP-0606` debe responder: horas y costo por prenda. Si preparar una prenda para AR cuesta más de lo que la empresa puede sostener, **AR se cancela** y la Fase 2 se reorienta a mejorar el 2D. Riesgo `R-01` |
| **Compatibilidad AR** | Sprint 8 | Matriz publicada; ninguna prenda entra sin perfil de activo validado (RN-018) |
| **Precisión de estimación** | Sprint 11 | Si la estimación automática es peor que la manual, se mantiene *beta* y no se promueve |
| **Piloto** | Sprint 13 | Métrica de negocio: delta de devolución con prueba previa vs. sin ella |

### Criterio de salida de la Fase 2

- [ ] Piloto en una tienda real durante ≥ 4 semanas
- [ ] Delta de devolución medido (sea cual sea el resultado — **el dato es el entregable**)
- [ ] AR funcionando en la matriz soportada, con fallback 2D verificado en el resto
- [ ] Portal administrativo usable por el comercio **sin intervención del equipo**
- [ ] Auditoría operativa y verificable
- [ ] Backend propio con SLO definidos y monitoreados

---

## 5. Fase 3 — SaaS e IA (Sprints 14–23+)

| Sprint | Objetivo | Añadidos |
|---|---|---|
| 14 | IA Stylist contextual | — |
| 15 | Ranking inteligente | — |
| 16 | Compra dentro del flujo | — |
| **17** | **Núcleo multi-tenant + super administración** | `US-1701`–`US-1706`, `US-1802` |
| 18 | Producto SaaS vendible | `US-1707`, `US-1708`, `US-1803`, `EN-1804`, `US-1006`, `US-1007`, `US-1905` |
| **19** | **Analítica para decisiones** | `US-1306`, `US-1307`, `US-1309` |
| 20 | Integración ERP + predicción | `EN-1308`, `US-1310` |
| 21 | **Espejo inteligente** — sprint doble | `SP-1405`, `US-1406`, `US-1407`, `EN-1408`, `US-1409`, `EN-1410`, `EN-1411` (+68 SP). `US-0618` retirado |
| 22 | I+D con evidencia | — |
| 23 | Escala y lanzamiento | — |

El Sprint 17 se vuelve el más pesado de la Fase 3 (~37 + ~50 SP). Con capacidad de 4 personas es un
sprint de dos; con menos, tres.

### Gates de la Fase 3

| Gate | Criterio |
|---|---|
| **Aislamiento multi-tenant** | Suite negativa completa en verde + pentest sin hallazgos de aislamiento. **Ningún cliente entra en producción antes de esto** |
| **Go/no-go de body scan** (`SP-0306`) | Benchmark contra medición manual. Si no supera de forma medible, se cancela |
| **Go/no-go de medición en espejo** (`SP-1405`) | Error contra cinta en instalación calibrada. Si supera el umbral, el espejo **no recomienda talla** y queda como probador visual |
| **Revisión jurídica del espejo** (`EN-1410`) | Sin aprobación documentada, el despliegue en tienda está **bloqueado** |
| **Go/no-go de física de tela** (`SP-0615`) | Idem |
| **Calidad de IA** | Suite de evaluación en verde antes de cada promoción de modelo |
| **Lanzamiento** | Pentest remediado, simulacro de restauración exitoso, auditoría de privacidad y accesibilidad aprobada |

---

## 6. Hitos

| # | Hito | Sprint | Qué se puede hacer después |
|---|---|---|---|
| M1 | Fundación lista | 0 | Desarrollar con CI, contratos y decisiones tomadas |
| M2 | Acceso seguro | 1 | Registrar usuarios reales sin riesgo |
| M3 | Perfil corporal cifrado | 2 | Tratar datos sensibles legalmente |
| M4 | Catálogo navegable | 3 | Mostrar producto real |
| M5 | **Talla explicable** | 4 | **Validar la hipótesis central del producto** |
| M6 | **MVP beta distribuible** | 5 | Poner el producto en manos de usuarios reales |
| M7 | Backend propio + web administrativa | 7 | Que un comercio se administre solo |
| M8 | AR validada o cancelada con evidencia | 8–10 | Decidir la inversión en 3D con un número, no con una intuición |
| M9 | **Piloto en tienda con dato de devolución** | 13 | **Vender el SaaS con evidencia** |
| M10 | Multi-tenant verificado + super administración | 17 | Operar varios clientes |
| M11 | Analítica predictiva | 19–20 | Cobrar por el diferenciador |
| M12 | Lanzamiento | 23 | Escalar comercialmente |

**M5, M6 y M9 son los que importan.** M5 dice si el producto resuelve el problema. M6 lo pone frente a
usuarios. M9 produce el número que convierte el producto en un negocio. Todo lo demás sirve a estos.

---

## 7. Gestión

### Ceremonias mínimas (equipo pequeño)

| Ceremonia | Frecuencia | Duración |
|---|---|---|
| Planificación de sprint | Inicio de sprint | 1–2 h |
| Revisión + retrospectiva | Fin de sprint | 1 h |
| Refinamiento | Mitad de sprint | 1 h |
| Revisión de riesgos y decisiones | Fin de fase | 2 h |

Con una sola persona, la retrospectiva sigue teniendo sentido: es donde se registra la velocidad real,
que es lo que permite recalibrar en lugar de acumular retraso silencioso.

### Recalibración

**Después de 3 sprints, la velocidad medida sustituye a la estimada.** Está en las instrucciones del
propio `Backlog_Ejecutable` y es la práctica correcta. La estimación inicial siempre es optimista; lo
que importa es que el plan se corrija pronto.

### Límite de trabajo en curso

Con una persona: **máximo 2 ítems en curso**. Con dos: 3. Terminar es más valioso que empezar; un
sprint con seis cosas al 80% no entrega nada.

### Qué se hace si un sprint no cierra

1. No se arrastra el ítem incompleto: se re-estima con lo aprendido.
2. Si un ítem se arrastra dos sprints, se **parte**, siempre.
3. La velocidad no se "recupera" trabajando más horas; se corrige el plan.

---

## 8. Camino crítico

```
EN-0002 (arquitectura + tenant_id)
   └─► EN-0206 ─► EN-0207 (perfil cifrado)
        └─► US-0202 (medidas)
             └─► EN-0501 ─► US-0502 ─► US-0503   ★ hipótesis central
                  └─► EN-0602 ─► US-0603          ★ probador 2D
                       └─► SP-0606 (activos 3D)   ★ gate de la Fase 2
                            └─► US-0609 (AR)
EN-0006 (OpenAPI)
   └─► EN-1201 ─► EN-1202 ─► EN-1203
        └─► US-1101 ─► EN-1801 (auditoría) ─► US-1102..1104
             └─► US-1204 ─► EN-1206 (aislamiento) ★ gate de la Fase 3
                  └─► US-1701 (super administración)
EN-1302 (eventos)
   └─► US-1303 ─► US-1306 ─► EN-1308 (predicción)
```

Los tres cuellos de botella reales:

1. **`SP-0606` (pipeline 3D)** — determina si la Fase 2 es viable. Adelantar el spike lo máximo
   posible; no requiere que el resto esté terminado.
2. **`EN-1206` (aislamiento)** — bloquea todo cliente real en producción. Por eso el modelo debe
   estar preparado desde el Sprint 0.
3. **`EN-1801` (auditoría)** — bloquea el portal administrativo si se hace bien, o genera deuda
   irreparable si se pospone.

---

## 9. Decisiones pendientes que bloquean el plan

Del `Backlog_Ejecutable`, siguen abiertas y afectan al calendario:

| ID | Decisión | Cuándo se necesita | Bloquea |
|---|---|---|---|
| `D-01` | Nombre del producto | Antes del Sprint 1 | Paquete de la app, dominio, marca, activos |
| `D-02` | Nombre de la empresa | Antes de constituir | Contratos, facturación |
| `D-08` | Menores de 18 años | Antes del Sprint 1 | Flujo de registro (RN-022) |
| `D-11` | Línea base temporal | Sprint 0 | Todo el calendario |
| `D-12` | Capacidad del equipo | **Ya** | Determina si el plan es de 11 meses o de 3 años |

`D-12` es la más urgente de todas y es una pregunta para ti, no una decisión técnica. Está en
[20-preguntas-abiertas.md](20-preguntas-abiertas.md).
