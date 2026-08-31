# ADR-0009 · Gateway de IA proveedor-agnóstico con validación determinista

- **Estado:** Aceptada (formaliza `D-09` del `Backlog_Ejecutable`)
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + IA

## Contexto

El producto usa IA en tres lugares con naturalezas muy distintas:

| Uso | Tipo | Dónde corre | Costo |
|---|---|---|---|
| Detección de pose | Visión, modelo fijo | **On-device** | Cero |
| Recomendación de talla | Determinista, sin modelo (ADR-0007) | On-device y servidor | Cero |
| Stylist y ranking de outfits | Generativo + recuperación | Servidor, proveedor externo | **Por invocación** |

El tercero es el único que implica un proveedor externo, y es donde se concentran los riesgos: `R-09`
(costos crecientes), `R-10` (recomienda inventario inexistente o viola restricciones) y `R-13`
(sesgo).

Los proveedores de IA cambian precios, modelos y capacidades cada pocos meses. Acoplar el dominio a
uno concreto significa que cada cambio del proveedor es un cambio en el producto.

## Decisión

**Un gateway propio con contrato estable, y validación determinista después de toda salida del
modelo.**

```
Caso de uso del dominio
   │  habla el contrato NUESTRO, no el del proveedor
   ▼
AiGateway (puerto)
   ├─► AdapterProveedorA
   ├─► AdapterProveedorB
   └─► AdapterLocal (pruebas, sin costo)
        │
        ▼
   Respuesta cruda del modelo
        │
        ▼
   VALIDACIÓN DETERMINISTA  ◄── inventario, restricciones del usuario,
        │                        tallas disponibles, seguridad de contenido
        ▼
   Resultado al usuario, con explicación y etiqueta de patrocinio si aplica
```

### El principio que gobierna todo

**El modelo propone; las reglas disponen.**

Un modelo generativo puede inventar un SKU que no existe, recomendar algo agotado o sugerir algo que
el usuario excluyó explícitamente. Por eso:

1. Los **candidatos se recuperan primero** del inventario real (búsqueda vectorial + filtros duros).
   El modelo combina lo que existe; no imagina el catálogo.
2. La **salida se valida después** contra stock vigente, restricciones del usuario y disponibilidad de
   talla (RN-011, RN-012, RN-034).
3. Lo que no pasa la validación **se descarta o se sustituye**, nunca se muestra con una nota al pie.

El inventario es la fuente de verdad. El modelo nunca lo es.

### Control de costo

| Control | Implementación |
|---|---|
| Cuota por tenant | Consultas/mes según plan; aviso al 80%, política del plan al 100% (`EN-1606`) |
| Caché | Respuestas semánticamente equivalentes se reutilizan dentro de una ventana |
| Modelo por caso | Un modelo pequeño para extraer intención, uno mayor solo para la combinación creativa |
| Presupuesto por consulta | Límite duro de tokens; superarlo devuelve resultado parcial, no un cobro sorpresa |
| Medición | Costo por consulta, por tenant y por funcionalidad, visible en el tablero (`US-1706`) |

### Evaluación como gate de promoción (`EN-0708`, `AUT-12`)

Ningún cambio de modelo, prompt o parámetro llega a producción sin pasar la suite:

| Dimensión | Umbral | Si falla |
|---|---|---|
| Relevancia | ≥ 70% aprobado por el panel | Bloquea |
| Restricciones del usuario | **100%** | Bloquea (gate duro) |
| Stock vigente | **100%** | Bloquea (gate duro) |
| Seguridad de contenido | 0 salidas inapropiadas en el conjunto adverso | Bloquea (gate duro) |
| Latencia p95 | ≤ 4 s (parcial en streaming ≤ 1,5 s) | Bloquea |
| Costo por consulta | Dentro del presupuesto del plan | Alerta |
| Sesgo por segmento | Sin degradación relativa sobre el umbral | Bloquea (`R-13`) |

El conjunto de evaluación **crece con cada defecto reportado en producción**. Así los mismos errores
no vuelven.

### Trazabilidad

Toda salida guarda versión de modelo, versión de prompt, versión de reglas y marca de tiempo
(RN-015). Sin esto, un comportamiento raro reportado por un usuario es imposible de reproducir.

## Consecuencias

**Positivas**
- Cambiar de proveedor es escribir un adaptador, no reescribir el producto.
- Se pueden comparar proveedores objetivamente con la misma suite de evaluación.
- Riesgo `R-10` cerrado por construcción: la validación determinista es obligatoria.
- Costo controlado y atribuible por tenant, que es lo que hace viable el margen del SaaS.
- Pruebas sin costo: el adaptador local devuelve respuestas fijas.
- El MVP no depende de ningún proveedor de IA (inversión cero).

**Negativas**
- El contrato propio es el mínimo común denominador: capacidades muy específicas de un proveedor no se
  aprovechan sin extender el contrato.
- Una capa más de indirección que mantener.
- La suite de evaluación es trabajo real y continuo (`EN-0708`, 8 SP más mantenimiento).
- La validación determinista puede descartar buenas sugerencias por un dato de stock desactualizado.
  Se mitiga con frescura de inventario, no relajando la validación.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Integrar directamente con un proveedor | Acopla el dominio a un tercero que cambia cada trimestre; hace imposible comparar alternativas |
| Modelo propio autoalojado | Costo de GPU incompatible con la inversión cero, y no hay datos para entrenar algo competitivo |
| Sin validación posterior (confiar en el modelo) | Garantiza que tarde o temprano se recomiende un producto inexistente o algo que el usuario excluyó. `R-10` se materializa |
| IA en el camino crítico del MVP | Rompe la inversión cero y añade una dependencia externa a la promesa central del producto |
