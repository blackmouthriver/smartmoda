# ADR-0006 · Probador progresivo: 2D antes que AR, con fallback siempre

- **Estado:** Aceptada (formaliza `D-05` y `D-10` del `Backlog_Ejecutable`)
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + PO

## Contexto

El producto promete "ver cómo te queda la prenda". Hay cuatro niveles técnicos para cumplir esa
promesa, con costos y riesgos muy distintos:

| Nivel | Qué es | Costo | Riesgo |
|---|---|---|---|
| 1 · Overlay 2D | Prenda plana compuesta sobre foto, guiada por landmarks de pose | Bajo | Bajo |
| 2 · Avatar 3D paramétrico | Cuerpo generado desde las medidas, con prenda 3D encima | Medio | Medio |
| 3 · AR en tiempo real | Prenda 3D anclada al cuerpo en vídeo en vivo | Alto | Alto |
| 4 · Física de tela | Simulación de caída, peso y comportamiento del tejido | Muy alto | Muy alto |

Los riesgos `R-01` (sin activos 3D estandarizados) y `R-02` (prometer física de tela o talla exacta
antes de validar) están correctamente identificados en tu backlog. El error clásico en este tipo de
producto es empezar por el nivel 3 o 4, gastar meses, y descubrir que preparar cada prenda cuesta más
de lo que el negocio puede sostener.

## Decisión

**Progresión estricta 1 → 2 → 3 → 4, con gate de evidencia entre cada nivel, y fallback garantizado
hacia abajo en todo momento.**

### Fase 1 · Overlay 2D

- Pose on-device con ML Kit (33 landmarks), tras una captura guiada.
- Composición 2D de la prenda sobre la foto, con ajuste manual disponible.
- **Etiqueta de confianza y aviso de no-garantía visibles siempre** (RN-004, RN-016).
- Sin activos 3D: la prenda es una imagen plana con puntos de anclaje. **Sin costo de modelado.**

### Fase 2 · Avatar 3D y AR — con gate

```
SP-0606 · Spike de pipeline 3D  (Sprint 8)
   Debe responder con un NÚMERO:
     ¿cuántas horas y cuántos dólares cuesta preparar una prenda para AR?
   │
   ├─► viable       ─► AR continúa (Sprints 9–10)
   └─► no viable    ─► AR SE CANCELA
                       La Fase 2 se reorienta a mejorar el 2D:
                       más categorías, mejor ajuste, mejor calidad de activo
```

Ese gate es la parte más importante de esta decisión. `R-01` puede hundir la Fase 2, y el momento de
descubrirlo es el Sprint 8, no el Sprint 12.

Reglas durante la Fase 2:
- **Ninguna prenda entra a AR sin perfil de activo validado** (RN-018, `D-10`).
- Matriz de compatibilidad ARCore publicada; los dispositivos fuera de ella caen a 2D **sin error
  visible** (RN-019).
- Presupuesto de rendimiento: ≥ 24 FPS. Por debajo, se degrada la calidad antes de congelar o
  calentar el dispositivo (RNF-02).

### Fase 3 · I+D, solo con evidencia

`SP-0306` (body scan) y `SP-0615` (física de tela) son **investigación con gate go/no-go**, no
funcionalidades comprometidas. `EN-0616` define el benchmark de precisión que decide. Si no superan la
línea base, se cancelan y se documenta por qué.

### Fallback como invariante

```
AR         ─► si no hay ARCore, Depth o rendimiento  ─► Avatar 3D
Avatar 3D  ─► si no hay activo 3D válido             ─► Overlay 2D
Overlay 2D ─► si la pose no se detecta               ─► Ajuste manual
Manual     ─► si el usuario no quiere cámara          ─► Foto cargada
Foto       ─► si no quiere foto                       ─► Avatar sin foto
```

Cada nivel tiene una prueba de degradación forzada (doc 15). Un fallback que nunca se ejerce no existe.

## Consecuencias

**Positivas**
- El MVP entrega valor real sin depender de activos 3D, que es el mayor riesgo del proyecto.
- La hipótesis central (talla explicable + verse la prenda) se valida en el Sprint 5, no en el 12.
- El gate del Sprint 8 convierte `R-01` en una decisión informada por un número.
- Nadie queda excluido por su dispositivo: siempre hay un nivel que funciona.
- Las promesas al usuario coinciden con lo que el sistema puede sostener (`R-02`).

**Negativas**
- El overlay 2D es visiblemente menos impresionante que el AR. En una demostración comercial se nota.
  Se compensa con la explicación de talla, que es donde está el valor real.
- Mantener cuatro niveles y sus transiciones es más código que mantener uno.
- La progresión estricta significa que el AR no llega antes del Sprint 9.

## Cómo se comunica al usuario

Esto es tan importante como la implementación técnica (`R-02`):

| Nivel | Etiqueta mostrada |
|---|---|
| Overlay 2D | *"Vista aproximada · no representa la caída real de la tela"* |
| Avatar 3D | *"Basado en tus medidas · aproximación"* |
| AR | *"Vista en tiempo real · aproximación"* |
| Talla `HIGH` | *"Tus medidas coinciden con la talla M de esta marca"* |
| Talla `MEDIUM` | *"Estás entre M y L · según tu preferencia de ajuste, te sugerimos M"* |
| Talla `LOW` | *"Con las medidas que tenemos, la sugerencia es poco precisa"* |
| Talla `NONE` | *"Esta marca no tiene tabla de tallas cargada · no podemos recomendarte"* |

Decir "no podemos recomendarte" cuando no hay datos genera más confianza que inventar una talla.
Es RN-001, y es también la diferencia entre un producto que se usa dos veces y uno que se usa siempre.
