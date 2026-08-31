# 16 · Sistema de diseño

Absorbe íntegro el documento *Sistema de color — Marketplace de moda v1* y añade lo que le faltaba
para ser ejecutable: **tipografía, espaciado, elevación, movimiento, tokens generables y estrategia de
personalización por tenant**.

---

## 1. Los tres principios (del documento original)

### 90 / 7 / 3
90% neutros, 7% acento, 3% semánticos. En retail el color dominante debe ser el de la mercancía, no el
del contenedor. La interfaz aloja catálogos de decenas de marcas con estética propia: si la UI tiene
personalidad cromática fuerte, pelea con cada foto.

### Neutro cálido, no gris
Neutros con croma 0,006–0,012 en hue 70°. El gris puro (croma 0) enfría la piel en las fotos
editoriales y hace ver amarillentos los blancos de estudio. El hueso resuelve ambas situaciones.
La paleta se define en **OKLCH** porque la luminosidad es perceptual: dos colores con la misma L pesan
visualmente igual aunque cambien de hue.

### Un acento, dos trabajos
**Arcilla** marca la acción y el deseo. **Índigo** marca lo institucional y la confianza. Dos hues
complementarios divididos (40° y 265°): contraste suficiente para distinguirse, sin la estridencia del
complementario exacto.

---

## 2. Neutros — el anfitrión

Nueve pasos en hue 70° (arena), con croma que crece hacia los medios y baja en los extremos.

| Nombre | HEX | OKLCH | Rol |
|---|---|---|---|
| Papel | `#FDFBFA` | `oklch(0.99 0.003 70)` | Fondo base claro |
| Hueso 50 | `#F9F6F2` | `oklch(0.975 0.006 70)` | Fondo alterno |
| Arena 100 | `#F0ECE7` | `oklch(0.945 0.008 70)` | Superficie hundida |
| Arena 200 | `#E2DDD8` | `oklch(0.9 0.009 70)` | Borde suave |
| Arena 300 | `#C8C3BD` | `oklch(0.82 0.01 70)` | Borde fuerte, divisor |
| Piedra 400 | `#9D9791` | `oklch(0.68 0.011 70)` | Ícono inactivo, marcador de posición |
| Piedra 600 | `#6E6862` | `oklch(0.52 0.012 70)` | Texto secundario (AA) |
| Grafito 800 | `#413C36` | `oklch(0.36 0.012 70)` | Texto de cuerpo |
| Tinta 950 | `#17130E` | `oklch(0.19 0.012 70)` | Titulares, botón oscuro |

## 3. Acento primario — Arcilla (hue 40°)

Terracota, no rojo. Los rojos saturados de moda rápida gritan descuento; la arcilla conserva la
temperatura cálida y suma la asociación de barro, cuero y tierra, que lee artesanal y aspiracional a
la vez. El relleno de botón se fija en luminosidad **0.53** —no 0.62— porque es la frontera donde el
texto blanco alcanza 4,5:1.

| Nombre | HEX | OKLCH | Rol |
|---|---|---|---|
| Arcilla Tint | `#FFEEE8` | `oklch(0.96 0.02 40)` | Fondo de chip |
| Arcilla Base | `#AE471F` | `oklch(0.53 0.145 40)` | Botón, insignia (texto blanco AA) |
| Arcilla Viva | `#CB6440` | `oklch(0.62 0.14 40)` | Solo gráficos y áreas sin texto |
| Arcilla Texto | `#873616` | `oklch(0.44 0.12 40)` | Enlace sobre fondo claro |
| Arcilla Presionado | `#571E08` | `oklch(0.32 0.09 40)` | Estado activo |

## 4. Acento secundario — Índigo (hue 265°)

A la misma luminosidad y croma que la arcilla en cada paso, así ninguno domina por accidente. Frío
contra cálido: sostiene los momentos en que el usuario deja de desear y empieza a decidir —pago,
devoluciones, verificación de vendedor—, que en un marketplace son la mitad de la confianza.

| Nombre | HEX | OKLCH | Rol |
|---|---|---|---|
| Índigo Tint | `#EBF2FF` | `oklch(0.96 0.02 265)` | Banner informativo |
| Índigo Base | `#4266BF` | `oklch(0.53 0.145 265)` | Botón, ícono |
| Índigo Viva | `#5C82DA` | `oklch(0.62 0.14 265)` | Solo gráficos y áreas sin texto |
| Índigo Texto | `#314E94` | `oklch(0.44 0.12 265)` | Enlace institucional |
| Índigo Presionado | `#1C3060` | `oklch(0.32 0.09 265)` | Estado activo |

## 5. Colores semánticos

El riesgo real: arcilla (40°) y error (15°) son vecinos. Se resuelve **por rol, no por color** — el
error nunca aparece como bloque relleno junto a producto; solo como texto, ícono y borde. La arcilla
nunca advierte. Ninguna alerta depende del color solo: siempre lleva ícono y texto.

| Nombre | HEX | OKLCH | Rol |
|---|---|---|---|
| Éxito | `#298646` | `oklch(0.55 0.13 150)` | Pedido confirmado, vendedor verificado |
| Aviso | `#BE8700` | `oklch(0.66 0.14 80)` | Últimas tallas (texto en Tinta 950) |
| Error | `#BD1F44` | `oklch(0.52 0.19 15)` | Pago rechazado — solo texto, ícono y borde |
| Info | `#4266BF` | `oklch(0.53 0.145 265)` | Envío y plazos |

## 6. Modo oscuro

No es la paleta clara invertida. El fondo sube a 0.16 de luminosidad (nunca negro puro: el negro
absoluto hace flotar las fotos con fondo blanco y cansa en scroll largo) y los acentos suben a 0.72
bajando croma, porque a igual croma un color cálido sobre fondo oscuro se percibe fluorescente.

| Nombre | HEX | OKLCH | Rol |
|---|---|---|---|
| Fondo oscuro | `#100D0A` | `oklch(0.16 0.008 70)` | Lienzo |
| Superficie oscura | `#1B1814` | `oklch(0.21 0.009 70)` | Tarjeta |
| Borde oscuro | `#312D28` | `oklch(0.3 0.01 70)` | Divisor |
| Texto sec. oscuro | `#A9A49E` | `oklch(0.72 0.01 70)` | Metadatos |
| Arcilla oscuro | `#DF8C6F` | `oklch(0.72 0.11 40)` | Acción en modo oscuro |
| Índigo oscuro | `#8CAAEB` | `oklch(0.74 0.1 265)` | Info en modo oscuro |

## 7. Verificación de contraste WCAG AA

| Combinación | Razón | Mínimo | Resultado |
|---|---|---|---|
| Texto de cuerpo sobre hueso | 10.12:1 | 4.5:1 | AAA |
| Texto secundario sobre hueso | 5.13:1 | 4.5:1 | AA |
| Enlace arcilla sobre papel | 7.96:1 | 4.5:1 | AAA |
| Texto blanco sobre botón arcilla | 5.42:1 | 4.5:1 | AA |
| Texto blanco sobre botón índigo | 5.24:1 | 4.5:1 | AA |
| Enlace índigo sobre papel | 7.69:1 | 4.5:1 | AAA |
| Texto de error sobre blanco | 8.21:1 | 4.5:1 | AAA |
| Arcilla oscuro sobre fondo oscuro | 7.52:1 | 4.5:1 | AAA |
| Texto secundario en modo oscuro | 7.82:1 | 4.5:1 | AAA |
| Aviso relleno sobre blanco (no textual) | 3.16:1 | 3:1 | AA |

Estas razones se **recalculan en CI** (`AUT-21`). Un token cambiado que rompa un mínimo bloquea el
merge, así el documento no se convierte en una foto de un estado que ya no existe.

## 8. Reglas de uso (del documento original)

- Un solo botón de acción primaria por pantalla, en Arcilla Base.
- Las fotos de producto siempre sobre Papel o Hueso 50, nunca sobre un acento.
- Precios en Tinta 950; el precio con descuento en Arcilla Texto, nunca en rojo semántico.
- Las insignias de urgencia (últimas unidades, oferta del día) son el único uso de Aviso; máximo una
  visible por tarjeta.
- Bordes: Arena 200 dentro de una tarjeta, Arena 300 para separar secciones.
- Cada marca puede tener color propio en su tienda dentro de la app, pero **navegación, carrito y pago
  conservan siempre esta paleta** (RN-027).

---

# Lo que se añade

## 9. Tipografía

El documento original no la define. Propuesta, con la misma lógica de "no competir con la ropa":

| Rol | Familia | Alternativas del sistema | Por qué |
|---|---|---|---|
| **Display / titulares editoriales** | Una serif de transición con contraste moderado (p. ej. *Fraunces*, *Playfair Display*, *Instrument Serif*) | Georgia, serif | La serif es el marcador tipográfico de la moda editorial. Usada **solo** en titulares grandes, no compite con el contenido |
| **Interfaz y cuerpo** | Una sans geométrico-humanista neutra (p. ej. *Inter*, *Manrope*, *Public Sans*) | system-ui, Roboto, SF | Legible en cuerpos pequeños, buena para números y tablas, no aporta personalidad que estorbe |
| **Números tabulares** | La misma sans con `font-variant-numeric: tabular-nums` | — | Precios y tallas alineados en columnas |

Regla: **como máximo dos familias**. La serif aparece en el titular de una pantalla y en poco más.
Una interfaz de comercio con serif en botones y etiquetas se lee lenta.

### Escala tipográfica (razón 1,250 · tercera mayor)

| Token | Tamaño | Interlínea | Peso | Uso |
|---|---|---|---|---|
| `display-lg` | 40 / 2.5rem | 1.1 | 600 serif | Portada, splash |
| `display-md` | 32 / 2rem | 1.15 | 600 serif | Titular de sección |
| `heading-lg` | 25 / 1.563rem | 1.25 | 600 sans | Título de pantalla |
| `heading-md` | 20 / 1.25rem | 1.3 | 600 sans | Título de tarjeta |
| `body-lg` | 18 / 1.125rem | 1.5 | 400 sans | Texto destacado |
| `body-md` | 16 / 1rem | 1.55 | 400 sans | **Cuerpo por defecto** |
| `body-sm` | 14 / 0.875rem | 1.5 | 400 sans | Metadatos |
| `label` | 14 | 1.2 | 500 sans | Botones, etiquetas |
| `caption` | 12 / 0.75rem | 1.4 | 400 sans | Notas legales, avisos de confianza |

Mínimo absoluto: **12 px**, y solo para texto no esencial. Todo debe escalar hasta 200% sin romper el
diseño (RNF-06).

## 10. Espaciado y forma

Escala base de **4 px**:

```
space-0: 0     space-1: 4    space-2: 8    space-3: 12
space-4: 16    space-5: 20   space-6: 24   space-8: 32
space-10: 40   space-12: 48  space-16: 64  space-20: 80
```

| Token | Valor | Uso |
|---|---|---|
| `radius-sm` | 6 px | Chips, campos |
| `radius-md` | 12 px | Tarjetas, botones |
| `radius-lg` | 20 px | Hojas modales, contenedores |
| `radius-full` | 999 px | Avatares, insignias |
| `border-hairline` | 1 px Arena 200 | Dentro de tarjeta |
| `border-section` | 1 px Arena 300 | Entre secciones |

**Elevación:** sombras muy sutiles y cálidas (nunca negro puro con alfa: sobre un fondo hueso, una
sombra gris se ve sucia). En modo oscuro la elevación se expresa con **luminosidad de superficie**, no
con sombra.

`touch-target-min`: **48 dp / 44 px**, sin excepción (RNF-06).

## 11. Movimiento

| Token | Duración | Curva | Uso |
|---|---|---|---|
| `motion-instant` | 80 ms | ease-out | Retroalimentación de presión |
| `motion-quick` | 160 ms | ease-out | Aparición de chip, tooltip |
| `motion-standard` | 240 ms | ease-in-out | Transición de pantalla, hoja modal |
| `motion-slow` | 400 ms | ease-in-out | Splash, transiciones celebratorias |

Reglas:
- Rango permitido en el editor por tenant: **80–600 ms** (US-1704 CA-2).
- `prefers-reduced-motion` / la reducción de movimiento del sistema **anula el preajuste del tenant**
  y deja solo transiciones de opacidad (US-1704 CA-3).
- Ninguna animación bloquea una interacción. Si el contenido está listo, la animación se corta.
- Degradación automática a `sutil` si se pierden fotogramas en la gama baja de la matriz.

## 12. Splash animado (EN-1608)

```
Frame 0    Logo del tenant sobre su color de fondo
   │
   │  la silueta de una persona se resuelve en avatar
   │  (transformación de contorno, no un vídeo)
   ▼
Frame N    Logo asentado + transición al contenido
```

- Se usa la API de splash del sistema operativo. **Sin actividad intermedia propia.**
- Contribución máxima al arranque: **300 ms**. Si el contenido está listo antes, se corta.
- Con reducción de movimiento: logo estático.
- Sin tenant activo: identidad de la plataforma.
- Mensajes base del concepto original: *"Descubre cómo te queda antes de comprar"*, *"Sé tu propio
  modelo"*, *"Tu estilo, tu cuerpo, tu avatar, tu elección"*. El texto final se decide con marca.

## 13. Tokens ejecutables (EN-1903)

Fuente de verdad única en `contracts/design-tokens.json`:

```json
{
  "color": {
    "neutral": {
      "paper":    { "value": "oklch(0.99 0.003 70)" },
      "bone50":   { "value": "oklch(0.975 0.006 70)" },
      "sand100":  { "value": "oklch(0.945 0.008 70)" },
      "sand200":  { "value": "oklch(0.9 0.009 70)" },
      "sand300":  { "value": "oklch(0.82 0.01 70)" },
      "stone400": { "value": "oklch(0.68 0.011 70)" },
      "stone600": { "value": "oklch(0.52 0.012 70)" },
      "graphite800": { "value": "oklch(0.36 0.012 70)" },
      "ink950":   { "value": "oklch(0.19 0.012 70)" }
    },
    "accent": {
      "clayBase": { "value": "oklch(0.53 0.145 40)", "tenantOverridable": true },
      "indigoBase": { "value": "oklch(0.53 0.145 265)", "tenantOverridable": true }
    },
    "semantic": {
      "success": { "value": "oklch(0.55 0.13 150)", "tenantOverridable": false },
      "warning": { "value": "oklch(0.66 0.14 80)",  "tenantOverridable": false },
      "error":   { "value": "oklch(0.52 0.19 15)",  "tenantOverridable": false },
      "info":    { "value": "oklch(0.53 0.145 265)","tenantOverridable": false }
    }
  },
  "contrastPairs": [
    { "fg": "color.neutral.graphite800", "bg": "color.neutral.bone50", "min": 4.5 },
    { "fg": "#FFFFFF", "bg": "color.accent.clayBase", "min": 4.5 }
  ]
}
```

El generador produce:

| Salida | Destino |
|---|---|
| `tokens.css` (variables CSS con bloques de tema claro/oscuro) | Web |
| `VistiaTheme.kt` (`ColorScheme`, `Typography`, `Shapes` de Compose) | Android |
| `tokens.json` por tenant | Bundle servido por CDN, aplicado en caliente |
| `contrast-report.json` | Gate de CI (`AUT-21`) |

`tenantOverridable` es lo que hace verificable la separación entre lo personalizable y lo que no
(doc 10 § 4). Un token semántico marcado como no personalizable **no puede** ser cambiado por el
editor de tema, ni por error ni a propósito.

## 14. Personalización por tenant: qué puede tocar

| Token | ¿Tenant puede cambiarlo? | Restricción |
|---|---|---|
| Acento primario y secundario | Sí | Debe cumplir 4,5:1 con el texto que lo acompaña |
| Neutros | Solo elegir entre variantes aprobadas | El neutro cálido es identidad de plataforma |
| Semánticos (éxito, aviso, error, info) | **No** | Un error debe verse igual en todas las tiendas |
| Tipografía | Sí, entre familias aprobadas | Deben existir alternativas del sistema, y cumplir el mínimo de tamaño |
| Radios y densidad | Sí | Dentro de rangos definidos |
| Logos | Sí | SVG/PNG ≤ 512 KB, con versión para fondo oscuro |
| Movimiento | Sí, por preajuste | 80–600 ms, anulado por reducción de movimiento |
| Colores de navegación, carrito y pago | **No** | RN-027 |

El editor (US-1703) valida el contraste **antes de permitir publicar**, calcula la razón y ofrece el
valor cumplidor más cercano. La accesibilidad no se delega a la buena voluntad del comerciante.

## 15. Catálogo de componentes

| Grupo | Componentes |
|---|---|
| Base | Button (primario/secundario/fantasma/destructivo), IconButton, Chip, Badge, Avatar, Divider |
| Formularios | TextField, NumberField (con unidad), Select, MultiSelect, Switch, Checkbox, RadioGroup, Slider, MeasurementInput |
| Contenedores | Card, ProductCard, Sheet, Dialog, Accordion, Tabs, EmptyState, ErrorState, SkeletonLoader |
| Navegación | TopAppBar, BottomNavigation, NavRail (tableta/escritorio), Breadcrumb, TenantSwitcher |
| Dominio | SizeRecommendationCard, ConfidenceBadge, TryOnCanvas, FitExplanation, StockIndicator, StoreLocationHint, OutfitStrip, ConsentCard, SponsoredLabel |
| Retroalimentación | Toast, InlineAlert, ProgressIndicator, QuotaWarning |
| Datos | DataTable (responsiva), FunnelChart, RankingList, SizeCurveChart, AuditEntry |

Cada componente existe en web **y** en Compose con el mismo nombre y la misma API conceptual; una
lista de paridad se verifica en CI (EN-1903 CA-4).

### Componentes de dominio que no son negociables

- **`ConfidenceBadge`** acompaña *toda* recomendación de talla y *toda* visualización del probador.
  No es decorativo: implementa RN-004 y RN-016. Una pantalla del probador sin él es un defecto.
- **`SponsoredLabel`** aparece en toda recomendación influida por regla comercial (RN-012).
- **`ConsentCard`** es el único componente autorizado para solicitar consentimiento; ninguna pantalla
  improvisa su propio texto legal.

---

## 16. Siguiente paso del sistema de color

El documento original cierra sugiriendo: *escalas completas de estados, tokens de código y aplicación
en pantallas reales*. Los tokens de código quedan resueltos en § 13. Falta:

1. **Escalas completas de estado** (reposo, hover, foco, presionado, deshabilitado, seleccionado) para
   cada componente interactivo — incluido el anillo de foco visible, obligatorio para navegación por
   teclado en web.
2. **Aplicación en pantallas reales**: maquetas de las tres pantallas de referencia (catálogo, detalle,
   probador) en claro y oscuro, que son las que usa la previsualización del editor de tema (US-1703 CA-1).
3. **Paleta de visualización de datos** para los tableros B2B: los acentos de marca no sirven para
   series múltiples. Se necesita una escala categórica accesible y una secuencial, ambas verificadas
   con las mismas reglas de contraste.
