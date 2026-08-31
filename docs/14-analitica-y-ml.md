# 14 · Analítica y modelos de IA

El dato que este producto genera y que hoy nadie tiene es **qué se prueba y no se compra**. Es el
activo diferenciador. Este documento define cómo se captura, se modela y se convierte en decisiones,
sin comprometer la privacidad que el doc 12 promete.

---

## 1. Taxonomía de eventos (EN-1302)

### Principios

1. **Esquema versionado.** Cada evento tiene `schema_key` y `schema_version`. Un evento que no valida
   va a **cuarentena**, no se descarta ni se ingiere corrupto.
2. **Seudónimo con sal por tenant.** `user_ref = HMAC(sal_del_tenant, user_id)`. Dos tenants no pueden
   correlacionar al mismo usuario entre sí.
3. **Sin datos corporales.** Nunca una medida individual. Sí bandas agregadas (`talla_recomendada`),
   nunca `busto = 94 cm`.
4. **El evento describe lo que pasó, no lo que la UI mostró.** `try_on_completed`, no `pantalla_5_vista`.
5. **El contrato del evento vive en `contracts/events/`** y se revisa como código.

### Eventos del núcleo

| Evento | Cuándo | Propiedades clave |
|---|---|---|
| `product_impression` | El producto aparece en una lista visible | `variant_id`, `position`, `surface`, `tenant_id` |
| `product_viewed` | Se abre el detalle | `variant_id`, `source`, `duration_ms` |
| `size_recommended` | El motor emite una talla | `variant_id`, `size`, `confidence`, `chart_version_id`, `between_sizes` |
| `size_recommendation_accepted` | El usuario confirma o selecciona esa talla | `variant_id`, `size`, `matched_recommendation` |
| `try_on_started` | Empieza una prueba | `variant_id`, `mode` (AVATAR/PHOTO_2D/AR/MIRROR) |
| **`try_on_completed`** | La prueba se renderiza y el usuario la ve ≥ 2 s | `variant_id`, `mode`, `size`, `duration_ms`, `session_id` |
| `try_on_failed` | La prueba no pudo completarse | `variant_id`, `reason`, `fallback_used` |
| `favorite_added` / `removed` | | `target_type`, `target_id` |
| `outfit_generated` | El Stylist devuelve looks | `outfit_id`, `item_count`, `model_version`, `latency_ms`, `cost_units` |
| `outfit_feedback` | El usuario califica | `outfit_id`, `verdict` |
| `cart_item_added` | | `variant_id`, `size`, `from_try_on_session_id` |
| **`commercial_action`** | Carrito, reserva, deep link o compra | `variant_id`, `action_type`, `try_on_session_id` |
| `order_placed` | Orden confirmada | `order_id`, `lines[].try_on_session_id` |
| `order_returned` | Devolución | `order_id`, `line_id`, `reason` |
| `store_location_viewed` | Consulta de ubicación en tienda | `variant_id`, `store_id` |
| `kiosk_session_started` / `ended` | | `store_id`, `duration_ms`, `paired` |
| **`mirror_session_completed`** | Termina una sesión del espejo | `store_id`, `session_hash`, bandas de talla, variantes probadas, duración, resultado — **nunca medidas ni identidad** (§ 7) |

### El enlace que lo hace todo posible

`try_on_session_id` viaja desde `try_on_completed` hasta `order_line`. Ese único campo convierte una
métrica de vanidad ("cuántas pruebas hubo") en una métrica de negocio ("cuántas pruebas se
convirtieron"). Está en el modelo de datos como columna real
([11 § 8](11-modelo-de-datos.md)), no como una unión heurística por tiempo.

---

## 2. Arquitectura analítica

```
Cliente (móvil/web/kiosco)
   │  lote de eventos, con reintento y cola offline
   ▼
Ingesta  ──► validación de esquema ──► válidos ──► warehouse (columnar)
                    │                                    │
                    └──► cuarentena + alerta             ▼
                                                  tablas agregadas
                                                         │
                                        ┌────────────────┼────────────────┐
                                        ▼                ▼                ▼
                                   Tableros B2B     Modelos ML      Exportación
                                   (US-1303..1310)  (EN-1308)       (US-1305)
```

**El OLTP no se consulta para analítica.** Un `GROUP BY` sobre millones de eventos en la misma base
que atiende el catálogo degrada la latencia que RNF-01 exige. Separación desde el primer tablero.

### Modelo del warehouse (esquema en estrella)

```
fact_try_on          fact_commercial_action      fact_order_line
  date_key             date_key                    date_key
  tenant_key           tenant_key                  tenant_key
  variant_key          variant_key                 variant_key
  store_key            store_key                   store_key
  user_ref             user_ref                    user_ref
  mode                 action_type                 try_on_session_id
  size                 try_on_session_id           quantity
  confidence           …                           unit_price
  completed                                        returned
  duration_ms

dim_variant   dim_product   dim_store   dim_tenant   dim_date   dim_size
```

Granularidad: un evento por fila. Las agregaciones se materializan por día y por tenant.

---

## 3. Métricas del embudo prueba → compra (US-1306)

```
impresión
   │ tasa de vista
   ▼
vista de detalle
   │ tasa de prueba
   ▼
prueba virtual iniciada
   │ tasa de finalización        ← calidad técnica del probador
   ▼
prueba virtual completada
   │ tasa de intención           ← favorito o carrito
   ▼
acción comercial
   │ tasa de cierre
   ▼
compra
   │ tasa de devolución          ← la métrica que valida la promesa del producto
   ▼
compra retenida
```

| Métrica | Fórmula | Qué revela |
|---|---|---|
| **Tasa de conversión de prueba** | compras / pruebas completadas | Salud comercial de la prenda |
| **Brecha prueba-compra** | tasa de la variante − mediana de su categoría | Dónde se pierde la venta |
| **Tasa de abandono de prueba** | pruebas fallidas o abandonadas / iniciadas | Salud técnica del probador |
| **Aceptación de talla** | recomendaciones aceptadas / emitidas | Confianza en el motor de tallas (KPI EP-05: ≥ 75%) |
| **Devolución por talla** | devoluciones por talla / compras | La prueba que valida la hipótesis central del producto |
| **Delta de devolución** | devolución con prueba previa vs. sin prueba previa | **El argumento de venta del SaaS** |

Esa última es la métrica que justifica el precio del producto ante un retailer. Debe estar disponible
desde el piloto de tienda (Sprint 13), no en Fase 3.

---

## 4. Modelos

### 4.1 Recomendación de talla — **determinista, no ML**

Deliberadamente sin aprendizaje automático en el MVP:

```
entrada:  medidas del usuario (mm) + preferencia de ajuste
          + size_chart_version aplicable
proceso:  para cada talla, calcular la distancia a los rangos de cada
          medida clave de la categoría (pecho/cintura/cadera/entrepierna)
          → puntuar → ordenar
salida:   talla recomendada
          + confianza (HIGH / MEDIUM / LOW / NONE)
          + explicación legible
          + alternativa si está entre dos tallas
```

Confianza:

| Nivel | Condición |
|---|---|
| `HIGH` | Todas las medidas clave dentro del rango de una sola talla |
| `MEDIUM` | Medidas clave en dos tallas adyacentes, o una medida faltante |
| `LOW` | Más de una medida faltante, o dispersión entre tallas no adyacentes |
| `NONE` | No hay tabla aplicable ⇒ **se informa, no se recomienda** (RN-001) |

Por qué determinista: es **explicable** ("tu pecho está en el rango de la M, tu cintura entre M y L"),
**reproducible** (guarda la versión de tabla, RN-015), **gratuito** y **funciona sin conexión**. Un
modelo estadístico requiere histórico de devoluciones que aún no existe. Cuando exista, se añade como
**capa de ajuste** sobre el determinista, no en su reemplazo.

### 4.2 Predicción de conversión prueba → compra (EN-1308)

| Aspecto | Definición |
|---|---|
| Objetivo | `P(compra | prueba completada)` por variante y contexto |
| Características | Categoría, precio relativo a la categoría, confianza de la talla, si estaba entre dos tallas, disponibilidad de la talla probada, calidad del activo, modo de prueba, estacionalidad, historial agregado del tenant |
| Modelo | Gradient boosting (LightGBM/XGBoost). No red neuronal: los datos son tabulares y la explicabilidad importa |
| Validación | **Partición temporal**, nunca aleatoria. El futuro no puede filtrarse al entrenamiento |
| Línea base | Tasa histórica de conversión de la categoría. Si el modelo no la supera de forma medible, **no se despliega** |
| Arranque en frío | Tenant nuevo ⇒ modelo global de categoría, predicción marcada como *baja confianza* |
| Deriva | Dos períodos consecutivos bajo el umbral ⇒ alerta + retorno automático a la línea base |
| Privacidad | Entrenamiento sobre agregados y seudónimos; **ninguna medida corporal individual** |
| Explicabilidad | SHAP o importancia de características, expuesta al usuario del tablero |

### 4.3 Predicción de curva de tallas (US-1310)

```
demanda estimada por talla =
      f(pruebas por talla, búsquedas por talla,
        distribución agregada de perfiles corporales del segmento,
        estacionalidad, histórico de ventas)

brecha = demanda estimada − stock disponible
```

El insumo más valioso es la distribución de perfiles corporales de los usuarios que interactuaron con
ese tenant, agregada con mínimo 30 usuarios por celda. Un retailer no puede obtener eso de ninguna
otra fuente: sus ventas solo le dicen qué compró quien encontró su talla, no qué buscó quien no la
encontró.

### 4.4 Detección de anomalías (US-1309)

Regla antes que modelo:

```
si  pruebas(variante, 30d) ≥ umbral_volumen
y   conversión(variante) < percentil_20(conversión de su categoría)
entonces
    generar alerta con causas candidatas ordenadas por evidencia:
      1. talla más probada sin stock            → INVENTARIO
      2. precio > percentil_75 de la categoría  → PRECIO
      3. resolución o calidad del activo baja   → CONTENIDO
      4. tabla de tallas antigua o inconsistente→ TALLAJE
      5. devoluciones altas del mismo producto  → PRODUCTO
```

Una regla explicable que el comerciante entiende y puede accionar vale más que un modelo que dice
"anomalía detectada" sin decir por qué.

### 4.5 Ranking de recomendaciones (EN-0707) — híbrido

```
puntuación = w1·ajuste_de_talla        (determinista, del motor de tallas)
           + w2·afinidad_de_estilo     (embeddings de preferencias × atributos)
           + w3·disponibilidad_stock   (filtro duro, no peso: RN-011)
           + w4·señal_de_negocio       (margen, rotación — identificado, RN-012)
           + w5·popularidad            (con corrección de sesgo de posición)
           − p1·penalización_restricciones  (filtro duro, RN-012)
```

Los pesos se ajustan por experimento (EN-1301), nunca a mano en producción. Stock y restricciones del
usuario son **filtros duros**, no pesos: ninguna ponderación comercial puede hacer que aparezca algo
agotado o algo que el usuario excluyó.

### 4.6 Stylist IA (EP-07)

```
Solicitud del usuario en lenguaje natural
   │
   ▼
Extracción de intención (ocasión, clima, presupuesto, restricciones)
   │
   ▼
Recuperación de candidatos ──► SOLO SKU con stock vigente del tenant
   │                            (búsqueda vectorial + filtros duros)
   ▼
Generación de combinaciones (modelo)
   │
   ▼
Validación determinista ──► descarta lo agotado, lo que viola restricciones,
   │                        lo que no tiene talla para este usuario (RN-034)
   ▼
Explicación + etiqueta de patrocinio si aplica (RN-012)
```

**El modelo propone; las reglas disponen.** Un modelo generativo puede inventar un SKU que no existe.
Por eso el catálogo se recupera antes y se valida después: el modelo nunca es la fuente de verdad del
inventario.

---

## 5. Evaluación continua de IA (EN-0708, AUT-12)

Ningún cambio de modelo, prompt o parámetro llega a producción sin pasar la suite:

| Dimensión | Umbral | Consecuencia si falla |
|---|---|---|
| Relevancia | ≥ 70% de outfits aprobados por el panel de evaluación | Bloquea promoción |
| Restricciones del usuario | **100%** de cumplimiento | Bloquea (gate duro) |
| Disponibilidad de stock | **100%** de SKU con stock vigente | Bloquea (gate duro) |
| Seguridad de contenido | 0 salidas inapropiadas en el conjunto adverso | Bloquea (gate duro) |
| Latencia p95 | ≤ 4 s (parcial en streaming ≤ 1,5 s) | Bloquea |
| Costo por consulta | Dentro del presupuesto del plan | Alerta + revisión |
| Sesgo | Métricas desagregadas por tipo de cuerpo, tono de piel y gama de dispositivo, sin degradación relativa por encima del umbral | Bloquea (riesgo `R-13`) |

El conjunto de evaluación se versiona y crece: cada defecto reportado en producción se convierte en un
caso de la suite. Así los mismos errores no vuelven.

---

## 6. Privacidad en analítica

| Control | Implementación |
|---|---|
| Seudonimización | `user_ref` derivado con sal por tenant, rotable |
| Umbral de agregación | Ningún segmento con menos de **30 usuarios distintos** muestra detalle (US-1306 CA-4) |
| Sin datos corporales | Bandas de talla, nunca medidas individuales |
| Consentimiento | La analítica opcional es rechazable sin degradar el producto (RN-007) |
| Retención | Eventos 24 meses; agregados indefinidos |
| Exportación | Solo datos agregados y anonimizados (US-1305); **la exportación queda auditada** |
| Aislamiento | Todo agregado filtrado por tenant; consultas cruzadas solo para la plataforma, sobre datos ya agregados |

Nota de honestidad técnica: el umbral de 30 y la seudonimización reducen el riesgo de
reidentificación, **no lo eliminan**. Un ataque de diferenciación sobre consultas repetidas con
filtros que se solapan puede aislar individuos. La mitigación adecuada si el volumen crece es
privacidad diferencial en las consultas exportables. Queda registrado como decisión pendiente en
[20-preguntas-abiertas.md](20-preguntas-abiertas.md).

---

## 7. El canal del espejo: analítica anónima en tienda física

Es la fuente de datos que ningún competidor tiene, y funciona con reglas distintas al móvil.
Decisión completa en [ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md).

### Qué lo hace distinto

| | Móvil | Espejo |
|---|---|---|
| Identidad | Usuario autenticado | **Anónima siempre** |
| Unidad de análisis | Usuario a lo largo del tiempo | **Sesión aislada** |
| Medidas | Cifradas en el dispositivo, nunca salen | **Solo en memoria durante la sesión** |
| Lo que llega al servidor | Bandas de talla calculadas en el móvil | Bandas de talla calculadas **en el espejo** |
| Retención | Perfil mientras exista la cuenta | **Cero** |
| Correlación entre sesiones | Sí, por usuario | **Imposible por diseño** |

### El evento del espejo

```json
{
  "schema_key": "mirror_session_completed",
  "schema_version": 1,
  "store_id": "…",
  "session_hash": "aleatorio por sesión, no derivado de nada estable",
  "size_bands": [{ "category": "shirts", "band": "M", "confidence": "HIGH" }],
  "variants_tried": ["sku-1", "sku-2"],
  "duration_ms": 184000,
  "outcome": "TRIED_ONLY | LOCATION_REQUESTED | PAIRED_TO_MOBILE",
  "consent_version": "…"
}
```

Lo que **no** contiene, y es lo importante: ninguna medida numérica, ningún identificador de persona,
ningún dato biométrico, nada que permita saber si dos sesiones son de la misma persona.

### La cadena que produce el contraste

```
Sesión anónima ──► "alguien de banda M probó la variante X en la tienda Y"
                              │
                     ¿se vendió esa variante allí?
                              │
              ┌───────────────┴────────────────┐
              ▼                                ▼
   Vía A · escaneo en caja            Vía B · correlación agregada
   El espejo emite un código;         El POS reporta ventas; se cruza por
   la caja lo escanea.                SKU + talla + ventana temporal.
   Atribución EXACTA.                 Atribución ESTIMADA.
```

**El tablero siempre etiqueta cuál de las dos está usando** (`US-1409` CA-4 y CA-5). Presentar una
correlación como si fuera atribución exacta lleva al comerciante a decidir compras sobre un dato más
débil de lo que cree — es el riesgo `R-26`.

### Lo que revela, y que hoy no existe en ninguna parte

| Métrica | Por qué es nueva |
|---|---|
| Bandas de talla que se prueban en tienda | Las ventas solo dicen qué compró quien **encontró** su talla |
| Bandas probadas **sin stock** en esa tienda | Venta perdida por curva de tallas, hoy completamente invisible |
| Prendas más probadas físicamente | Interés en el punto de venta, distinto del interés digital |
| Contraste probado-vs-comprado por tienda | Dónde exactamente se pierde la venta en el local |
| Diferencia de curva entre sucursales | Una tienda de centro comercial no tiene la misma distribución que una de barrio |

Estas métricas alimentan `US-1310` (curva de tallas), `US-1307` (rankings) y `US-1309` (anomalías),
que hasta ahora solo se nutrían de datos digitales.

### Los tres controles que hacen esto legítimo

1. **Agregación en el borde** (`EN-1408`): las medidas se convierten en bandas *dentro del espejo*.
   Lo que no sale no se puede filtrar.
2. **`session_hash` aleatorio**: no deriva de nada estable, así que dos visitas de la misma persona
   son dos sesiones sin relación. El sistema **no cuenta visitantes únicos** — es una renuncia
   deliberada, no una limitación técnica.
3. **Umbral de 30 sesiones por celda** antes de mostrar detalle, igual que en el resto de la analítica.

Sin estos tres, el espejo dejaría de ser un probador y sería un sistema de vigilancia en un comercio,
con un marco legal por completo distinto (`R-25`).

---

## 8. Tableros

### Para el comercio (B2B)

| Tablero | Contenido | Historia |
|---|---|---|
| Resumen | KPI del período, variación, alertas activas | US-1303 |
| Embudo | Impresión → compra, segmentable | US-1306 |
| Rankings | Más vistas, más probadas, mayor brecha | US-1307 |
| Tallas | Curva demandada vs. disponible, quiebres recurrentes | US-1310 |
| **Espejo en tienda** | Probado vs. comprado, bandas sin stock, atribución etiquetada como exacta o estimada | US-1409 |
| Tiendas | Comparativa por sucursal, stock, conversión | US-1304 |
| Devoluciones | Tasa, motivos, correlación con prueba previa | US-1305 |
| Alertas | Anomalías con causas candidatas y acción directa | US-1309 |

### Para la plataforma

| Tablero | Contenido | Historia |
|---|---|---|
| Salud de tenants | Uso, cuotas, errores, costo variable | US-1706 |
| Adopción | Usuarios activos, retención, pruebas por usuario | EN-1302 |
| Calidad de IA | Relevancia, latencia, costo, evaluaciones | EN-0708 |
| Seguridad | Accesos denegados, intentos entre tenants, impersonaciones | US-1803 |
| Calidad de datos | Eventos en cuarentena, esquemas fallidos | EN-1302 |
