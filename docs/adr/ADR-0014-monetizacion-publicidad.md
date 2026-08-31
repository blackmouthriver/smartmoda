# ADR-0014 · Monetización: publicidad contextual + licencia por negocio

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** PO + Arquitectura + Privacidad

## Contexto

El sponsor define el modelo de ingresos:

> *"para la monetización, la idea primero que sea para usuario o clientes usar banners fijos y también
> por ingresos por anunciantes que quieran mostrar sus productos. También por licencia de uso por
> negocio o sucursales"*

Y confirma que **SmartModa es un marketplace**: el usuario compra dentro, y el espejo también lleva
carrito.

Esto cambia el modelo de negocio que yo había supuesto (suscripción SaaS por SKU) y tiene una
consecuencia arquitectónica que conviene ver de frente.

## La tensión que hay que resolver

El negocio publicitario funciona mejor cuanto mejor conoce al usuario. Y la arquitectura de este
producto está construida sobre lo contrario: **el servidor no tiene los datos corporales**
([ADR-0003](ADR-0003-almacenamiento-perfil-corporal.md)) y el espejo es anónimo
([ADR-0011](ADR-0011-espejo-inteligente-anonimo.md)).

Un anunciante preguntará, tarde o temprano, si puede segmentar por talla o por tipo de cuerpo. La
respuesta tiene que estar decidida antes de que lo pregunte con un contrato encima de la mesa.

## Decisión

**Publicidad contextual. Nunca segmentación por datos corporales.**

### Qué se puede usar para decidir qué anuncio mostrar

| Señal | ¿Se usa? | Por qué |
|---|---|---|
| Categoría que el usuario está viendo | **Sí** | Contexto de navegación, no dato personal |
| Marca o tienda que está viendo | **Sí** | Idem |
| Ciudad o tienda física | **Sí** | Necesario para anunciar comercio local |
| Momento y temporada | **Sí** | Contexto |
| Idioma y moneda | **Sí** | Configuración |
| Preferencias de estilo declaradas | **Sí, si el usuario aceptó personalización** | Dato declarado, revocable (RN-007) |
| **Medidas corporales** | **No, nunca** | El servidor no las tiene, y no las va a tener |
| **Talla recomendada** | **No** | Es un dato derivado del cuerpo. Anunciar por talla es anunciar por cuerpo |
| **Silueta, avatar, fotos** | **No, nunca** | — |
| **Sesiones del espejo** | **No** | Anónimas y no correlacionables por diseño (`EN-1408`) |

**Regla nueva RN-038:** *ninguna decisión publicitaria puede tomarse a partir de medidas corporales,
talla recomendada, silueta, avatar o imágenes del usuario. La segmentación es contextual y, como
máximo, por preferencias declaradas con consentimiento revocable.*

Esto no es solo una postura ética: es la única forma de que la arquitectura siga siendo coherente.
Si mañana se segmentara por talla, habría que subir las medidas al servidor, y todo el edificio de
privacidad —que es también la defensa regulatoria del producto— se cae.

### Formatos publicitarios

| Formato | Dónde | Regla |
|---|---|---|
| **Banner fijo** | Zonas designadas del catálogo y del inicio | Nunca sobre la foto del producto ni sobre el probador |
| **Producto patrocinado** | Listados y resultados de búsqueda | **Etiquetado visible** (RN-012, componente `SponsoredLabel`) |
| **Marca destacada** | Sección de descubrimiento | Etiquetado |
| **Banner en el espejo** | Pantalla en reposo, entre sesiones | **Nunca durante una sesión activa de probador** |

Límites duros, no configurables por el tenant:

1. **Ningún anuncio dentro del probador.** Ni en el móvil, ni en el espejo, ni sobre el avatar. El
   momento en que el usuario se está viendo con una prenda es el momento en que el producto se gana la
   confianza; meter publicidad ahí la destruye.
2. **Un anuncio patrocinado nunca viola las restricciones del usuario** (RN-012): si excluyó una
   categoría o un material, no se le anuncia.
3. **Un producto patrocinado sin stock no se muestra** (RN-011). Vale para publicidad igual que para
   recomendación.
4. **El etiquetado de patrocinio no es opcional** y no depende del idioma ni del tema del tenant.
5. **Máximo un anuncio visible por pantalla** en el catálogo, coherente con el principio 90/7/3 del
   sistema de diseño: la interfaz no compite con la mercancía, y la publicidad tampoco.

### Modelo de licencia por negocio

Sustituye a la suscripción por SKU que yo había supuesto:

| Dimensión de cobro | Nota |
|---|---|
| **Licencia por negocio (tenant)** | Cuota base |
| **Licencia por sucursal** | Escala con el tamaño real del comercio, que es como el comercio piensa su costo |
| **Licencia por espejo instalado** | El espejo tiene costo de hardware y de soporte propios |
| Cuotas técnicas (SKU, almacenamiento, llamadas de IA) | Siguen existiendo como **límites**, no como unidad de cobro |

Cobrar por sucursal es más comprensible para el comerciante que cobrar por SKU, y se alinea con cómo
crece el negocio. `US-1603` (planes, prueba y límites) se ajusta a esto.

### Marketplace confirmado

El usuario compra dentro de SmartModa (`US-0905`), y el espejo lleva carrito. Con una restricción de
seguridad para el espejo:

**En el espejo nunca se introducen datos de pago.** Es una pantalla compartida en un espacio público.
El carrito del espejo termina de una de estas dos formas:

```
Carrito en el espejo
   ├─► Código de un solo uso ──► pagar en la caja de la tienda
   └─► QR ──► el carrito pasa al MÓVIL del cliente ──► paga ahí
```

Ambas ya existen en el diseño (`US-1409` para el código, `US-1402` para el QR): el carrito del espejo
las reutiliza en lugar de inventar un flujo de pago nuevo.

## Consecuencias

**Positivas**
- Ingresos desde la Fase 1 sin depender de que haya comercios pagando licencia todavía, que es
  relevante porque **aún no hay comercio piloto**.
- La publicidad contextual es compatible con la arquitectura de privacidad; no obliga a mover ni un
  dato corporal al servidor.
- Cobrar por sucursal es más vendible que cobrar por SKU.
- La posición sobre segmentación está escrita **antes** de la primera negociación con un anunciante,
  que es cuando hay que tenerla.

**Negativas**
- La publicidad contextual rinde menos por impresión que la conductual. Es un costo de ingresos
  asumido a cambio de coherencia y de menor riesgo regulatorio.
- Habrá presión comercial para segmentar por talla. Este ADR es la respuesta preparada.
- El inventario publicitario necesita gestión: campañas, límites, facturación, métricas. Es trabajo
  nuevo (~34 SP, EP-20).
- Un marketplace con pago propio implica pasarela, conciliación y responsabilidad sobre la
  transacción (`US-0905`, `EN-0908`) desde Fase 3.
- Publicidad y experiencia compiten: cada anuncio ocupa espacio que podría ser producto. Los límites
  duros de arriba existen para que esa tensión no se resuelva siempre a favor del anuncio.

## Historias nuevas · EP-20 Publicidad y anunciantes

| ID | Historia | Prio | SP |
|---|---|---|---|
| `US-2001` | Espacios publicitarios y banners en la app | Should | 8 |
| `US-2002` | Gestión de anunciantes y campañas en el backoffice | Should | 8 |
| `EN-2003` | Segmentación contextual sin datos corporales | Must | 8 |
| `US-2004` | Métricas de campaña para el anunciante | Should | 5 |
| `US-2005` | Facturación de campañas publicitarias | Could | 5 |

Total **34 SP**. Fase 3, salvo `EN-2003`, que debe existir antes que el primer anuncio.

## Riesgo asociado

**`R-30` · Presión para segmentar publicidad por datos corporales.** Un anunciante ofrece más dinero
por segmentar por talla. Aceptar obliga a subir medidas al servidor y desmonta
[ADR-0003](ADR-0003-almacenamiento-perfil-corporal.md), la defensa regulatoria y el argumento de
confianza. Mitigación: RN-038 como regla de dominio verificada por prueba, no como política escrita.
