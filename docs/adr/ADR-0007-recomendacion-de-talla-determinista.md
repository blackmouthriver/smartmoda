# ADR-0007 · La recomendación de talla es determinista, no un modelo

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + IA + PO

## Contexto

La recomendación de talla es **la promesa central del producto**. El KPI de `EP-05` es "confirmación
de talla recomendada ≥ 75% en piloto". Si esto falla, el resto del producto no importa.

Hay dos formas de construirlo:

| Enfoque | Cómo |
|---|---|
| **Determinista** | Comparar las medidas del usuario contra la tabla de tallas versionada de la marca |
| **Estadístico / ML** | Aprender de compras y devoluciones históricas qué talla acierta |

El enfoque ML es el que suena más moderno, y es lo que hacen varios productos del sector. Pero
requiere algo que no tenemos: **histórico de devoluciones por talla**. En el Sprint 4 hay cero.

## Decisión

**Motor determinista en el MVP. El aprendizaje se añade después como capa de ajuste, nunca como
reemplazo.**

```
entrada:  medidas del usuario (mm) + preferencia de ajuste
          + size_chart_version aplicable a esa marca/categoría/país
proceso:  para cada talla candidata:
            para cada medida clave de la categoría (pecho, cintura, cadera, entrepierna):
              calcular distancia al rango [min_mm, max_mm]
            puntuar y ordenar
salida:   talla recomendada
          + confianza (HIGH / MEDIUM / LOW / NONE)
          + explicación legible por una persona
          + alternativa si está entre dos tallas
          + size_chart_version_id  (reproducibilidad, RN-015)
```

### Confianza

| Nivel | Condición |
|---|---|
| `HIGH` | Todas las medidas clave dentro del rango de una sola talla |
| `MEDIUM` | Medidas clave repartidas en dos tallas adyacentes, o falta una medida |
| `LOW` | Faltan varias medidas, o hay dispersión entre tallas no adyacentes |
| `NONE` | No hay tabla aplicable ⇒ **se informa, no se recomienda** (RN-001) |

### Dónde vive

En `shared-domain`, Kotlin puro. Corre en el dispositivo (funciona sin conexión), en el servidor (para
web y kiosco) y en el navegador compilado a WebAssembly. **Una implementación, un conjunto de
pruebas, el mismo resultado en los tres canales.**

### Cuándo se añade aprendizaje

Cuando exista histórico suficiente (estimado: Fase 3, tras el piloto), se añade una **capa de ajuste**:

```
talla_final = ajuste_aprendido( talla_determinista, marca, categoría, señales )
```

Con tres condiciones:
1. El ajuste **nunca** cambia más de una talla respecto al determinista.
2. Si el ajuste no supera de forma medible al determinista en el conjunto de validación, no se activa.
3. La explicación mostrada al usuario sigue siendo la del determinista, más el ajuste explicitado.

## Consecuencias

**Positivas**
- **Explicable**: *"tu pecho está en el rango de la M, tu cintura entre M y L"*. Un modelo dice "M" y
  no puede justificarlo. En una decisión de compra, la justificación es la mitad del valor.
- **Reproducible**: guarda la versión de tabla y de algoritmo (RN-015). Una recomendación de hace seis
  meses se puede reconstruir exactamente. Con un modelo reentrenado, no.
- **Gratuito**: cero costo por invocación. Esto es lo que hace posible la Fase 1 con inversión cero.
- **Funciona sin conexión** (RNF-07).
- **Auditable**: se puede demostrar por qué se recomendó lo que se recomendó, ante el usuario y ante
  un cliente empresarial.
- **Honesto ante la ausencia de datos**: si no hay tabla, dice que no puede recomendar. Un modelo
  siempre devuelve algo, aunque no tenga base.

**Negativas**
- Depende críticamente de la **calidad de las tablas de tallas** (riesgo `R-03`). Una tabla mal
  cargada produce recomendaciones malas, y el usuario culpa al producto, no a la marca.
  Mitigación: validación en la carga, versión, fuente registrada, y `NONE` en lugar de inventar.
- No aprende de los errores por sí solo. El bucle de mejora es el feedback (`US-0706`) y las
  devoluciones (`US-1305`), procesados de forma explícita.
- No captura efectos que las tablas no representan: elasticidad del tejido, patronaje, corte real vs.
  declarado. La preferencia de ajuste solo lo aproxima.
- Requiere que cada marca cargue su tabla. Es fricción real en el onboarding (`US-1104`).

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| ML desde el inicio | No hay datos de entrenamiento en el Sprint 4. Un modelo sin datos es una tabla determinista con ruido añadido y sin explicación |
| Proveedor externo de recomendación de talla | Costo por consulta (rompe la inversión cero), dependencia crítica en la promesa central, y las medidas tendrían que salir del dispositivo, contradiciendo ADR-0003 |
| Tabla única normalizada para todas las marcas | Las tallas no son comparables entre marcas: es precisamente el problema que el producto resuelve. Una "M universal" sería una mentira útil que destruye la confianza al primer error |
| Preguntar al usuario su talla habitual y usarla | Es la línea base contra la que hay que compararse, no la solución. Pero **es un buen valor de respaldo** cuando la confianza es `LOW`, y conviene ofrecerlo |
