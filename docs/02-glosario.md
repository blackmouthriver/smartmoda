# 02 · Glosario (lenguaje ubicuo)

Este glosario es normativo: los nombres aquí definidos son los que se usan en código, base de datos,
eventos de analítica y documentación. Si un término no está aquí, no debe aparecer en un identificador.

## Dominio de negocio

| Término | Definición | Nombre técnico |
|---|---|---|
| **Tenant** | Unidad de aislamiento comercial y de datos. Una empresa contratante. | `tenant` |
| **Empresa** | Sinónimo de negocio de un tenant. Un tenant = una empresa. | `tenant` |
| **Marca** | Identidad comercial dentro de una empresa. Una empresa puede tener varias. | `brand` |
| **Tienda / Sucursal** | Punto físico o digital con inventario propio. | `store` |
| **Sección** | Agrupación física dentro de una tienda (hombre, mujer, deportivo). | `store_section` |
| **Ubicación en tienda** | Piso + sección + percha donde se encuentra un SKU. | `store_location` |
| **Producto** | Prenda conceptual: "Camisa Oxford manga larga". | `product` |
| **Variante** | Combinación concreta color + talla de un producto. Tiene SKU. | `product_variant` |
| **SKU** | Identificador único de una variante en el inventario. | `sku` |
| **Referencia** | Código interno del comercio para el producto. No es el SKU. | `reference_code` |
| **Activo** | Imagen 2D, modelo 3D (glTF/GLB) o textura asociada a un producto/variante. | `asset` |
| **Tabla de tallas** | Mapa versionado medida corporal → talla, por marca y categoría. | `size_chart` (versionada) |
| **Curva de tallas** | Distribución de stock por talla de un producto en una tienda. | `size_curve` |

## Dominio del usuario

| Término | Definición | Nombre técnico |
|---|---|---|
| **Perfil corporal** | Conjunto de medidas + unidades + preferencia de ajuste. **Dato de alto impacto.** | `body_profile` |
| **Medida** | Valor numérico de una dimensión corporal (busto, cintura, cadera, entrepierna, etc.). | `measurement` |
| **Preferencia de ajuste** | Ceñido / regular / holgado. Influye la explicación, **no** la medida. | `fit_preference` |
| **Avatar** | Representación 3D paramétrica derivada del perfil corporal. | `avatar` |
| **Silueta** | Contorno 2D derivado de la pose. Paso previo al avatar. | `silhouette` |
| **Pose** | 33 landmarks corporales detectados on-device. Nunca sale del dispositivo sin consentimiento. | `pose_landmarks` |
| **Prueba virtual (try-on)** | Evento de visualizar una variante sobre avatar, foto o cámara. | `try_on_session` |
| **Look / Outfit** | Conjunto de variantes combinadas propuesto por el Stylist o armado por el usuario. | `outfit` |
| **Closet** | Prendas propias que el usuario registra, no necesariamente del catálogo. | `closet_item` |
| **Confianza** | Etiqueta `alta / media / baja` que acompaña toda recomendación o visualización. | `confidence_level` |

## Dominio técnico

| Término | Definición |
|---|---|
| **Bounded context** | Frontera de modelo. En este proyecto: Identity, Profile, Catalog, Inventory, Sizing, TryOn, Styling, Commerce, Analytics, Tenancy, Audit. |
| **Habilitador (Enabler)** | Ítem de backlog técnico sin valor directo al usuario final. Prefijo `EN-`. |
| **Spike** | Investigación con caja de tiempo y decisión al final. Prefijo `SP-`. |
| **Gate go/no-go** | Punto de decisión formal donde una línea de trabajo se aprueba o se cancela con evidencia. |
| **Feature flag** | Interruptor de funcionalidad por tenant, plan o porcentaje de usuarios. |
| **Fallback** | Comportamiento degradado garantizado. AR → 2D. Automático → manual. |
| **Modo kiosco** | Sesión efímera en espejo de tienda. Se borra al terminar. Nunca restaura la sesión anterior. |

## Roles

Ver matriz completa en [03-actores-roles-permisos.md](03-actores-roles-permisos.md).

| Rol | Alcance |
|---|---|
| `PLATFORM_SUPER_ADMIN` | Toda la plataforma, todos los tenants |
| `PLATFORM_SUPPORT` | Lectura global + impersonación auditada |
| `TENANT_OWNER` | Un tenant completo |
| `TENANT_ADMIN` | Un tenant, sin facturación ni borrado |
| `BRAND_MANAGER` | Una o varias marcas del tenant |
| `STORE_MANAGER` | Una o varias tiendas |
| `CATALOG_EDITOR` | Productos, variantes, activos |
| `INVENTORY_OPERATOR` | Solo stock y ubicaciones |
| `ANALYST` | Solo lectura de analítica |
| `KIOSK_DEVICE` | Identidad de dispositivo, sesión efímera |
| `END_USER` | Persona consumidora |

## Convención de identificadores de backlog

| Prefijo | Significado |
|---|---|
| `EP-nn` | Épica |
| `US-nnnn` | Historia de usuario (valor al usuario) |
| `EN-nnnn` | Habilitador técnico |
| `SP-nnnn` | Spike de investigación |
| `RF-nnn` | Requisito funcional |
| `RNF-nn` | Requisito no funcional |
| `RN-nnn` | Regla de negocio |
| `ADR-nnnn` | Decisión de arquitectura |
| `R-nn` | Riesgo |

Los 4 dígitos de `US-nnnn` codifican la épica: `US-05 02` = épica 05, ítem 02.
