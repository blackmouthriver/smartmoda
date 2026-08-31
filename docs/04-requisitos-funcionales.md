# 04 · Requisitos funcionales

Cada RF es trazable a una o más historias. La historia contiene los criterios de aceptación
en formato Gherkin; el RF define **qué** debe existir, la historia define **cómo se acepta**.

Prioridad MoSCoW: **M**ust · **S**hould · **C**ould · **W**on't (esta versión).

---

## RF-1xx · Identidad, acceso y consentimiento

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-101 | Registro con correo y contraseña, con validación de fortaleza y verificación de correo | M | 1 | US-0102 |
| RF-102 | Acceso federado (Google; Apple si se publica en iOS) | S | 1 | US-0103 |
| RF-103 | Inicio, cierre y recuperación de sesión con enlace de un solo uso y expiración | M | 1 | US-0104 |
| RF-104 | Consentimiento **granular, versionado y revocable** por finalidad (imagen, biometría, marketing, analítica opcional) | M | 1 | US-0105 |
| RF-105 | Revocación de consentimientos y eliminación de cuenta con borrado verificable | M | 1 | US-0106 |
| RF-106 | Exportación de datos del usuario en formato legible por máquina (portabilidad) | S | 2 | US-1514 |
| RF-107 | MFA/2FA obligatorio para roles administrativos y de plataforma | M | 2 | EN-1511 |
| RF-108 | Defensa contra fuerza bruta y credential stuffing en todos los endpoints de autenticación | M | 1 | EN-1510 |
| RF-109 | Gestión de sesiones activas: listar, revocar individual y revocar todas | S | 2 | EN-1512 |
| RF-110 | Selección de tenant/tienda activa en aplicaciones administrativas multi-ámbito | M | 2 | US-0107 |
| RF-111 | Bloqueo de flujos corporales para menores de 18 años hasta análisis jurídico | M | 1 | US-0105 |

## RF-2xx · Perfil corporal y preferencias

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-201 | Captura **manual** de medidas corporales con guía visual por medida | M | 1 | US-0202 |
| RF-202 | Validación de rangos plausibles y coherencia entre medidas, con aviso no bloqueante | M | 1 | US-0203 |
| RF-203 | Conversión bidireccional cm ⇄ pulgadas y kg ⇄ lb sin pérdida | M | 1 | US-0203 |
| RF-204 | Cuestionario de estilo (estilos preferidos, ocasiones, siluetas) | M | 1 | US-0204 |
| RF-205 | Preferencias y restricciones de color, material y prendas excluidas | S | 1 | US-0205 |
| RF-206 | Preferencia de ajuste (ceñido/regular/holgado) que modula la **explicación**, no la medida | M | 1 | US-0202, RN-002 |
| RF-207 | Persistencia local cifrada del perfil y sincronización cifrada E2E opcional | M | 1 | EN-0206, EN-0207 |
| RF-208 | Recuperación del perfil en un dispositivo nuevo mediante frase de recuperación | S | 2 | US-0208 |
| RF-209 | **Estimación automática** de medidas desde foto/cámara, marcada como *beta* y siempre corregible | M | 2 | SP-0304, US-0305 |
| RF-210 | La medida corregida por el usuario prevalece sobre la estimada (RN-005) | M | 2 | US-0305 |

## RF-3xx · Avatar y gemelo digital

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-301 | Generación de avatar 3D paramétrico a partir del perfil corporal | M | 2 | US-0301 |
| RF-302 | Vista 360°, edición y regeneración del avatar | S | 2 | US-0302 |
| RF-303 | Calibración del avatar con referencia física conocida (altura, objeto patrón) | M | 2 | US-0303 |
| RF-304 | Personalización de rostro del avatar (aproximación, no reconstrucción biométrica) con consentimiento adicional y control de visibilidad | S | 2 | US-0307 |
| RF-305 | Silueta 2D como paso previo y fallback permanente del avatar 3D | M | 1 | EN-0602 |
| RF-306 | I+D de body scan avanzado con gate go/no-go basado en benchmark de precisión | C | 3 | SP-0306, EN-0616 |

## RF-4xx · Catálogo, inventario y organización comercial

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-401 | Navegación de catálogo por categoría, marca, género y colección | M | 1 | US-0401 |
| RF-402 | Búsqueda con filtros (talla, color, precio, material, disponibilidad) y ordenamiento | M | 1 | US-0402 |
| RF-403 | Detalle de producto con variantes (color/talla), material, precio y activos | M | 1 | US-0403 |
| RF-404 | Consulta de stock por tienda en tiempo casi real | M | 1 | US-0404 |
| RF-405 | Caché y paginación del catálogo con disponibilidad offline del recorrido reciente | M | 1 | EN-0405 |
| RF-406 | **Jerarquía organizacional**: empresa → marca → tienda → sección → piso/percha | M | 2 | US-0410 |
| RF-407 | **Localizador en tienda**: indicar piso, sección y percha donde está el SKU | S | 2 | US-0411 |
| RF-408 | **Importación masiva** de catálogo (CSV/Excel) con validación previa y ejecución en seco | M | 2 | US-0412 |
| RF-409 | Taxonomía configurable por tenant: categorías, colores, materiales, atributos propios | M | 2 | US-0413 |
| RF-410 | Adaptadores de integración ERP/POS con contrato estable | M | 3 | EN-0406 |
| RF-411 | Webhooks de inventario y precio con procesamiento idempotente y cola de fallos | M | 3 | EN-0407, EN-0408 |
| RF-412 | Reconciliación programada de inventario con reporte de desvíos | S | 3 | EN-0409 |
| RF-413 | Gestión de referencias, modelos y códigos internos del comercio | M | 2 | US-0413 |

## RF-5xx · Recomendación de talla

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-501 | Modelo de **tablas de talla versionadas** por marca, categoría y país | M | 1 | EN-0501 |
| RF-502 | Cálculo de talla recomendada a partir de medidas manuales | M | 1 | US-0502 |
| RF-503 | **Nivel de confianza y explicación** legible de la recomendación | M | 1 | US-0503 |
| RF-504 | Manejo explícito de "entre dos tallas" con alternativa segura | M | 1 | US-0503, RN-002 |
| RF-505 | Si no existe tabla válida, informar la ausencia sin inventar recomendación | M | 1 | RN-001 |
| RF-506 | Trazabilidad: toda recomendación guarda la versión de tabla y algoritmo usados | M | 1 | RN-015 |

## RF-6xx · Probador virtual

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-601 | Captura fotográfica guiada (encuadre, distancia, iluminación, pose) | M | 1 | US-0601 |
| RF-602 | Detección de pose y silueta **on-device** (33 landmarks) | M | 1 | EN-0602 |
| RF-603 | **Overlay 2D** de prenda sobre la foto de cuerpo completo | M | 1 | US-0603 |
| RF-604 | Ajuste manual del overlay (escala, posición, rotación) | S | 1 | US-0604 |
| RF-605 | Guardar y compartir una prueba, sin metadatos sensibles (RN-008) | S | 1 | US-0605 |
| RF-606 | Pipeline de activos 3D glTF/GLB con perfil de activo validado | M | 2 | SP-0606 |
| RF-607 | Matriz de compatibilidad ARCore y **fallback 2D garantizado** | M | 2 | EN-0607 |
| RF-608 | Pose en tiempo real para AR | M | 2 | EN-0608 |
| RF-609 | Anclaje de prenda 3D al cuerpo con escala, orientación y suavizado | M | 2 | US-0609, EN-0612 |
| RF-610 | Controles de sesión AR (pausar, capturar, cambiar prenda, salir) | S | 2 | US-0610 |
| RF-611 | Oclusión con Depth API y degradación controlada | S | 2 | EN-0613 |
| RF-612 | Comparación lado a lado: 2D, AR y foto de producto | S | 2 | US-0614 |
| ~~RF-613~~ | ~~Probador en tiempo real por cámara web para consumidor~~ **RETIRADO** — el avatar y las medidas son exclusivamente del canal móvil. Su contenido pasa a RF-1407 | — | — | — |
| RF-614 | I+D de física de tela con gate go/no-go | C | 3 | SP-0615 |
| RF-615 | Toda visualización lleva etiqueta de confianza y aviso de no-garantía (RN-004) | M | 1 | US-0603 |

## RF-7xx · Stylist IA y recomendación

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-701 | Asistente conversacional que recibe solicitudes en lenguaje natural | M | 3 | US-0701 |
| RF-702 | Contexto de ocasión, clima, presupuesto y restricciones del usuario | M | 3 | US-0702 |
| RF-703 | Generación de ≥2 outfits combinados **solo con SKU disponibles** (RN-011) | M | 3 | US-0703 |
| RF-704 | Probar un look completo en el avatar/foto en una sola acción | M | 3 | US-0703 |
| RF-705 | Feedback explícito (me gusta / no es para mí / no es mi talla) que alimenta el ranking | S | 3 | US-0706 |
| RF-706 | Motor de recomendación desacoplado detrás de un gateway proveedor-agnóstico | M | 3 | EN-0705 |
| RF-707 | Ranking híbrido: talla + estilo + stock + regla comercial, con explicabilidad | M | 3 | EN-0707 |
| RF-708 | Identificación visible de recomendación patrocinada (RN-012) | M | 3 | EN-0707 |
| RF-709 | Evaluación automática (relevancia, stock, seguridad, latencia, costo) como gate antes de promover modelo o prompt | M | 3 | EN-0708 |
| RF-710 | **Asistente de localización**: sugerir tiendas donde está la prenda o una similar | M | 3 | US-0903, US-0411 |

## RF-8xx · Favoritos, closet e historial

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-801 | Guardar y retirar favoritos (prendas, looks, marcas, tiendas) | M | 1 | US-0801 |
| RF-802 | Mi Closet: registro de prendas propias del usuario | S | 1 | US-0802 |
| RF-803 | Historial de prendas probadas con fecha, talla y resultado | M | 1 | US-0802 |
| RF-804 | Guardar, nombrar y reutilizar outfits | S | 3 | US-0704 |

## RF-9xx · Comercio omnicanal

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-901 | Carrito armado desde un look o desde el probador | M | 2 | US-0901 |
| RF-902 | Reserva de prenda en tienda física con vigencia y confirmación | S | 2 | US-0902 |
| RF-903 | Ficha de tienda con ruta, horario y disponibilidad | S | 2 | US-0903 |
| RF-904 | Deep link y handoff al checkout del retailer | M | 2 | US-0904 |
| RF-905 | Pago integrado con pasarela | S | 3 | US-0905 |
| RF-906 | Historial y estado de órdenes | S | 3 | US-0906 |
| RF-907 | Cancelación y solicitud de devolución | C | 3 | US-0907 |
| RF-908 | Conciliación idempotente de pagos y webhooks | M | 3 | EN-0908 |
| RF-909 | Revalidación de stock y precio antes de reservar, pagar o salir a canal externo (RN-003) | M | 2 | US-0901 |

## RF-10xx · Engagement

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1001 | Preferencias granulares de notificación con opt-in explícito | M | 2 | US-1002 |
| RF-1002 | Alertas de stock, reposición de talla o cambio de precio | S | 3 | US-1003 |
| RF-1003 | Resumen de estilo periódico con control de frecuencia | C | 3 | US-1004 |
| RF-1004 | Feedback contextual durante el piloto | S | 2 | US-1001 |
| RF-1005 | Cupones y suscripción premium del consumidor | C | 3 | US-1006, US-1007 |

## RF-11xx · Portal administrativo del comercio

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1101 | Acceso administrativo con roles por ámbito | M | 2 | US-1101 |
| RF-1102 | CRUD de productos y variantes | M | 2 | US-1102 |
| RF-1103 | Carga y validación de activos 2D/3D con perfil de activo | M | 2 | US-1103 |
| RF-1104 | Gestión de tallas, inventario, precios y publicación | M | 2 | US-1104 |
| RF-1105 | **App móvil de administración** con paridad funcional del núcleo operativo | S | 2 | US-1904 |
| RF-1106 | Web administrativa **responsiva** (escritorio, tableta, móvil) | M | 2 | EN-1901 |

## RF-12xx · Multi-tenancy

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1201 | Creación y configuración de tenant | M | 3\* | US-1204, US-1701 |
| RF-1202 | Roles por tenant y delegación de administración | M | 3\* | US-1205 |
| RF-1203 | **Aislamiento verificado** de datos entre tenants con prueba negativa automatizada | M | 3\* | EN-1206 |
| RF-1204 | Marca y configuración visual por tienda | S | 3 | US-1207, US-1703 |
| RF-1205 | Límites y cuotas por tenant (SKU, pruebas/mes, almacenamiento, llamadas IA) | M | 3 | US-1603 |

> **\*** El `tenant_id` y la derivación de contexto deben existir desde el **Sprint 0**.
> Ver [ADR-0002](adr/ADR-0002-estrategia-multitenant.md).

## RF-13xx · Analítica y analítica predictiva

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1301 | Taxonomía de eventos versionada con validación de esquema y cuarentena | M | 3 | EN-1302 |
| RF-1302 | Dashboard de embudo: impresión → vista → prueba → favorito → acción comercial | M | 3 | US-1303 |
| RF-1303 | **Embudo prueba → compra** por prenda, talla, color, tienda y segmento | M | 3 | US-1306 |
| RF-1304 | Ranking de **prendas más vistas, más probadas y peor convertidas** | M | 3 | US-1307 |
| RF-1305 | **Modelo predictivo** de conversión y demanda por SKU/talla | S | 3 | EN-1308 |
| RF-1306 | **Alertas de anomalía**: alta prueba con baja compra ⇒ señal de tallaje, precio o activo | S | 3 | US-1309 |
| RF-1307 | Predicción de curva de tallas faltante en inventario | S | 3 | US-1310 |
| RF-1308 | Mapa de tallas y demanda por región | S | 3 | US-1304 |
| RF-1309 | Devoluciones y exportación de datos anonimizados | S | 3 | US-1305 |
| RF-1310 | Experimentos A/B con guardrails y criterio de parada | S | 3 | EN-1301 |

## RF-14xx · Espejo inteligente

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1401 | Sesión invitada efímera en kiosco, sin persistencia entre usuarios (RN-013) | M | 3 | US-1401 |
| **RF-1407** | **Medición automática anónima** en el espejo, sin registro ni perfil | M | 3 | US-1406 |
| **RF-1408** | **Probador en tiempo real con selección táctil** sobre pantalla en tienda | M | 3 | US-1407 |
| **RF-1409** | Agregación en el borde: ninguna medida individual sale del espejo ni se persiste | M | 3 | EN-1408 |
| **RF-1410** | **Contraste de lo probado frente a lo comprado** en tienda física | M | 3 | US-1409 |
| **RF-1411** | Consentimiento y señalización en espacio físico, con alternativa sin cámara | M | 3 | EN-1410 |
| **RF-1412** | Calibración de la instalación y detección de descalibración | M | 3 | EN-1411 |
| RF-1402 | Emparejamiento seguro por QR con el móvil del usuario | M | 3 | US-1402 |
| RF-1403 | Continuidad: seguir en el móvil el look armado en el espejo | S | 3 | US-1403 |
| RF-1404 | Borrado verificable de la sesión física con evidencia auditable | M | 3 | EN-1404 |
| RF-1405 | Catálogo del kiosco restringido al inventario **de esa tienda en tiempo real** | M | 3 | US-1401 |
| RF-1406 | Guía al usuario hacia la ubicación física de la prenda dentro de la tienda | S | 3 | US-0411 |

## RF-15xx · Seguridad, privacidad y auditoría

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1501 | Mapa de datos y modelo de amenazas mantenido y revisado por fase | M | 1 | EN-1501 |
| RF-1502 | Reglas de seguridad de backend y atestación de app (App Check / Play Integrity) | M | 1 | EN-1502 |
| RF-1503 | Retención automática y borrado de imágenes de captura | M | 2 | EN-1505 |
| RF-1504 | **Log de auditoría append-only** de toda acción administrativa | M | 2 | EN-1801 |
| RF-1505 | Consulta y exportación del log de auditoría por el admin del tenant | M | 3 | US-1802 |
| RF-1506 | Consulta global del log por el super administrador | M | 3 | US-1803 |
| RF-1507 | Integridad del log verificable (encadenamiento por hash) y retención mínima 1 año | M | 3 | EN-1804 |
| RF-1508 | Cifrado en reposo de datos sensibles con bóveda de claves gestionada | M | 2 | EN-1513 |
| RF-1509 | Prueba de penetración con remediación antes de producción SaaS | M | 3 | EN-1507 |
| RF-1510 | Backup, restauración y simulacro de recuperación verificado | M | 3 | EN-1508 |
| RF-1511 | Auditoría final de privacidad y accesibilidad | M | 3 | EN-1509 |

## RF-16xx · Super administración de plataforma

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1601 | Crear y aprovisionar empresas (tenants) con plan inicial | M | 3\* | US-1701 |
| RF-1602 | Suspender, reactivar y eliminar tenant respetando retención contractual | M | 3 | US-1702 |
| RF-1603 | **Editor de tema visual** por tenant (colores, tipografía, radios, densidad, logo) con validación WCAG AA automática | M | 3 | US-1703 |
| RF-1604 | **Editor de animaciones** y micro-interacciones por tenant, respetando `prefers-reduced-motion` | C | 3 | US-1704 |
| RF-1605 | Catálogo de funcionalidades activables por tenant y por plan | M | 3 | US-1705 |
| RF-1606 | Panel de salud, uso y consumo por tenant (IA, almacenamiento, ancho de banda) | M | 3 | US-1706, EN-1606 |
| RF-1607 | Impersonación segura con doble control y auditoría | S | 3 | US-1707 |
| RF-1608 | Planes, prueba gratuita, límites y facturación | M | 3 | US-1603, US-1604 |
| RF-1609 | Onboarding autoservicio de comercios | S | 3 | US-1605 |

## RF-17xx · Plataforma web y experiencia transversal

| ID | Requisito | Prio | Fase | Historias |
|---|---|---|---|---|
| RF-1701 | **Splash animado** de arranque, personalizable por tenant, con degradación a estático | S | 1 | EN-1608 |
| RF-1702 | Diseño **responsivo** en web para escritorio, tableta y móvil | M | 2 | EN-1901 |
| RF-1703 | **Design system compartido** entre web y móvil generado desde tokens únicos | M | 2 | EN-1903 |
| RF-1704 | Paridad de sesión entre web y app (SSO propio) | S | 3 | US-1905 |
| RF-1705 | Localización: es-CO primero, textos externalizados, moneda/unidades por configuración | M | 1 | RNF-14 |
| RF-1706 | Accesibilidad AA en flujos críticos, móvil y web | M | 1 | EN-1503 |
| RF-1707 | Feature flags y despliegue progresivo con kill switch | M | 2 | EN-0007 |

---

## Trazabilidad inversa: qué pediste ↔ dónde quedó

| Petición original | Cubierto por |
|---|---|
| Medir cuerpo manual y automático | RF-201, RF-209 |
| Mostrar prendas en avatar | RF-301, RF-603 |
| Mostrar prendas en foto de cuerpo completo | RF-603 |
| Tiempo real con cámara (móvil) | RF-608, RF-609 |
| Tiempo real con cámara y pantalla en tienda | RF-1407 a RF-1412 |
| Multi-tenant | RF-12xx completo |
| Uso para empresas, almacenes y personas | RF-406, RF-1601, RF-1101 |
| Inventario | RF-404, RF-408, RF-411, RF-412 |
| Perfil de usuario / de empresa | RF-2xx / RF-1201 |
| Super administrador | RF-16xx completo |
| Administrador por empresa | RF-1101, RF-1202 |
| Fotos, stock, referencia, modelos, precio, colores, categorías | RF-403, RF-409, RF-413, RF-1103, RF-1104 |
| Secciones, tiendas o sucursales según tamaño | RF-406, RF-407 |
| Roles | Doc 03 completo, RF-1202 |
| Analítica predictiva prueba vs compra | RF-1303, RF-1305, RF-1306 |
| Prendas más vistas | RF-1304 |
| Crear empresas desde super admin | RF-1601 |
| Modificar colores, tamaños, estilos, animaciones | RF-1603, RF-1604 |
| Configuración de funciones por necesidad de la tienda | RF-1605, RF-1707 |
| Mostrar tallas, colores, referencias en avatar | RF-502, RF-403, RF-603 |
| Selección desde base de datos | RF-401, RF-402 |
| Guardado local del perfil en el móvil | RF-207, ADR-0003 (**confirmado: solo local, cifrado**) |
| Espejo que mide sin guardar perfil y alimenta la analítica | RF-1407, RF-1409, RF-1410, ADR-0011 |
| Web para super admin y admin de tiendas | RF-1106, RF-16xx |
| App alterna para admin | RF-1105 |
| Microservicios | ADR-0001 (revisado a monolito modular por fases) |
| Pruebas unitarias | Doc 15 |
| Splash animado | RF-1701 |
| Responsivo | RF-1702 |
| Auditorías por rol | RF-1504 a RF-1507 |
| Fuerza bruta | RF-108, EN-1510 |
| Sugerencia de tiendas donde está la prenda | RF-710, RF-903 |
| Tendencias de moda en las sugerencias | RF-702, RF-707 |
| Espejo digital en vestier | RF-14xx completo |
| Tipografía y paleta de moda | Doc 16 |
| MVP a costo cero | Doc 13 § 2 |
