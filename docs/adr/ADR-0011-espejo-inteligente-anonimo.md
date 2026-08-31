# ADR-0011 · Espejo inteligente: medición automática anónima y analítica sin perfil

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + Privacidad + PO
- **Relacionado:** [ADR-0003](ADR-0003-almacenamiento-perfil-corporal.md), [ADR-0005](ADR-0005-canales-y-plataformas.md), [ADR-0006](ADR-0006-probador-progresivo.md)

## Contexto

Alcance confirmado por el usuario:

> *"para la web sería para más adelante para las tiendas físicas que deseen implementar una función
> en donde un cliente mediante una cámara y un TV o pantalla táctil tome las medidas de manera
> automática y que simule el espejo, se vea, y que también pueda escoger de manera táctil la prenda y
> se la muestre en tiempo real. Para esta función no debe guardar datos de usuario en un perfil, sino
> que esa información que se recoja automáticamente alimente la analítica y predicción de los
> productos de la tienda, y que de acuerdo a lo que se compre vs. lo que se midió la gente contraste."*

Esto define un canal con propiedades muy distintas al móvil:

| | Móvil | Espejo en tienda |
|---|---|---|
| Identidad | Usuario autenticado | **Anónimo, siempre** |
| Perfil | Persiste local | **No existe** |
| Medición | Manual (Fase 1) o asistida (Fase 2) | **Automática, es la única vía** |
| Cámara | Sostenida a mano, distancia variable | **Fija, distancia y luz controladas** |
| Sesión | Continua en el tiempo | **Efímera, minutos** |
| Propósito del dato | Servir al usuario | **Servir a la analítica de la tienda** |
| Entorno | Privado | **Espacio público** |

La diferencia de identidad es la que gobierna todo el diseño: **sin usuario no hay a quién pedirle
consentimiento en una pantalla de registro**, y sin embargo se están midiendo cuerpos.

## Decisión

**El espejo es un canal anónimo por construcción, que agrega en el borde y nunca persiste una medida
individual atribuible a una persona.**

### 1. Arquitectura del canal

```
Cámara fija + TV/pantalla táctil (aplicación web en modo quiosco)
        │
        │  todo el procesamiento de imagen ocurre EN EL DISPOSITIVO
        ▼
  Detección de pose + estimación de medidas (calibrada al espacio)
        │
        ├─► Uso inmediato: silueta + talla + prenda superpuesta en tiempo real
        │                  (vive solo en memoria durante la sesión)
        │
        └─► AGREGACIÓN EN EL BORDE
                 medidas ─► banda de talla por categoría
                 ("talla M en camisas", nunca "pecho 94 cm")
                          │
                          ▼
                 Evento anónimo al servidor:
                 { store_id, session_hash, size_band, category,
                   variant_id probado, duración, resultado }
        │
        ▼
  FIN DE SESIÓN ─► borrado verificable de todo (RN-013, EN-1404)
                   fotogramas, pose, medidas, silueta: nada sobrevive
```

**Ningún fotograma sale del dispositivo. Ninguna medida individual se persiste, ni siquiera anónima.**

### 2. Por qué la medición automática es más viable aquí que en el móvil

Contraintuitivo pero cierto: una instalación fija es un entorno **calibrable**.

| Variable | Móvil | Espejo |
|---|---|---|
| Distancia cámara-persona | Desconocida, cambia | **Fija y conocida** |
| Altura y ángulo de cámara | Variables | **Fijos, medidos en la instalación** |
| Iluminación | Impredecible | **Controlada** |
| Fondo | Cualquiera | **Controlado** |
| Referencia de escala | Hay que pedirla al usuario | **El espacio se calibra una vez** |

La instalación incluye una **calibración por dispositivo** con un patrón de referencia físico. Eso
elimina la principal fuente de error de la estimación automática, que es no conocer la escala.

**Consecuencia importante:** en el móvil la medición automática es *asistencia* y el usuario puede
corregirla (RN-005). En el espejo **es la única entrada** —nadie va a teclear sus medidas en una
pantalla de tienda—, así que el listón de precisión es más alto y el fallback debe estar diseñado:
si la confianza es baja, el espejo muestra el probador sin recomendación de talla, no una talla
inventada (RN-001).

### 3. Anonimato: qué significa exactamente

| Garantía | Cómo se cumple |
|---|---|
| Sin identidad | No hay registro, ni login, ni correo, ni teléfono |
| Sin rostro | El espejo **nunca** genera ni muestra rostro, aunque el usuario tenga la app (US-0307 CA-7) |
| Sin reconocimiento | No hay plantilla biométrica, ni comparación entre sesiones, ni conteo de visitantes recurrentes |
| Sin medidas persistidas | Solo bandas de talla agregadas salen del dispositivo |
| Sesión no correlacionable | `session_hash` aleatorio por sesión, sin derivación de nada estable |
| Borrado verificable | Al terminar, con evidencia auditable (`EN-1404`) |
| Sin restauración | La sesión siguiente arranca vacía (RN-013) |

Lo que **no** hace el espejo, y conviene decirlo explícitamente porque es la tentación obvia:
no identifica clientes recurrentes, no cuenta personas únicas, no vincula una sesión con otra, y no
construye un perfil implícito. Cualquiera de esas cosas convertiría un sistema anónimo en un sistema
de vigilancia, con un marco legal completamente distinto.

### 4. Consentimiento en espacio físico

Aquí está el problema legal real, y no se resuelve con una casilla en una pantalla.

La Superintendencia de Industria y Comercio ya se ha pronunciado sobre tratamiento biométrico en
accesos: exige autorización **previa, expresa, informada y cualificada**, y ha ordenado habilitar
mecanismos alternativos que no impliquen ese tratamiento.

Controles adoptados:

1. **Señalización visible** en el punto de instalación, antes de que la persona entre en el campo de
   la cámara: qué hace el sistema, qué se procesa, que nada se guarda, y quién es el responsable.
2. **La cámara está apagada hasta que alguien toca la pantalla.** No hay captura pasiva de quien pasa
   por delante. Esto es la diferencia entre un probador y una cámara de vigilancia.
3. **Consentimiento en pantalla** antes de activar la cámara, con texto corto y una alternativa clara
   (explorar el catálogo sin cámara).
4. **Alternativa siempre disponible**: elegir prenda y ver talla por catálogo, sin medición.
5. **Aviso permanente en pantalla** mientras la cámara está activa.
6. **Botón de terminar sesión visible en todo momento**, y expiración por inactividad.
7. **Revisión jurídica obligatoria antes de la primera instalación.** Este control no es opcional y
   bloquea el despliegue.

### 5. Analítica: el contraste medido vs. comprado

Es el propósito declarado del canal. La cadena es:

```
Sesión del espejo (anónima)
   └─► "alguien de banda M probó la variante X durante 40 s en la tienda Y"
                 │
                 ▼
        ¿Se compró esa variante en esa tienda?
                 │
     ┌───────────┴────────────┐
     ▼                        ▼
  Vía A: ticket con QR    Vía B: correlación agregada
  (el espejo emite un     (el POS reporta ventas; se contrasta
   código, la caja lo      por SKU + talla + ventana temporal,
   escanea)                nunca por persona)
```

**La vía A da atribución exacta; la vía B da correlación estadística.** Cuál se usa depende de si el
comercio puede modificar su punto de venta, y es una de las preguntas abiertas de abajo.

Lo que alimenta:

| Métrica | Qué revela | Historia |
|---|---|---|
| Bandas de talla que se prueban en tienda | Demanda real, incluida la de quien no compró | `US-1310` |
| Bandas que se prueban y **no** hay en stock | Venta perdida por curva de tallas, hoy invisible | `US-1310` |
| Prendas más probadas en el espejo | Interés físico, distinto del interés digital | `US-1307` |
| Contraste probado vs. comprado por tienda | Dónde se pierde la venta en el punto físico | `US-1306`, `US-1409` |
| Tiempo de sesión y abandono | Calidad del espejo y del catálogo | `EN-1302` |

Esto es información que un retailer **hoy no tiene de ninguna forma**: sus ventas solo le dicen qué
compró quien encontró su talla, nunca qué buscó quien no la encontró y se fue.

### 6. Tecnología: web, no APK

El espejo pasa a ser una **aplicación web en modo quiosco** sobre el TV o la pantalla táctil, no una
app Android. Razones:

- El hardware de señalización digital en tienda es heterogéneo (Android TV, mini-PC con Windows,
  paneles con navegador embebido). Web funciona en todos.
- Actualizar 50 pantallas es publicar, no distribuir un APK a 50 dispositivos.
- La pila de visión en navegador (MediaPipe Tasks Vision + WebGL/WebGPU) ya estaba prevista para el
  retirado `US-0618`; se reutiliza aquí, que es donde tiene sentido.
- El modo pantalla completa y el bloqueo de navegación son estándar en navegadores de señalización.

**Requisito de hardware que hay que fijar antes de comprometer nada:** el pipeline de visión en tiempo
real necesita GPU. Un panel comercial de gama baja no lo sostiene. La matriz de hardware soportado es
parte del spike (ver abajo).

## Consecuencias

**Positivas**
- Alinea el producto con lo que pediste, y elimina la contradicción entre "no guardar perfil" y
  "alimentar la analítica": se resuelve agregando en el borde.
- Riesgo regulatorio muy inferior al de un sistema identificado.
- Da al retailer el dato que justifica el precio del SaaS, sin exponer a nadie.
- La instalación fija hace la medición automática **más precisa** que en el móvil.
- Reutiliza la pila web que ya estaba planificada.
- La sesión anónima es más simple de implementar que una autenticada.

**Negativas**
- **La medición automática es la única entrada**, así que su precisión es crítica. Si falla, el
  espejo no sirve. Necesita su propio spike y su propio gate.
- Sin identidad no se puede medir retención, ni recurrencia, ni recorrido del cliente. Es una
  renuncia deliberada.
- La atribución medido→comprado por la vía B es estadística, no exacta. Suficiente para tendencias,
  insuficiente para afirmaciones individuales.
- Requiere hardware con GPU en tienda, y una calibración física por instalación.
- El consentimiento en espacio físico exige revisión jurídica que hoy no está hecha.
- La calibración se puede desajustar (alguien mueve la cámara). Necesita verificación periódica y
  detección de descalibración.

## Impacto en el backlog

### Se retira
`US-0618` (probador web por cámara para consumidor, 13 SP) — su contenido se reubica.

### Se añaden a EP-14

| ID | Historia | Prio | SP |
|---|---|---|---|
| `SP-1405` | Spike: precisión de medición automática calibrada en instalación fija | Must | 13 |
| `US-1406` | Medición automática anónima en el espejo | Must | 13 |
| `US-1407` | Probador en tiempo real en el espejo con selección táctil | Must | 13 |
| `EN-1408` | Agregación en el borde y no persistencia de medidas | Must | 8 |
| `US-1409` | Contraste probado vs. comprado en tienda física | Must | 8 |
| `EN-1410` | Consentimiento y señalización en espacio físico | Must | 5 |
| `EN-1411` | Calibración de la instalación y detección de descalibración | Must | 8 |

**Neto:** +68 SP, −13 SP retirados = **+55 SP**.

`US-1402` (emparejamiento por QR) y `US-1403` (continuar en el móvil) **se conservan pero pasan a
opcionales del usuario**: solo si la persona quiere llevarse su look al teléfono. Por defecto no se
guarda nada, que es exactamente lo que pediste.

## Gate

Igual que con los activos 3D, esto necesita un punto de decisión con evidencia:

```
SP-1405 · Spike de medición automática calibrada
   Debe responder con NÚMEROS:
     ¿qué error medio y qué error máximo tiene la estimación
      contra medición manual con cinta, en una instalación calibrada?
   │
   ├─► error dentro del umbral que hace útil la talla
   │      └─► el espejo continúa
   └─► error fuera del umbral
          └─► el espejo se reorienta: probador visual + selección táctil,
              SIN recomendación de talla automática.
              Sigue siendo valioso; simplemente no promete lo que no cumple (R-02)
```

---

## Respuestas del usuario · 31-ago-2026

Las cuatro preguntas que abría este ADR quedan respondidas así:

| # | Pregunta | Respuesta | Consecuencia |
|---|---|---|---|
| 1 | Atribución de la venta | **Ambas vías: escaneo y manual** | Se diseña un código corto que la caja puede *escanear* o *teclear*. La correlación agregada queda como tercera red de seguridad |
| 2 | Hardware | **Sin definir; se piden sugerencias** | Ver § "Hardware recomendado" abajo y [13 § 7](../13-stack-tecnologico.md) |
| 3 | Umbral de precisión | **"Lo más preciso posible"** | No es una especificación. Se convierte en umbrales por nivel de confianza, y **la precisión pasa a ser una decisión de hardware**. Ver § "Umbrales" |
| 4 | Ubicación | **Dentro de la tienda, cerca del vestier** | Mejora consentimiento, iluminación y contexto. Introduce una restricción física dura: **espacio libre** |

---

## 1. Atribución: código escaneable y tecleable

El espejo emite al final de la sesión un **código corto de un solo uso**:

```
Formato:  4 letras + 4 dígitos   →   VSTA-4172
          Alfabeto sin caracteres ambiguos (sin O/0, I/1, S/5)
          Impreso grande en pantalla + como QR
          Vigencia: 2 horas o hasta usarse
```

Tres vías de captura, en orden de preferencia:

| Vía | Cómo | Atribución | Requiere del comercio |
|---|---|---|---|
| **A · Escaneo** | La caja escanea el QR desde la pantalla o desde el móvil del cliente | **Exacta** | Lector de códigos, ya presente en casi toda caja |
| **B · Manual** | El cajero teclea `VSTA-4172` en el POS o en una pantalla auxiliar | **Exacta** | Solo un campo de texto y 5 segundos |
| **C · Correlación** | Sin código: se cruza por SKU + talla + tienda + ventana temporal | **Estimada** | Nada |

La vía B es la que hace esto viable en un comercio pequeño: **no requiere integrar nada**. Basta una
pantalla auxiliar (una tableta barata en la caja) donde el cajero teclea el código antes de cerrar la
venta. Si no lo teclea, se cae a la vía C sin romper nada.

**El tablero siempre indica qué vía se usó** (`US-1409` CA-4 y CA-5). Una correlación presentada como
atribución exacta lleva a decidir compras sobre un dato más débil de lo que parece (`R-26`).

### Por qué el código no identifica a nadie

`VSTA-4172` es aleatorio, de un solo uso y de vida corta. No deriva del `session_hash` ni de nada de
la persona. Vincula **una sesión con una venta**, no una persona con un historial. Al canjearse o
expirar, deja de existir. El anonimato de `EN-1408` se mantiene intacto.

---

## 2. Umbrales de precisión

### Por qué "lo más preciso posible" no se puede implementar

Es una intención, no un criterio. Sin un número, `SP-1405` no puede concluir nada y el equipo no sabe
cuándo parar de optimizar. Peor: lleva a prometer una precisión que la cámara no puede dar, que es
exactamente el riesgo `R-02`.

La pregunta correcta es: **¿cuánto error tolera una recomendación de talla antes de dejar de ser
útil?** Y eso sí tiene respuesta, porque depende de algo medible: el ancho de las bandas de talla.

```
Entre dos tallas contiguas hay, típicamente:
   pecho / busto     4 – 8 cm
   cintura           4 – 6 cm
   cadera            4 – 6 cm

Si el error de medición es de ±3 cm y la banda mide 5 cm,
la persona puede caer en la talla equivocada solo por el error.
```

Regla derivada: **el error debe ser sensiblemente menor que la mitad de la banda más estrecha.**
Con bandas de 4–6 cm, eso sitúa el objetivo alrededor de **±1,5 cm**.

### Umbrales propuestos

En lugar de un umbral único, se mapean al modelo de confianza que ya existe (RN-016, ADR-0007):

| Error en medidas clave | Confianza | Qué hace el espejo |
|---|---|---|
| **≤ ±1,5 cm** | `HIGH` | Recomienda talla con explicación |
| **±1,5 a ±3,0 cm** | `MEDIUM` | Recomienda talla **e indica la contigua** como alternativa |
| **±3,0 a ±5,0 cm** | `LOW` | Muestra rango de tallas probable, **sin recomendar una** |
| **> ±5,0 cm** o pose inválida | `NONE` | Probador visual únicamente. No recomienda (RN-001) |

Medidas clave por categoría: superior → pecho, hombro, largo de brazo. Inferior → cintura, cadera,
entrepierna.

### Criterio de aprobación del gate `SP-1405`

```
APRUEBA si, sobre un panel diverso de ≥ 30 personas:
   ≥ 80% de las mediciones caen en ±2,0 cm en pecho, cintura y cadera
   y ninguna supera ±4,0 cm
   y no hay degradación relativa por segmento (contextura, altura, tono de piel)

Si no aprueba ⇒ el espejo NO recomienda talla.
   Sigue siendo un probador visual válido y valioso.
   No se promete lo que no se cumple.
```

### La verdad incómoda sobre la precisión

Estos umbrales no dependen del software, sino de tres cosas por orden de peso:

| Factor | Impacto | Se controla con |
|---|---|---|
| **1. Escala conocida** | Enorme | **Cámara con profundidad** o calibración física rigurosa. Sin escala real, el error no baja de varios centímetros por mucho modelo que se ponga |
| **2. Ropa que lleva puesta** | Muy alto | Detección y degradación de confianza. **Ninguna cámara mide bien a alguien con abrigo.** Cerca del vestier ayuda: la gente se quita el abrigo |
| **3. Iluminación y fondo** | Alto | Instalación fija con luz difusa frontal y fondo liso contrastante |

Traducción práctica: **la precisión es una decisión de hardware, no de algoritmo.** Con una webcam RGB
corriente, el error habitual en medición corporal se sitúa en el rango de varios centímetros, es decir
`LOW`. Con **cámara de profundidad + calibración + condiciones controladas**, el rango `HIGH`/`MEDIUM`
es alcanzable.

Si el objetivo es "lo más preciso posible", la respuesta es: **cámara de profundidad**. Todo lo demás
es secundario. Los números concretos los tiene que confirmar `SP-1405` en el montaje real; no se
pueden dar por sentados desde un documento.

---

## 3. Hardware recomendado

Especificación completa y alternativas en [13 § 7](../13-stack-tecnologico.md). Resumen de la
recomendación:

| Componente | Recomendación | Por qué |
|---|---|---|
| **Cámara** | **RGB-D (con profundidad)**: Intel RealSense D435i/D455, Orbbec Femto o equivalente | Es el factor que más determina la precisión. Resuelve la escala sin depender de suposiciones |
| **Cómputo** | Mini-PC con GPU (integrada moderna o dedicada de entrada), 16 GB RAM | El pipeline en tiempo real necesita GPU. Un panel de señalización de gama media **no lo sostiene** |
| **Pantalla** | Táctil comercial 43–55", **en vertical** | Un espejo de cuerpo entero es vertical. En horizontal la persona sale pequeña y la ilusión se pierde |
| **Iluminación** | Panel LED difuso frontal, temperatura neutra | Segunda variable de precisión, y además la prenda se ve como es |
| **Fondo** | Superficie lisa, mate, de color uniforme y contrastante | Mejora la segmentación de silueta |
| **Suelo** | Marca física a distancia fija de la cámara | Parte de la calibración (`EN-1411`) |

**Lo que descartaría:** los paneles "todo en uno" de señalización digital con Android. Son cómodos de
instalar y baratos, pero su GPU y su navegador embebido no sostienen visión en tiempo real. Es la
opción que parece más simple y termina cancelando la funcionalidad.

---

## 4. Ubicación: cerca del vestier

Es la mejor de las opciones posibles, y trae una restricción dura.

### Lo que mejora

| Aspecto | Por qué mejora |
|---|---|
| **Consentimiento** | Es una zona semiprivada con intención declarada: quien está ahí ya quiere probarse ropa. Muy distinto de una cámara en la entrada |
| **Precisión** | La gente suele haberse quitado el abrigo, que es la principal fuente de error |
| **Iluminación** | Zona interior, fácil de controlar. Nada de luz solar cambiante |
| **Contexto de uso** | Complementa la cola del vestier en lugar de competir con ella: es exactamente el caso de uso que planteaste |
| **Conversión** | La persona ya está en modo de decisión de compra |

### Las tres restricciones que hay que verificar **antes de comprar hardware**

1. **Espacio libre.** Una cámara necesita distancia para captar un cuerpo entero: como referencia,
   entre 2,5 y 3 m entre la cámara y la marca del suelo, más espacio para que la persona se mueva. Los
   pasillos de vestier suelen ser estrechos. **Medir el sitio real antes de decidir nada.** Si no hay
   espacio, hay que cambiar de ubicación o usar óptica gran angular, que introduce distorsión y
   perjudica la precisión.

2. **Campo de visión.** La cámara **no puede** encuadrar puertas de probador, cortinas ni la zona
   donde alguien pueda estar cambiándose. Es un requisito de diseño de la instalación, no una
   recomendación: se verifica en la puesta en marcha y se documenta con una foto del encuadre.

3. **Cola y rotación.** Cerca del vestier habrá gente esperando. La expiración por inactividad debe
   ser corta y el botón de terminar muy visible, para que la sesión de una persona no bloquee a la
   siguiente.

### Impacto en las historias

- `EN-1410` (consentimiento en espacio físico): la señalización va **en el acceso a la zona de
  vestier**, no solo en la pantalla, y se añade un criterio sobre el encuadre.
- `EN-1411` (calibración): incorpora la verificación de distancia mínima y de campo de visión.
- `US-1407`: expiración por inactividad corta, adecuada a una zona con cola.

---

## Preguntas que esta decisión abre

1. **Atribución de la venta.** ¿El comercio puede modificar su punto de venta para escanear un código
   emitido por el espejo (vía A, exacta)? ¿O nos quedamos con correlación agregada (vía B)?
2. **Hardware.** ¿Qué pantallas y qué equipos? Determina si el pipeline en tiempo real es viable.
3. **Umbral de precisión.** ¿Qué error de medida hace que una recomendación de talla siga siendo útil
   para el negocio? Es el criterio del gate y debe fijarlo producto, no ingeniería.
4. **Ubicación física.** ¿El espejo va en el vestier (privado) o en la sala (público)? Cambia por
   completo el análisis de consentimiento y de aceptación social.
