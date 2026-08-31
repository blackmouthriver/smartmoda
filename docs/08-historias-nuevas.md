# 08 · Historias de usuario nuevas

52 historias que cierran las brechas identificadas en [07 § 3](07-backlog-consolidado.md) y el alcance
confirmado por el sponsor el 31-ago-2026 (espejo, menores, ropa íntima, marketplace y monetización).
Formato: historia + criterios Gherkin, incluyendo **caminos de error y casos límite** (que es lo que
faltaba en los criterios existentes).

Convención de estados en los criterios: `Dado / Cuando / Entonces / Y`.

---

# EP-17 · Super administración de plataforma

> **Objetivo:** operar N empresas sin tocar código ni base de datos.
> **KPI:** aprovisionar un tenant nuevo en menos de 15 minutos, sin intervención de ingeniería.

---

## US-1701 · Crear y aprovisionar una empresa
**Must · 8 SP · Fase 3 (adelantar el modelo a Sprint 0)** · Depende de: `US-1204`, `EN-1206`

> Como **super administrador de plataforma**, quiero crear una empresa con su plan, su dominio y su
> primer usuario propietario, para que empiece a operar sin que ingeniería toque la base de datos.

**Criterios de aceptación**

1. Dado que soy super administrador autenticado con MFA vigente, cuando abro *Nueva empresa* y diligencio razón social, NIT/identificación fiscal, país, moneda, subdominio, plan y correo del propietario, entonces el sistema crea el tenant en estado `PROVISIONING` y muestra el progreso paso a paso.
2. Dado que el aprovisionamiento termina correctamente, cuando consulto la empresa, entonces está en estado `ACTIVE`, tiene su esquema de datos aislado, sus cuotas del plan aplicadas, su tema visual por defecto y un usuario `TENANT_OWNER` invitado por correo con enlace de un solo uso válido 72 h.
3. Dado que el subdominio o la identificación fiscal ya existen, cuando intento crear la empresa, entonces el sistema rechaza la creación con mensaje específico e indica cuál campo colisiona, sin crear registros parciales.
4. Dado que un paso del aprovisionamiento falla, cuando el proceso se interrumpe, entonces el tenant queda en estado `PROVISIONING_FAILED`, no es accesible por nadie, se registra la causa y existe una acción explícita de reintento idempotente.
5. Dado que la empresa se creó, cuando consulto el log de auditoría global, entonces existe un registro con actor, motivo, marca de tiempo, IP y el estado inicial completo del tenant.

**Notas técnicas:** el aprovisionamiento es una saga con compensación. Nunca dejar un tenant a medias visible.

---

## US-1702 · Suspender, reactivar y eliminar una empresa
**Must · 8 SP · Fase 3** · Depende de: `US-1701`

> Como **super administrador**, quiero suspender, reactivar o eliminar una empresa respetando la
> retención contractual, para gestionar impagos y bajas sin destruir datos que aún debo conservar.

**Criterios de aceptación**

1. Dado un tenant `ACTIVE`, cuando lo suspendo indicando motivo, entonces sus usuarios administrativos ven una pantalla de cuenta suspendida al iniciar sesión, su catálogo deja de aparecer en la app de consumidor, y sus datos se conservan íntegros.
2. Dado un tenant `SUSPENDED`, cuando lo reactivo, entonces recupera exactamente el estado previo (catálogo publicado, usuarios, configuración, cuotas) sin acción manual adicional.
3. Dado un tenant que solicita baja, cuando ejecuto *eliminar*, entonces el sistema exige confirmación escribiendo la razón social, pasa el tenant a `PENDING_DELETION` con la fecha de purga calculada según el período de retención del contrato, y notifica al propietario.
4. Dado un tenant en `PENDING_DELETION` dentro del período de retención, cuando el propietario o el super administrador cancela la baja, entonces el tenant vuelve a `SUSPENDED` sin pérdida de datos.
5. Dado que vence el período de retención, cuando corre el trabajo de purga, entonces se eliminan datos personales y de catálogo, se conservan únicamente los registros de facturación y auditoría exigidos por ley, y se genera un certificado de borrado descargable.
6. Dado cualquiera de estas transiciones, cuando se ejecuta, entonces queda registrada en auditoría con actor, motivo y estado anterior/posterior.

---

## US-1703 · Editor de tema visual por empresa
**Must · 13 SP · Fase 3** · Depende de: `US-1701`, `EN-1903`

> Como **super administrador o dueño de empresa**, quiero definir colores, tipografía, logo, radios,
> densidad y modo oscuro de mi tienda, para que la aplicación se vea como mi marca sin pedir un
> despliegue.

**Criterios de aceptación**

1. Dado que abro el editor de tema, cuando modifico el color de acento, la tipografía, el radio de esquina, la densidad o el logo, entonces veo una **previsualización en vivo** de al menos tres pantallas reales (catálogo, detalle de producto, probador) en modo claro y oscuro.
2. Dado que elijo una combinación de color, cuando el contraste de texto sobre fondo cae por debajo de 4,5:1 (o 3:1 en elementos no textuales), entonces el editor lo señala con la razón calculada y **no permite publicar** hasta corregirlo (RN-026).
3. Dado que el tema incumple el contraste, cuando pido *corregir automáticamente*, entonces el sistema propone el valor de luminosidad más cercano que sí cumple, explicando el cambio.
4. Dado que subo un logo, cuando el archivo no es SVG/PNG, supera 512 KB o no tiene versión para fondo oscuro, entonces el sistema lo rechaza indicando el requisito incumplido.
5. Dado un tema válido, cuando publico, entonces se versiona, se aplica a web, app móvil y kiosco de ese tenant en menos de 5 minutos sin desplegar código, y queda disponible la opción de revertir a la versión anterior.
6. Dado un tema publicado, cuando navego por carrito, pago o navegación principal, entonces **estos conservan la paleta base de la plataforma** independientemente del tema del tenant (RN-027).
7. Dado que el tenant no ha configurado tema, cuando un usuario abre la aplicación, entonces se aplica el tema por defecto de la plataforma sin errores ni parpadeo.

---

## US-1704 · Editor de animaciones y micro-interacciones
**Could · 8 SP · Fase 3** · Depende de: `US-1703`

> Como **super administrador**, quiero elegir el nivel y estilo de animación de la aplicación por
> empresa, para ajustar la personalidad de marca sin comprometer rendimiento ni accesibilidad.

**Criterios de aceptación**

1. Dado que abro el editor, cuando selecciono un preajuste (`ninguna`, `sutil`, `estándar`, `expresiva`), entonces la previsualización muestra transición de pantalla, aparición de tarjeta, retroalimentación de botón y animación del splash con ese preajuste.
2. Dado un preajuste, cuando ajusto duración y curva de una transición, entonces el sistema impide valores fuera del rango permitido (duración 80–600 ms) explicando el límite.
3. Dado que el dispositivo del usuario tiene activada la reducción de movimiento del sistema, cuando abro la aplicación, entonces **se ignora el preajuste del tenant** y se usan transiciones de opacidad únicamente (RNF-06).
4. Dado un preajuste `expresiva`, cuando se mide en un dispositivo de gama baja de la matriz, entonces no se pierden fotogramas por encima del presupuesto definido; si se pierden, el sistema degrada automáticamente a `sutil` y lo registra.
5. Dado un cambio de configuración de animación, cuando se publica, entonces queda versionado y auditado igual que el tema visual.

---

## US-1705 · Catálogo de funcionalidades activables por empresa
**Must · 8 SP · Fase 3** · Depende de: `US-1701`, `EN-0007`

> Como **super administrador**, quiero activar o desactivar funcionalidades por empresa y por plan,
> para vender configuraciones distintas sin mantener ramas de código.

**Criterios de aceptación**

1. Dado el catálogo de funcionalidades, cuando lo abro para un tenant, entonces veo cada funcionalidad con su estado (`incluida en el plan`, `activada`, `desactivada`, `no disponible en este plan`), su descripción de negocio y su impacto en costo.
2. Dado que activo una funcionalidad no incluida en el plan del tenant, cuando confirmo, entonces el sistema exige una justificación y la marca como *excepción comercial* con fecha de vencimiento.
3. Dado que desactivo una funcionalidad en uso, cuando confirmo, entonces el sistema advierte qué datos y pantallas quedan inaccesibles, y los conserva (no los borra) para permitir reactivación.
4. Dado un cambio de funcionalidad, cuando se aplica, entonces surte efecto en las aplicaciones del tenant en menos de 5 minutos y no requiere que el usuario reinstale ni cierre sesión.
5. Dado que una funcionalidad está desactivada, cuando un cliente llama al endpoint correspondiente, entonces recibe `404` (no `403`), sin filtrar que la funcionalidad existe.
6. Dado que el super administrador desactiva una funcionalidad de seguridad obligatoria (auditoría, MFA administrativo, aislamiento), cuando lo intenta, entonces el sistema lo rechaza: esas funcionalidades no son configurables.

---

## US-1706 · Panel de salud, uso y consumo por empresa
**Must · 8 SP · Fase 3** · Depende de: `EN-1506`, `EN-1606`

> Como **super administrador**, quiero ver por empresa su uso, su consumo de recursos costosos y su
> salud operativa, para anticipar problemas comerciales y de margen.

**Criterios de aceptación**

1. Dado el panel, cuando lo abro, entonces veo por tenant: usuarios activos, SKU publicados, pruebas virtuales del mes, consultas al Stylist, almacenamiento consumido, tasa de error de API y porcentaje de cuota utilizado.
2. Dado un tenant que supera el 80% de cualquier cuota, cuando se calcula el panel, entonces aparece destacado y se envía una alerta al equipo comercial y al propietario del tenant.
3. Dado un tenant que supera el 100% de su cuota, cuando ocurre, entonces se aplica la política del plan (degradar o bloquear la funcionalidad afectada) con aviso claro al usuario, **sin pérdida de datos** (RN-028).
4. Dado el panel, cuando selecciono un rango de fechas, entonces los datos se recalculan y puedo exportarlos en CSV.
5. Dado que consulto costo, cuando abro el detalle de un tenant, entonces veo el costo variable estimado (IA, almacenamiento, ancho de banda) y el margen frente al precio del plan.

---

## US-1707 · Impersonación segura para soporte
**Should · 8 SP · Fase 3** · Depende de: `EN-1801`, `EN-1511`

> Como **soporte de plataforma**, quiero ver la aplicación tal como la ve un usuario administrativo de
> una empresa, para diagnosticar un problema que no puedo reproducir, sin que eso se convierta en una
> puerta trasera.

**Criterios de aceptación**

1. Dado que soy `PLATFORM_SUPPORT` o `PLATFORM_SUPER_ADMIN`, cuando inicio una impersonación, entonces el sistema exige motivo escrito, número de ticket y **re-autenticación con MFA en ese momento** (no basta la sesión abierta).
2. Dada una impersonación iniciada, cuando navego, entonces la sesión es de **solo lectura**: cualquier acción de escritura se bloquea con mensaje explícito.
3. Dado que necesito escribir durante la impersonación, cuando lo solicito, entonces se requiere aprobación de un segundo super administrador (*four-eyes*) antes de habilitarla, y esa aprobación se audita.
4. Dada una impersonación activa, cuando la vista se renderiza, entonces un banner permanente y no ocultable indica *"Sesión impersonada por &lt;actor&gt; · motivo &lt;x&gt; · expira en &lt;mm:ss&gt;"*.
5. Dado que transcurren 30 minutos, cuando llega el límite, entonces la sesión impersonada termina automáticamente y exige un nuevo motivo para reanudarse.
6. Dada una impersonación, cuando intento acceder a un perfil corporal, avatar o foto de un usuario final, entonces el dato llega cifrado y **es ilegible**, y así se indica en la interfaz (RN-020).
7. Dada una impersonación iniciada, cuando arranca, entonces se notifica por correo al `TENANT_OWNER` en menos de 5 minutos y queda registrada íntegramente en auditoría.

---

## US-1708 · Gestión de usuarios de plataforma
**Must · 5 SP · Fase 3** · Depende de: `US-1701`

> Como **super administrador**, quiero administrar los usuarios internos de la plataforma y sus roles,
> para controlar quién puede operar el backoffice.

**Criterios de aceptación**

1. Dado el módulo de usuarios de plataforma, cuando invito a un usuario con rol `PLATFORM_SUPPORT` o `PLATFORM_SUPER_ADMIN`, entonces recibe una invitación con enlace de un solo uso y debe configurar MFA antes de su primer acceso.
2. Dado un usuario de plataforma, cuando revoco su acceso, entonces todas sus sesiones activas se invalidan en menos de 60 segundos.
3. Dado que solo queda un `PLATFORM_SUPER_ADMIN`, cuando intento eliminarlo o degradarlo, entonces el sistema lo impide: siempre debe existir al menos uno.
4. Dado un usuario de plataforma sin actividad durante 90 días, cuando corre el trabajo de revisión, entonces se marca para revisión de acceso y se notifica.
5. Dada cualquier alta, baja o cambio de rol, cuando ocurre, entonces se audita con actor, objetivo, rol anterior y nuevo.

---

# EP-18 · Auditoría y trazabilidad de acciones

> **Objetivo:** poder responder "quién hizo qué, cuándo, desde dónde y sobre qué" para cualquier
> acción administrativa.
> **KPI:** 100% de acciones administrativas con registro íntegro y verificable.

---

## EN-1801 · Servicio de auditoría append-only
**Must · 8 SP · Fase 2 (Sprint 7)** · Depende de: `EN-1201`, `EN-1202`

> Como **equipo de plataforma**, necesito un servicio transversal de auditoría que registre toda acción
> administrativa de forma inmutable, para cumplir el requisito de auditoría por rol y sostener
> investigaciones de seguridad.

**Criterios de aceptación**

1. Dado cualquier comando administrativo (crear, modificar, borrar, publicar, despublicar, exportar, importar, cambiar rol, impersonar, cambiar configuración), cuando se ejecuta correctamente, entonces se escribe un registro con: `event_id`, `occurred_at` (UTC), `actor_id`, `actor_role`, `tenant_id`, `scope`, `action`, `resource_type`, `resource_id`, `ip`, `user_agent`, `request_id`, `before`, `after`, `result`.
2. Dado un comando administrativo que **falla**, cuando se rechaza por autorización, entonces también se registra, con `result = DENIED` y la razón.
3. Dado que la escritura del registro de auditoría falla, cuando ocurre en una acción crítica (cambio de rol, cambio de configuración de seguridad, impersonación, borrado), entonces **la operación de negocio se revierte**: no hay acción crítica sin auditoría.
4. Dado un registro que contiene datos corporales o personales sensibles, cuando se construye, entonces esos campos se sustituyen por referencias y hashes, nunca por el contenido (RN-033).
5. Dada la tabla de auditoría, cuando cualquier rol o proceso intenta `UPDATE` o `DELETE` sobre ella, entonces la base de datos lo rechaza (permisos + trigger), y el intento se registra como incidente (RN-032).
6. Dado el volumen esperado, cuando se consulta por tenant y rango de fechas, entonces la respuesta p95 es ≤ 2 s sobre 12 meses de datos (tabla particionada por mes).

**Notas técnicas:** implementado como aspecto/interceptor sobre la capa de aplicación, no disperso en controladores. La escritura crítica es transaccional (outbox); la no crítica es asíncrona.

---

## US-1802 · Consulta y exportación de auditoría por la empresa
**Must · 5 SP · Fase 3** · Depende de: `EN-1801`

> Como **administrador de una empresa**, quiero consultar y exportar el historial de acciones de mi
> equipo, para investigar incidentes y responder auditorías de mi propia organización.

**Criterios de aceptación**

1. Dado que soy `TENANT_OWNER` o `TENANT_ADMIN`, cuando abro el log de auditoría, entonces veo únicamente eventos de **mi tenant**, con filtros por actor, rol, acción, tipo de recurso y rango de fechas.
2. Dado un evento, cuando lo abro, entonces veo el detalle legible del cambio (antes/después) en formato comprensible, no un volcado JSON crudo.
3. Dado un filtro aplicado, cuando exporto, entonces obtengo un CSV con los mismos registros visibles, y **la exportación misma queda auditada**.
4. Dado que soy un rol sin permiso de auditoría (`CATALOG_EDITOR`, `ANALYST`, etc.), cuando intento acceder al módulo, entonces recibo `404`.
5. Dado un intento de consultar eventos de otro tenant manipulando parámetros, cuando se procesa, entonces devuelve `404` y el intento se registra como evento de seguridad (RN-009).

---

## US-1803 · Consulta global de auditoría
**Must · 5 SP · Fase 3** · Depende de: `EN-1801`

> Como **super administrador**, quiero consultar la auditoría de todas las empresas y de la plataforma,
> para investigar incidentes de seguridad transversales.

**Criterios de aceptación**

1. Dado que soy `PLATFORM_SUPER_ADMIN` o `PLATFORM_SUPPORT`, cuando abro la auditoría global, entonces puedo filtrar por tenant, actor, acción, IP, rango de fechas y resultado.
2. Dada una búsqueda por IP o por actor, cuando la ejecuto, entonces veo la actividad correlacionada a través de todos los tenants.
3. Dados eventos de seguridad (accesos denegados, intentos entre tenants, fallos de autenticación repetidos, impersonaciones), cuando abro la vista dedicada, entonces aparecen agrupados con su severidad.
4. Dado que consulto la auditoría global, cuando lo hago, entonces **mi propia consulta queda auditada** con el filtro aplicado.
5. Dado un patrón anómalo configurado (p. ej. > 20 accesos denegados de un actor en 10 minutos), cuando se detecta, entonces se genera una alerta con enlace al runbook correspondiente.

---

## EN-1804 · Integridad, retención y protección del log
**Must · 8 SP · Fase 3** · Depende de: `EN-1801`

> Como **responsable de seguridad**, necesito que el log de auditoría sea verificablemente íntegro y se
> conserve el tiempo debido, para que sirva como evidencia.

**Criterios de aceptación**

1. Dado un lote de registros, cuando se escribe, entonces cada registro incluye el hash del anterior (cadena por tenant), de modo que cualquier alteración o eliminación rompe la cadena.
2. Dada la cadena, cuando corre la verificación diaria de integridad, entonces se emite un informe; si la cadena está rota, se genera una alerta crítica inmediata.
3. Dado el cierre de cada día, cuando corre el trabajo de sellado, entonces el hash raíz del día se almacena en un medio independiente (bucket con retención inmutable / WORM).
4. Dada la política de retención, cuando un registro supera el período configurado (mínimo 12 meses, configurable por plan y por exigencia legal), entonces se archiva a almacenamiento frío antes de cualquier purga, nunca se borra directamente.
5. Dado un tenant eliminado, cuando se purgan sus datos, entonces sus registros de auditoría se conservan según la exigencia legal aunque el resto se elimine (US-1702 CA-5).
6. Dado el acceso al almacenamiento de auditoría, cuando se revisan permisos, entonces ninguna identidad de aplicación tiene permiso de borrado sobre él.

---

# EP-19 · Plataforma web y paridad de canales

> **Objetivo:** que web y móvil sean el mismo producto con el mismo sistema de diseño.
> **KPI:** cero divergencias de token entre plataformas; paridad funcional del núcleo administrativo.

---

## EN-1901 · Aplicación web administrativa responsiva
**Must · 13 SP · Fase 2 (Sprint 6–7)** · Depende de: `EN-0006`, `EN-1201`

> Como **equipo**, necesito una aplicación web administrativa responsiva sobre el contrato OpenAPI,
> para que empresas y plataforma operen desde el navegador.

**Criterios de aceptación**

1. Dada la aplicación web, cuando la abro en 360 px, 768 px y 1440 px de ancho, entonces la navegación, las tablas y los formularios son usables sin desbordamiento horizontal ni pérdida de acciones.
2. Dado el cliente HTTP, cuando se compila, entonces se genera desde el contrato OpenAPI; un cambio incompatible en el contrato rompe la compilación (AUT-05).
3. Dado que abro cualquier pantalla administrativa, cuando mido, entonces LCP ≤ 2,5 s e INP ≤ 200 ms en 4G simulada, verificado por Lighthouse CI como gate de PR.
4. Dada una sesión, cuando el token expira, entonces se renueva de forma transparente; si la renovación falla, se redirige al acceso conservando la ruta de retorno.
5. Dada la accesibilidad, cuando corre axe en CI sobre las rutas críticas, entonces hay 0 violaciones serias o críticas.
6. Dado el tema del tenant, cuando se carga la aplicación, entonces se aplican los tokens del tenant en la primera pintura, sin parpadeo de tema.

---

## EN-1903 · Sistema de diseño compartido web/móvil
**Must · 8 SP · Fase 2** · Depende de: `EN-1901`

> Como **equipo**, necesito una única fuente de verdad de tokens de diseño que genere artefactos para
> web y para Compose, para que las dos plataformas no diverjan.

**Criterios de aceptación**

1. Dada la definición de tokens en un único archivo fuente (JSON/W3C Design Tokens), cuando corre la generación, entonces produce variables CSS para web y un `Theme` de Compose para Android, ambos versionados.
2. Dado un token modificado, cuando se genera sin haber ejecutado la generación, entonces CI falla indicando que los artefactos están desactualizados.
3. Dado el conjunto de tokens de color, cuando corre la validación, entonces todas las parejas texto/fondo declaradas cumplen WCAG AA y el incumplimiento bloquea el merge.
4. Dado un componente del sistema, cuando existe en web, entonces existe su equivalente en Compose con el mismo nombre y la misma API conceptual, verificado por una lista de paridad en CI.
5. Dado el catálogo de componentes, cuando se publica, entonces está disponible como Storybook (web) y como catálogo navegable en la app de depuración (Android).

---

## US-1904 · Aplicación móvil de administración
**Should · 13 SP · Fase 2** · Depende de: `US-1101`, `EN-1903`

> Como **administrador de tienda**, quiero operar mi inventario desde el móvil, para actualizar stock,
> precios y publicación sin volver al computador.

**Criterios de aceptación**

1. Dado que inicio sesión con un rol administrativo en la app, cuando accedo, entonces veo el módulo de administración con las tiendas y marcas de mi ámbito, y **no** el módulo de consumidor.
2. Dado el módulo, cuando lo uso, entonces puedo: consultar y editar stock, ajustar precio, publicar/despublicar una variante, subir fotos desde la cámara y consultar la ubicación en tienda de un SKU.
3. Dado que no tengo conexión, cuando registro un ajuste de stock, entonces queda en cola con estado visible y se sincroniza al recuperar conexión, resolviendo conflictos con la última escritura del servidor y avisando al usuario.
4. Dado que mi rol es `INVENTORY_OPERATOR`, cuando abro el módulo, entonces solo veo acciones de stock y ubicación; las de precio y publicación no aparecen (no basta con deshabilitarlas).
5. Dada cualquier acción administrativa desde el móvil, cuando se ejecuta, entonces se audita igual que desde la web, incluyendo el canal de origen.
6. Dado que escaneo un código de barras o QR de un producto, cuando la cámara lo reconoce, entonces se abre directamente la ficha de esa variante.

---

## US-1905 · Paridad de sesión entre web y aplicación
**Should · 5 SP · Fase 3** · Depende de: `EN-1203`, `EN-1512`

> Como **usuario administrativo**, quiero pasar de la web a la aplicación sin volver a autenticarme
> desde cero, para no perder tiempo entre canales.

**Criterios de aceptación**

1. Dado que tengo sesión activa en la web, cuando abro la aplicación móvil y elijo *continuar sesión*, entonces se emite un código de emparejamiento de corta vida (≤ 2 min, un solo uso) que autentica el dispositivo.
2. Dado un código de emparejamiento, cuando se usa por segunda vez o después de expirar, entonces se rechaza y el intento se audita.
3. Dado que revoco una sesión desde *sesiones activas*, cuando lo hago, entonces ese dispositivo pierde el acceso en menos de 60 segundos (revocación efectiva del refresh token).
4. Dado que mi rol exige MFA, cuando emparejo un dispositivo nuevo, entonces se exige MFA en el dispositivo nuevo aunque la sesión de origen ya lo hubiera superado.

---

## ~~US-0618 · Probador en tiempo real por cámara web~~ — **RETIRADA**

> **Retirada el 31-ago-2026.** El alcance confirmado por el usuario establece que el avatar y las
> medidas son exclusivamente del canal móvil, y que la web no es probador de consumidor. El contenido
> técnico de esta historia (visión en navegador, composición en tiempo real, degradación) se reubica
> en el espejo inteligente: `US-1407`. Ver [ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md).
>
> Si más adelante aparece demanda de usuarios de iPhone, reintroducirla cuesta ~8–13 SP reutilizando
> la pila del espejo. Queda registrada como opción, no como alcance.

---

# EP-04 (ampliación) · Organización comercial e inventario

---

## US-0410 · Jerarquía organizacional de la empresa
**Must · 8 SP · Fase 2 (Sprint 7)** · Depende de: `US-1101`, `EN-1202`

> Como **administrador de empresa**, quiero modelar mi organización real —marcas, tiendas, secciones y
> ubicaciones— para que el inventario y los permisos reflejen cómo opero, sea yo una boutique o una
> cadena.

**Criterios de aceptación**

1. Dada mi empresa, cuando configuro la organización, entonces puedo crear marcas, y dentro de cada marca tiendas/sucursales, y dentro de cada tienda secciones y ubicaciones (piso, sección, percha).
2. Dado un negocio pequeño, cuando no creo marcas ni secciones, entonces el sistema funciona con una marca y una tienda implícitas y **no me obliga a diligenciar la jerarquía completa**.
3. Dada una tienda, cuando la creo, entonces registro dirección, horario, contacto, geolocalización y si permite reserva y recogida.
4. Dado un usuario con rol `STORE_MANAGER` asignado a la tienda A, cuando consulta inventario, entonces solo ve el de la tienda A y sus secciones; una petición por la tienda B devuelve `404`.
5. Dado que intento eliminar una tienda con inventario o reservas activas, cuando lo hago, entonces el sistema lo impide y explica qué debo resolver primero; ofrece *archivar* como alternativa.
6. Dada la jerarquía, cuando consulto el stock de un producto, entonces puedo agregarlo por empresa, por marca o por tienda.

---

## US-0411 · Localizador de la prenda en tienda
**Should · 5 SP · Fase 2** · Depende de: `US-0410`, `US-0404`

> Como **persona consumidora en una tienda física**, quiero saber en qué piso, sección y percha está la
> prenda, para encontrarla sin preguntar.

**Criterios de aceptación**

1. Dado un SKU con stock en una tienda, cuando consulto su disponibilidad, entonces veo su ubicación declarada (piso, sección, percha) si el comercio la registró.
2. Dado un SKU sin ubicación registrada, cuando lo consulto, entonces se muestra la tienda y la sección si existe, y se indica que la ubicación exacta no está disponible: **nunca se inventa una ubicación**.
3. Dado que estoy en el kiosco de una tienda, cuando selecciono una prenda, entonces la guía de ubicación se muestra referida a esa tienda física, con un plano o descripción textual.
4. Dado que la prenda no está en la tienda actual, cuando la consulto, entonces se sugieren las tiendas más cercanas que sí la tienen, ordenadas por distancia, con su disponibilidad por talla.
5. Dado que la ubicación cambia en el sistema del comercio, cuando se sincroniza, entonces la aplicación refleja el cambio en menos de 15 minutos.

---

## US-0412 · Importación masiva de catálogo
**Must · 13 SP · Fase 2 (Sprint 7)** · Depende de: `US-1102`, `US-1104`

> Como **administrador de tienda**, quiero cargar mi catálogo completo desde un archivo, para no
> capturar cinco mil referencias a mano.

**Criterios de aceptación**

1. Dado el módulo de importación, cuando descargo la plantilla, entonces obtengo un CSV/XLSX con las columnas requeridas, sus tipos, sus valores permitidos y una fila de ejemplo.
2. Dado un archivo cargado, cuando lo envío, entonces el sistema ejecuta primero una **validación en seco** y me muestra un informe fila por fila: registros válidos, con advertencia y con error, indicando la columna y la causa.
3. Dado un informe con errores, cuando decido continuar, entonces solo se importan las filas válidas y puedo descargar un archivo con las filas rechazadas y su causa, listo para corregir y reenviar.
4. Dado un archivo con referencias ya existentes, cuando importo, entonces puedo elegir entre *crear solo nuevas*, *actualizar existentes* o *reemplazar*, y el sistema muestra cuántos registros afectará cada opción **antes** de ejecutar.
5. Dada una importación en curso, cuando el archivo supera las 1.000 filas, entonces el proceso corre en segundo plano, muestra progreso y notifica al terminar; la sesión puede cerrarse sin interrumpirlo.
6. Dada una importación terminada, cuando reviso el resultado, entonces existe un identificador de lote que permite **revertir** la importación completa dentro de las 24 horas siguientes.
7. Dado el mismo archivo enviado dos veces por error, cuando se procesa, entonces el sistema detecta el duplicado por huella del contenido y pide confirmación explícita (RN-010).
8. Dada cualquier importación, cuando termina, entonces queda auditada con actor, lote, conteos y resultado.

---

## US-0413 · Taxonomía y atributos configurables por empresa
**Must · 8 SP · Fase 2** · Depende de: `US-1102`

> Como **administrador de empresa**, quiero definir mis propias categorías, colores, materiales,
> referencias y atributos, para que el catálogo hable el lenguaje de mi negocio.

**Criterios de aceptación**

1. Dada la configuración de catálogo, cuando la abro, entonces puedo gestionar categorías (con jerarquía de hasta 3 niveles), paleta de colores comerciales con su valor hexadecimal, materiales/telas y temporadas.
2. Dado un atributo personalizado, cuando lo creo, entonces defino su nombre, tipo (texto, número, lista, booleano), si es obligatorio y si es filtrable en el catálogo del consumidor.
3. Dado un color comercial (p. ej. "azul petróleo"), cuando lo defino, entonces lo asocio a un color canónico de la plataforma para que la búsqueda por color funcione entre marcas distintas.
4. Dado que intento eliminar una categoría o atributo en uso, cuando lo hago, entonces el sistema lo impide indicando cuántos productos lo usan y ofrece reasignar antes de eliminar.
5. Dada la taxonomía de un tenant, cuando otro tenant consulta, entonces no puede verla ni usarla (RN-009).
6. Dado que no configuro nada, cuando creo un producto, entonces uso la taxonomía base de la plataforma sin fricción.

---

# EP-13 (ampliación) · Analítica predictiva prueba → compra

---

## US-1306 · Embudo prueba → compra
**Must · 8 SP · Fase 3** · Depende de: `EN-1302`, `US-1303`

> Como **administrador de empresa**, quiero ver qué prendas se prueban y cuáles terminan en compra,
> desglosado por talla, color y tienda, para saber dónde estoy perdiendo la venta.

**Criterios de aceptación**

1. Dado el tablero, cuando lo abro, entonces veo el embudo `impresión → vista de detalle → prueba virtual → favorito/carrito → acción comercial` con conteos y tasas de paso entre etapas.
2. Dado el embudo, cuando aplico un desglose, entonces puedo segmentarlo por producto, variante, talla, color, categoría, marca, tienda y rango de fechas.
3. Dada una variante concreta, cuando la abro, entonces veo su tasa de conversión prueba → compra comparada con la mediana de su categoría, con la diferencia señalada.
4. Dados los datos mostrados, cuando corresponden a un segmento con menos de 30 usuarios distintos, entonces el sistema **oculta el detalle** y muestra solo el agregado, para no permitir reidentificación.
5. Dado el tablero, cuando exporto, entonces obtengo CSV con los datos agregados visibles y la exportación queda auditada.
6. Dado que soy `STORE_MANAGER`, cuando abro el tablero, entonces solo veo datos de mis tiendas.

---

## US-1307 · Prendas más vistas, más probadas y peor convertidas
**Must · 5 SP · Fase 3** · Depende de: `US-1306`

> Como **administrador de empresa**, quiero rankings de interés y de conversión, para decidir qué
> reponer, qué destacar y qué revisar.

**Criterios de aceptación**

1. Dado el módulo de rankings, cuando lo abro, entonces veo tres listas: *más vistas*, *más probadas* y *mayor brecha prueba-compra*, cada una con su métrica y su variación frente al período anterior.
2. Dada la lista de mayor brecha, cuando la ordeno, entonces prioriza productos con volumen suficiente de pruebas (umbral configurable) para evitar ruido de cola larga.
3. Dado un producto del ranking, cuando lo abro, entonces veo su desglose por talla y color y las señales asociadas (stock agotado en la talla más probada, precio por encima de la categoría, activo de baja calidad).
4. Dado el período, cuando lo cambio (7, 30, 90 días), entonces todos los rankings se recalculan de forma coherente.
5. Dado que un producto se despublicó en el período, cuando aparece en el ranking, entonces se marca como despublicado para no inducir a error.

---

## EN-1308 · Modelo predictivo de conversión y demanda
**Should · 13 SP · Fase 3** · Depende de: `EN-1302`, `US-1306`

> Como **equipo de datos**, necesito un modelo que estime la probabilidad de que una prueba virtual
> termine en compra y proyecte la demanda por SKU y talla, para alimentar reposición y ranking.

**Criterios de aceptación**

1. Dado el histórico de eventos, cuando se entrena el modelo, entonces se usan únicamente datos agregados o seudonimizados, y ninguna medida corporal individual identificable entra al conjunto de entrenamiento.
2. Dado el modelo entrenado, cuando se evalúa contra un conjunto de prueba temporalmente posterior (no aleatorio), entonces supera de forma medible a la línea base (tasa histórica de conversión de la categoría); el umbral concreto se fija en el primer entrenamiento y queda registrado.
3. Dado el modelo en producción, cuando emite una predicción, entonces registra versión de modelo, versión de características y marca de tiempo (RN-015).
4. Dado que el rendimiento del modelo se degrada por debajo del umbral durante dos períodos consecutivos, cuando se detecta, entonces se genera alerta y el sistema vuelve a la línea base determinista automáticamente.
5. Dado un tenant nuevo sin histórico, cuando se solicita una predicción, entonces se usa el modelo global de la categoría y se marca la predicción como *baja confianza*.
6. Dado el desempeño del modelo, cuando se evalúa, entonces se reportan métricas desagregadas por categoría y por tienda para detectar sesgos de segmento.

---

## US-1309 · Alertas de anomalía prueba-compra
**Should · 5 SP · Fase 3** · Depende de: `US-1306`, `EN-1308`

> Como **administrador de empresa**, quiero que el sistema me avise cuando una prenda se prueba mucho y
> se compra poco, para corregir la causa antes de perder la temporada.

**Criterios de aceptación**

1. Dado el motor de anomalías, cuando una variante supera un volumen mínimo de pruebas y su conversión cae significativamente por debajo de la de su categoría, entonces se genera una alerta.
2. Dada una alerta, cuando la abro, entonces incluye **causas candidatas ordenadas**: talla más probada sin stock, precio por encima de la mediana de la categoría, activo visual de baja calidad, tabla de tallas inconsistente, o devoluciones altas del mismo producto.
3. Dada una causa candidata, cuando la selecciono, entonces me lleva directamente a la pantalla donde puedo corregirla (stock, precio, activos, tabla de tallas).
4. Dada una alerta, cuando la marco como *revisada* o *descartada*, entonces no vuelve a generarse para esa variante durante el período configurado.
5. Dadas las alertas, cuando configuro notificaciones, entonces puedo elegir recibirlas por correo diaria o semanalmente, agrupadas, nunca una por evento.

---

## US-1310 · Predicción de curva de tallas faltante
**Should · 8 SP · Fase 3** · Depende de: `EN-1308`, `US-1304`

> Como **administrador de empresa**, quiero saber qué tallas debería tener y no tengo, para comprar
> mejor la próxima temporada.

**Criterios de aceptación**

1. Dado un producto o categoría, cuando abro el análisis de curva de tallas, entonces veo la distribución de **demanda estimada por talla** (derivada de pruebas, búsquedas y perfiles corporales agregados) frente a la **distribución de stock real**.
2. Dada la comparación, cuando existe una brecha, entonces se cuantifica en unidades estimadas y en venta potencial perdida, con su intervalo de confianza.
3. Dada una talla con demanda alta y stock cero de forma sostenida, cuando se detecta, entonces se marca como *quiebre recurrente* y se prioriza en el informe.
4. Dado el análisis, cuando lo genero por tienda, entonces refleja las diferencias de curva entre tiendas (una sucursal puede tener distribución distinta a otra).
5. Dados los datos de origen, cuando se calculan, entonces provienen de agregados sin medidas individuales identificables (mínimo 30 usuarios por celda).
6. Dado el resultado, cuando lo exporto, entonces obtengo un archivo apto para pasar a compras, con SKU, talla, demanda estimada y stock actual.

---

# EP-15 (ampliación) · Seguridad

---

## EN-1510 · Defensa contra fuerza bruta y credential stuffing
**Must · 8 SP · Fase 1 (Sprint 1)** · Depende de: `US-0102`, `EN-1502`

> Como **responsable de seguridad**, necesito que ningún endpoint de autenticación sea vulnerable a
> adivinación de credenciales, para proteger las cuentas sin castigar a los usuarios legítimos.

**Criterios de aceptación**

1. Dado un intento fallido de acceso, cuando se repite sobre la misma cuenta, entonces se aplica **retroceso exponencial** creciente por cuenta (p. ej. 0 s, 1 s, 2 s, 4 s, 8 s… hasta un tope), independientemente de la IP de origen.
2. Dados 10 intentos fallidos sobre una cuenta en 15 minutos, cuando se supera el umbral, entonces la cuenta queda temporalmente bloqueada 15 minutos, se notifica al titular por correo con la IP y el momento, y se ofrece recuperación segura.
3. Dados intentos fallidos distribuidos desde muchas IP contra muchas cuentas (patrón de credential stuffing), cuando se detecta, entonces se activa un desafío adicional (prueba de trabajo o CAPTCHA) **solo a partir del umbral**, nunca en el primer intento de un usuario legítimo.
4. Dado un intento de acceso, cuando la respuesta se construye, entonces el mensaje y el tiempo de respuesta son **indistinguibles** entre "usuario no existe" y "contraseña incorrecta" (mitigación de enumeración de usuarios), verificado por prueba automatizada de tiempos.
5. Dada una contraseña propuesta en registro o cambio, cuando se valida, entonces se rechaza si aparece en una lista de contraseñas filtradas conocidas (verificación con k-anonimato, sin enviar la contraseña).
6. Dados los endpoints de recuperación de contraseña, verificación de correo, reenvío de código y MFA, cuando se invocan, entonces todos tienen límite de tasa propio por cuenta, por IP y global.
7. Dado un acceso exitoso desde un dispositivo o ubicación no vistos antes, cuando ocurre, entonces se notifica al titular y se ofrece revocar la sesión desde el correo.
8. Dado cualquiera de estos eventos, cuando ocurre, entonces se registra en el log de seguridad con severidad y alimenta las alertas de US-1803.
9. Dada la suite de pruebas de seguridad, cuando corre en CI, entonces incluye un caso por cada uno de los criterios anteriores y **bloquea el merge** si alguno falla.

---

## EN-1511 · MFA para roles administrativos
**Must · 5 SP · Fase 2** · Depende de: `US-1101`

> Como **responsable de seguridad**, necesito segundo factor obligatorio en los roles con poder
> administrativo, porque una contraseña filtrada no puede bastar para acceder al catálogo o a los
> datos de un tenant.

**Criterios de aceptación**

1. Dado un usuario con rol administrativo o de plataforma, cuando accede por primera vez, entonces debe registrar un segundo factor (TOTP o llave WebAuthn) antes de poder operar.
2. Dado un usuario con MFA registrado, cuando inicia sesión, entonces se le exige el segundo factor en cada nueva sesión y tras cada cambio de rol.
3. Dada una operación de alto impacto (cambio de rol, cambio de configuración de seguridad, eliminación de tenant, impersonación, exportación masiva), cuando se ejecuta, entonces se exige **step-up**: reconfirmar el segundo factor en ese momento.
4. Dado el registro de MFA, cuando se completa, entonces se entregan códigos de recuperación de un solo uso, mostrados una única vez.
5. Dado que un usuario pierde su segundo factor, cuando solicita restablecerlo, entonces requiere verificación fuera de banda y aprobación de un `TENANT_OWNER` (o de un super administrador para roles de plataforma); nunca es autoservicio.
6. Dado el intento de deshabilitar MFA en un rol que lo exige, cuando se intenta, entonces se rechaza.

---

## EN-1512 · Gestión y revocación de sesiones
**Should · 5 SP · Fase 2** · Depende de: `EN-1203`

> Como **usuario**, quiero ver y cerrar mis sesiones activas, para recuperar el control si pierdo un
> dispositivo.

**Criterios de aceptación**

1. Dada mi cuenta, cuando abro *sesiones activas*, entonces veo cada sesión con dispositivo, sistema operativo, ubicación aproximada, último uso y si es la sesión actual.
2. Dada una sesión, cuando la revoco, entonces ese dispositivo pierde acceso en menos de 60 segundos, incluyendo su refresh token.
3. Dado que elijo *cerrar todas las demás sesiones*, cuando confirmo, entonces todas menos la actual se invalidan y se notifica por correo.
4. Dado un refresh token, cuando se usa, entonces se rota; si se detecta la reutilización de un token ya rotado, entonces se invalida toda la familia de tokens y se alerta al titular (detección de robo de token).
5. Dado un cambio de contraseña o una revocación de consentimiento, cuando ocurre, entonces todas las sesiones se invalidan salvo la actual.

---

## EN-1513 · Cifrado en reposo y bóveda de claves
**Must · 8 SP · Fase 2** · Depende de: `EN-1202`, `EN-1501`

> Como **responsable de seguridad**, necesito que los datos sensibles estén cifrados en reposo con
> claves gestionadas y rotables, para que una fuga de la base de datos no sea una fuga de datos.

**Criterios de aceptación**

1. Dado el almacenamiento, cuando se aprovisiona, entonces base de datos, backups y almacenamiento de objetos tienen cifrado en reposo habilitado con claves gestionadas.
2. Dados los campos clasificados como sensibles en el mapa de datos (EN-1501), cuando se persisten, entonces se cifran a nivel de aplicación con una clave por tenant, de modo que el cifrado de disco no sea la única capa.
3. Dado el perfil corporal, avatar y fotos del usuario final, cuando se almacenan, entonces están cifrados **de extremo a extremo** con clave derivada del usuario, y el servidor no posee material para descifrarlos (RN-020, ADR-0003).
4. Dadas las claves, cuando se gestionan, entonces residen en una bóveda (KMS/Secret Manager), nunca en el código, ni en variables de entorno en texto plano, ni en la base de datos.
5. Dada la política de rotación, cuando se cumple el período (máximo 12 meses o ante incidente), entonces las claves se rotan sin tiempo de inactividad y el material anterior queda disponible solo para descifrar.
6. Dado el acceso a la bóveda, cuando se revisa, entonces cada servicio tiene su propia identidad y solo los permisos que necesita, y todo acceso queda auditado.

---

## US-1514 · Exportación y portabilidad de datos del usuario
**Should · 5 SP · Fase 2** · Depende de: `US-0106`, `EN-0207`

> Como **persona usuaria**, quiero descargar todos mis datos, para ejercer mi derecho de portabilidad
> y para llevarme mi perfil si cambio de aplicación.

**Criterios de aceptación**

1. Dado que solicito la exportación desde privacidad, cuando confirmo mi identidad, entonces el sistema genera un paquete con: datos de cuenta, perfil corporal, preferencias, favoritos, closet, historial de pruebas y consentimientos con su versión y fecha.
2. Dado el paquete, cuando lo recibo, entonces está en formatos abiertos (JSON + imágenes en su formato original) y viene acompañado de un archivo que explica cada campo.
3. Dado que mi perfil está cifrado E2E, cuando se exporta, entonces la aplicación lo descifra localmente antes de empaquetar; el servidor nunca ve el contenido claro.
4. Dado el enlace de descarga, cuando se genera, entonces es de un solo uso, expira en 72 horas y requiere autenticación para usarse.
5. Dado que solicito una exportación, cuando ya hay una en curso o una completada en las últimas 24 horas, entonces se me informa en lugar de generar otra (protección contra abuso).
6. Dada la solicitud, cuando se procesa, entonces se completa en un plazo máximo de 15 días hábiles y queda registrada.

---

# EP-02 / EP-03 (ampliación) · Perfil corporal y avatar

---

## EN-0207 · Cifrado local del perfil corporal
**Must · 8 SP · Fase 1 (Sprint 2)** · Depende de: `EN-0206`, `EN-1501`

> Como **equipo**, necesito que el perfil corporal se almacene cifrado en el dispositivo y, si se
> sincroniza, cifrado de extremo a extremo, para que el dato más sensible del producto no sea legible
> por nadie más que su titular.

**Criterios de aceptación**

1. Dado el perfil corporal (medidas, silueta, avatar, fotos), cuando se persiste en el dispositivo, entonces se cifra con una clave protegida por el almacén de claves del sistema operativo (Android Keystore), respaldada por hardware cuando el dispositivo lo permite.
2. Dado un dispositivo con bloqueo de pantalla activo, cuando la aplicación está en segundo plano, entonces la clave no está disponible en memoria y el dato no puede leerse sin desbloquear.
3. Dado que el usuario activa la sincronización, cuando el perfil se envía al servidor, entonces viaja y se almacena como blob cifrado con clave derivada del usuario; **el servidor almacena, no descifra**.
4. Dado un intento de leer el perfil desde el backend con credenciales de administrador o de plataforma, cuando se ejecuta, entonces se obtiene el blob cifrado y es ilegible (prueba negativa automatizada, RN-020).
5. Dado un dispositivo rooteado o comprometido detectado por atestación, cuando se detecta, entonces la aplicación advierte y ofrece no persistir datos corporales en ese dispositivo.
6. Dado que el usuario desinstala la aplicación, cuando ocurre, entonces el material de clave local se destruye con ella.

---

## US-0208 · Recuperar el perfil en un dispositivo nuevo
**Should · 8 SP · Fase 2** · Depende de: `EN-0207`

> Como **persona usuaria**, quiero recuperar mis medidas y mi avatar en un teléfono nuevo, para no
> volver a medirme cada vez que cambio de dispositivo.

**Criterios de aceptación**

1. Dado que activo la sincronización cifrada, cuando lo hago, entonces se me genera una **frase de recuperación** (12 palabras), se me explica que sin ella el dato es irrecuperable, y debo confirmar que la guardé.
2. Dado un dispositivo nuevo, cuando inicio sesión e introduzco la frase de recuperación, entonces mi perfil corporal, preferencias y avatar se restauran completos.
3. Dada una frase incorrecta, cuando la introduzco, entonces se rechaza sin revelar información parcial, y se aplica límite de intentos con retroceso.
4. Dado que pierdo la frase de recuperación, cuando lo indico, entonces el sistema explica claramente que el perfil cifrado no puede recuperarse y ofrece empezar de nuevo, **sin ofrecer ninguna vía que implique que el servidor pueda descifrar**.
5. Dado que no quiero sincronizar, cuando lo elijo, entonces todo funciona solo en local y se me advierte que perderé el perfil si pierdo el dispositivo.
6. Dado que emparejo un dispositivo adicional, cuando lo hago, entonces recibo notificación en los dispositivos ya registrados.

---

## US-0307 · Rostro del avatar y control de visibilidad
**Should · 8 SP · Fase 2** · Depende de: `US-0301`, `US-0105`

> Como **persona usuaria**, quiero que mi avatar se parezca a mí, incluyendo una aproximación de mi
> rostro, y decidir cuándo se muestra, para reconocerme sin exponer mi cara.

**Criterios de aceptación**

1. Dado el avatar creado, cuando abro la personalización de rostro, entonces puedo elegir entre: rostro genérico, rostro estilizado a partir de una foto, o sin rostro.
2. Dado que elijo generar el rostro desde una foto, cuando lo solicito, entonces se requiere un **consentimiento adicional y específico** para tratamiento de imagen facial, separado del consentimiento corporal, explicando finalidad, tratamiento local y retención (RN-006).
3. Dado el procesamiento del rostro, cuando ocurre, entonces se realiza en el dispositivo y produce una **aproximación estilizada**, no una plantilla biométrica de identificación; la foto de origen se descarta al terminar salvo consentimiento explícito de conservación.
4. Dado el control de visibilidad, cuando lo configuro, entonces puedo definir por separado si el rostro se muestra en la aplicación, al compartir una prueba y en el kiosco de tienda; el valor por defecto al compartir es **oculto**.
5. Dado que comparto una prueba con el rostro oculto, cuando se genera la imagen, entonces el rostro se sustituye por la versión sin rostro en el archivo exportado, no se difumina sobre el original.
6. Dado que revoco el consentimiento facial, cuando lo hago, entonces el rostro se elimina del avatar y de todos los activos derivados en menos de 72 horas (RN-021).
7. Dado el modo kiosco, cuando se usa, entonces nunca se genera ni se muestra un rostro, aunque el usuario lo tenga configurado en su cuenta.

---

# Varios

---

## EN-1608 · Splash animado por empresa
**Should · 3 SP · Fase 1 (Sprint 5)** · Depende de: `US-0101`

> Como **equipo**, necesito una pantalla de arranque animada, personalizable por tenant y accesible,
> para dar identidad al inicio sin retrasar el uso.

**Criterios de aceptación**

1. Dado que abro la aplicación, cuando arranca, entonces se muestra la animación de marca (persona que se transforma en avatar) sobre la identidad del tenant activo, usando la API de splash screen del sistema operativo (sin actividad intermedia propia).
2. Dada la animación, cuando el contenido de la aplicación está listo antes de que termine, entonces la animación se corta con una transición suave: **el splash nunca añade espera artificial**.
3. Dado un arranque en frío, cuando se mide, entonces el splash no aporta más de 300 ms al tiempo total hasta el primer contenido útil.
4. Dado un dispositivo con reducción de movimiento activada, cuando arranca, entonces se muestra el logo estático sin animación.
5. Dado un tenant con logo y colores propios, cuando un usuario de ese tenant abre la aplicación, entonces el splash usa su identidad; sin tenant activo, usa la identidad de la plataforma.

---

## US-0107 · Selección de empresa y tienda activa
**Must · 3 SP · Fase 2** · Depende de: `US-1101`, `US-0410`

> Como **usuario administrativo con acceso a varias empresas o tiendas**, quiero elegir el contexto en
> el que trabajo, para no equivocarme de inventario.

**Criterios de aceptación**

1. Dado que tengo acceso a más de un tenant o más de una tienda, cuando inicio sesión, entonces debo elegir el contexto activo antes de operar.
2. Dado que tengo acceso a uno solo, cuando inicio sesión, entonces entro directo sin preguntar.
3. Dado un contexto activo, cuando navego, entonces está siempre visible en la interfaz (nombre de empresa y tienda) y puedo cambiarlo desde cualquier pantalla.
4. Dado un cambio de contexto, cuando ocurre, entonces se descarta la caché del contexto anterior y no se muestran datos mezclados en ninguna pantalla.
5. Dado el contexto activo, cuando se envía una petición, entonces el servidor valida que ese contexto pertenece a mis asignaciones de rol; si no, devuelve `404` y audita el intento (RN-009).

---

## US-1006 · Cupones y promociones
**Could · 5 SP · Fase 3** · Depende de: `US-0901`, `US-1204`

> Como **persona usuaria**, quiero recibir y aplicar cupones, para aprovechar promociones de las
> tiendas que sigo.

**Criterios de aceptación**

1. Dado un cupón vigente para mi perfil, cuando abro la sección de cupones, entonces lo veo con su descuento, condiciones, tiendas aplicables y fecha de vencimiento.
2. Dado un cupón aplicado al carrito, cuando no cumple las condiciones (monto mínimo, tienda, categoría, primera compra), entonces se rechaza indicando exactamente qué condición falta.
3. Dado un cupón de un solo uso, cuando intento aplicarlo por segunda vez, entonces se rechaza (RN-010).
4. Dado un cupón vencido o revocado por el comercio, cuando lo abro, entonces aparece marcado como no disponible y no puede aplicarse.
5. Dado que un comercio crea un cupón, cuando lo configura, entonces define alcance, vigencia, límite de usos totales y por usuario, y la creación queda auditada.
6. Dados dos cupones aplicables, cuando ambos están en el carrito, entonces se aplica la política de acumulación configurada por el tenant y se explica al usuario cuál se aplicó y por qué.

---

## US-1007 · Suscripción premium del consumidor
**Could · 5 SP · Fase 3** · Depende de: `US-0905`, `US-1603`

> Como **persona usuaria**, quiero una suscripción opcional con beneficios, para acceder a funciones
> avanzadas si me resultan útiles.

**Criterios de aceptación**

1. Dada la pantalla de suscripción, cuando la abro, entonces veo con claridad qué incluye cada nivel, su precio, su periodicidad y qué sigue siendo gratuito.
2. Dado el núcleo del producto (medidas, talla, probador 2D, catálogo, favoritos), cuando no tengo suscripción, entonces **sigue siendo gratuito y completo**.
3. Dado que me suscribo, cuando el pago se confirma, entonces los beneficios se activan de inmediato y recibo comprobante.
4. Dado que cancelo, cuando lo hago, entonces conservo los beneficios hasta el final del período pagado y se me informa la fecha exacta; no hay reactivación automática silenciosa.
5. Dado que la suscripción vence o falla el cobro, cuando ocurre, entonces se degradan solo las funciones premium; **ningún dato del usuario se pierde ni se bloquea**.
6. Dada la gestión de la suscripción, cuando la abro, entonces puedo ver el historial de cobros, cambiar de nivel y cancelar sin contactar a soporte.

---

# EP-14 (ampliación) · Espejo inteligente anónimo

> **Origen:** alcance aclarado por el usuario el 31-ago-2026. El espejo mide automáticamente, muestra
> la prenda en tiempo real, **no guarda perfil de usuario**, y sus datos anónimos alimentan la
> analítica y la predicción de la tienda. Ver [ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md).
>
> Estas historias conviven con las existentes `US-1401` (sesión invitada), `US-1402` (QR),
> `US-1403` (continuar en móvil) y `EN-1404` (borrado verificable). `US-1402` y `US-1403` pasan a ser
> **opcionales del usuario**: solo si quiere llevarse su look al teléfono.

---

## SP-1405 · Spike: precisión de la medición automática calibrada
**Must · 13 SP · Fase 3 (adelantar a Fase 2 si hay comercio interesado)** · Depende de: `SP-0304`

> Como **equipo**, necesito saber con qué error mide una instalación fija calibrada, para decidir si
> el espejo puede recomendar talla o solo mostrar la prenda.

**Criterios de aceptación**

1. Dado un montaje de referencia (cámara fija, distancia y altura conocidas, iluminación controlada, patrón de calibración), cuando se mide a un panel de al menos 30 personas con diversidad de contextura, altura, tono de piel y vestimenta, entonces se registra el error de cada medida clave contra medición manual con cinta.
2. Dado el conjunto de resultados, cuando se analiza, entonces se reporta error medio, error máximo, desviación y **métricas desagregadas por segmento** para detectar sesgo (`R-13`).
3. Dado el error medido, cuando se compara con el criterio de aprobación, entonces el spike **aprueba** si al menos el 80% de las mediciones caen en ±2,0 cm en pecho, cintura y cadera, ninguna supera ±4,0 cm, y no hay degradación relativa por segmento; en caso contrario **el espejo no recomienda talla** y queda como probador visual.
4. Dado el resultado, cuando se documenta, entonces incluye el impacto de la ropa que lleva puesta la persona (holgada frente a ajustada), que es la principal fuente de error en este escenario.
5. Dado el montaje, cuando se prueba, entonces se documenta la matriz de hardware con la que el pipeline en tiempo real sostiene el objetivo de FPS, y con cuál no.
6. Dado que la precisión depende sobre todo de la cámara, cuando se diseña el banco de pruebas, entonces se comparan al menos **cámara RGB simple frente a cámara con profundidad (RGB-D)** en el mismo montaje, para cuantificar la diferencia antes de decidir la compra ([13 § 7](13-stack-tecnologico.md)).
7. Dado el banco de pruebas, cuando se monta, entonces es un prototipo desechable (trípode, portátil con GPU, lámpara, tela de fondo): **el número decide la compra, no al revés**.

**Gate:** si el error supera el umbral, `US-1406` se reduce a estimación de banda de talla sin recomendación explícita, y el espejo sigue siendo válido como probador visual (RN-001, `R-02`).

---

## US-1406 · Medición automática anónima en el espejo
**Must · 13 SP · Fase 3** · Depende de: `SP-1405`, `EN-1411`

> Como **cliente en una tienda física**, quiero que la pantalla tome mis medidas automáticamente al
> ponerme frente a ella, para ver qué talla me sirve sin registrarme ni desvestirme.

**Criterios de aceptación**

1. Dado que me acerco al espejo, cuando la cámara está inactiva, entonces **no hay captura de ningún tipo** hasta que toco la pantalla y acepto el aviso (`EN-1410`).
2. Dado que acepto, cuando me sitúo en la marca del suelo, entonces la pantalla me guía visualmente (posición, distancia, postura) y detecta cuándo la pose es válida.
3. Dada una pose válida, cuando se procesa, entonces la estimación de medidas ocurre **en el dispositivo del espejo**; ningún fotograma sale de él, verificable con capturador de tráfico (RN-014).
4. Dadas las medidas estimadas, cuando se evalúa el error frente a la calibración, entonces la confianza se asigna así: **`HIGH`** si el error es ≤ ±1,5 cm en las medidas clave; **`MEDIUM`** entre ±1,5 y ±3,0 cm; **`LOW`** entre ±3,0 y ±5,0 cm; **`NONE`** por encima de ±5,0 cm o con pose inválida (RN-016).
5. Dada la confianza asignada, cuando se presenta el resultado, entonces: con `HIGH` se recomienda una talla con explicación; con `MEDIUM` se recomienda **e indica la talla contigua** como alternativa; con `LOW` se muestra el **rango probable sin recomendar una**; con `NONE` el espejo **no recomienda nada** y ofrece el probador visual e ir a un asesor (RN-001). Nunca inventa una talla.
6. Dado que llevo ropa muy holgada o abrigo, cuando se detecta que degrada la estimación, entonces se me indica de forma amable que el resultado puede ser menos preciso.
7. Dado que en algún momento no quiero usar la cámara, cuando lo indico, entonces puedo seguir explorando el catálogo y ver tallas por tabla, sin medición.
8. Dado que termina la sesión, cuando ocurre, entonces medidas, silueta, pose y fotogramas se destruyen; **nada persiste** (RN-013).

---

## US-1407 · Probador en tiempo real con selección táctil
**Must · 13 SP · Fase 3** · Depende de: `US-1406`, `EN-1411`

> Como **cliente en una tienda física**, quiero elegir prendas tocando la pantalla y verlas sobre mi
> cuerpo en tiempo real, como si fuera un espejo, para decidir sin hacer fila en el vestier.

**Criterios de aceptación**

1. Dada una sesión activa, cuando el catálogo se muestra, entonces contiene **únicamente el inventario disponible en esa tienda en tiempo real** (RN-029), con filtros táctiles por categoría, color y talla.
2. Dado que toco una prenda, cuando se selecciona, entonces se superpone sobre mi imagen siguiendo mi pose, con la etiqueta de confianza y el aviso de no-garantía visibles (RN-004).
3. Dado que cambio de prenda, cuando toco otra, entonces la sustitución ocurre sin reiniciar la sesión ni volver a medir.
4. Dada una prenda con stock en la tienda, cuando la veo puesta, entonces se muestra **dónde encontrarla**: piso, sección y percha (`US-0411`).
5. Dada una prenda sin stock en mi talla en esa tienda, cuando la selecciono, entonces se indica claramente y se ofrecen las tiendas cercanas que sí la tienen.
6. Dado el rendimiento, cuando se mide en el hardware soportado, entonces la sesión sostiene el objetivo de FPS definido en `SP-1405`; por debajo, se reduce la resolución de procesamiento antes de degradar.
7. Dado un hardware que no sostiene el tiempo real, cuando se detecta, entonces el espejo degrada a **foto fija con overlay**, explicándolo, sin error (RN-019).
8. Dado que la sesión está activa, cuando miro la pantalla, entonces hay un aviso permanente de cámara activa y un botón visible de **terminar sesión**.
9. Dada la inactividad, cuando supera el tiempo configurado — **corto por defecto, porque el espejo está junto al vestier y habrá cola** —, entonces la sesión termina sola y la pantalla vuelve al estado de reposo.
10. Dado el espejo, cuando se usa, entonces **nunca genera ni muestra un rostro**, aunque el usuario tenga rostro configurado en su cuenta móvil (`US-0307` CA-7).

---

## EN-1408 · Agregación en el borde y no persistencia de medidas
**Must · 8 SP · Fase 3** · Depende de: `US-1406`, `EN-1302`

> Como **responsable de privacidad**, necesito que el espejo alimente la analítica sin que jamás salga
> ni se almacene una medida individual, para que el canal sea anónimo por construcción y no por
> política.

**Criterios de aceptación**

1. Dadas las medidas estimadas en una sesión, cuando se preparan para analítica, entonces se convierten **en el dispositivo del espejo** a bandas de talla por categoría (por ejemplo, talla M en camisas); las medidas numéricas nunca se transmiten.
2. Dado el evento que sale del espejo, cuando se inspecciona, entonces contiene únicamente: `store_id`, `session_hash` aleatorio, banda de talla, categoría, variantes probadas, duración y resultado. Ningún identificador de persona, ningún dato biométrico.
3. Dado el `session_hash`, cuando se genera, entonces es aleatorio por sesión y **no deriva de nada estable**: dos sesiones de la misma persona no son correlacionables (prueba automatizada).
4. Dado el conjunto de eventos, cuando se consulta la analítica, entonces ningún segmento con menos de 30 sesiones muestra detalle; solo agregado.
5. Dado un intento de reconstruir medidas individuales desde los datos almacenados, cuando se audita, entonces se verifica que es imposible: la información no existe en el sistema.
6. Dado el diseño, cuando se revisa, entonces el sistema **no** cuenta visitantes únicos, no detecta clientes recurrentes y no vincula sesiones entre sí. Es una restricción deliberada, verificada en la revisión de arquitectura.

---

## US-1409 · Contraste de lo probado frente a lo comprado en tienda
**Must · 8 SP · Fase 3** · Depende de: `EN-1408`, `US-1306`

> Como **administrador de tienda**, quiero saber qué se probaron los clientes en el espejo y qué
> terminaron comprando, para descubrir la venta que estoy perdiendo y en qué talla.

**Criterios de aceptación**

1. Dado el tablero de la tienda, cuando lo abro, entonces veo qué variantes y qué bandas de talla se probaron en el espejo, y cuáles se vendieron en el mismo período.
2. Dada una variante muy probada y poco vendida, cuando se calcula, entonces aparece destacada con su brecha cuantificada frente a la mediana de su categoría.
3. Dada una banda de talla probada con frecuencia y **sin stock** en esa tienda, cuando se detecta, entonces se reporta como **venta perdida por curva de tallas**, con la demanda estimada (`US-1310`).
4. Dada la sesión terminada, cuando el cliente quiere llevarse su selección a la caja, entonces el espejo emite un **código corto de un solo uso** (4 letras + 4 dígitos, alfabeto sin caracteres ambiguos) mostrado en pantalla grande **y** como QR, con vigencia de 2 horas.
5. Dado ese código, cuando llega a la caja, entonces puede capturarse **escaneándolo o tecleándolo manualmente**: ambas vías producen atribución **exacta** y así se indica en el tablero.
6. Dado un código ya canjeado o vencido, cuando se intenta usar de nuevo, entonces se rechaza (RN-010) y el intento no altera ninguna atribución previa.
7. Dado el código, cuando se genera, entonces es aleatorio, de un solo uso y **no deriva del `session_hash` ni de nada de la persona**: vincula una sesión con una venta, nunca una persona con un historial.
8. Dada una venta sin código capturado, cuando se calcula la atribución, entonces se usa correlación agregada por SKU, talla, tienda y ventana temporal, y el tablero la muestra **etiquetada como estimada**, nunca como exacta.
9. Dados los datos presentados, cuando se generan, entonces provienen solo de agregados anónimos; ningún dato del tablero permite identificar a una persona (`EN-1408`).
10. Dado el resultado, cuando lo exporto, entonces obtengo un archivo apto para compras, y la exportación queda auditada.

---

## EN-1410 · Consentimiento y señalización en espacio físico
**Must · 5 SP · Fase 3** · Depende de: `EN-1501`

> Como **responsable legal**, necesito que el tratamiento de imagen en un espacio público cumpla la
> exigencia de autorización previa, expresa, informada y cualificada, porque una casilla en una
> pantalla no basta en un entorno físico.

**Criterios de aceptación**

1. Dada la instalación, cuando se despliega, entonces existe señalización visible **en el acceso a la zona de vestier**, antes del campo de la cámara, indicando qué hace el sistema, qué procesa, que nada se guarda y quién es el responsable del tratamiento.
2. Dada la pantalla en reposo, cuando nadie interactúa, entonces **la cámara está apagada**: no hay captura pasiva de quien pasa por delante, verificado por prueba y por indicador físico del hardware.
3. Dado que alguien toca la pantalla, cuando se inicia, entonces se muestra un aviso corto y comprensible y se exige aceptación explícita antes de activar la cámara.
4. Dado el aviso, cuando lo leo, entonces existe una alternativa clara e igual de accesible para explorar el catálogo y consultar tallas **sin cámara**.
5. Dada la cámara activa, cuando la sesión transcurre, entonces hay un indicador permanente en pantalla y un botón de terminar siempre visible.
6. Dada la primera instalación, cuando se planifica, entonces **existe revisión jurídica documentada y aprobada**; sin ella el despliegue está bloqueado.
7. Dado el texto del aviso, cuando cambia, entonces se versiona igual que los demás consentimientos y la versión mostrada queda registrada en el evento de sesión.
8. Dado el encuadre de la cámara, cuando se instala junto al vestier, entonces **no puede incluir puertas, cortinas ni el interior de ningún probador**; se verifica en la puesta en marcha y se documenta con una foto del encuadre aprobada por el responsable de la tienda.

---

## EN-1411 · Calibración de la instalación y detección de descalibración
**Must · 8 SP · Fase 3** · Depende de: `SP-1405`

> Como **equipo de operación**, necesito que cada espejo se calibre en la instalación y avise cuando
> se descalibre, porque toda la precisión depende de conocer la geometría del espacio.

**Criterios de aceptación**

1. Dada una instalación nueva, cuando se pone en marcha, entonces un asistente guía la calibración con un patrón físico de referencia y registra altura, ángulo y distancia de la cámara.
2. Dada una calibración, cuando se completa, entonces se valida midiendo el patrón y comparando contra su dimensión conocida; si el error supera el umbral, no se acepta y se pide repetir.
3. Dada una instalación calibrada, cuando corre la verificación periódica, entonces detecta desviaciones respecto a la referencia (por ejemplo si alguien movió la cámara) y genera alerta.
4. Dada una descalibración detectada, cuando ocurre, entonces el espejo **deja de recomendar talla** y sigue funcionando como probador visual, avisando al operador de la tienda.
5. Dado el estado de cada espejo, cuando el administrador consulta el panel, entonces ve su estado de calibración, última verificación y versión de software.
6. Dada la configuración de una instalación, cuando se guarda, entonces queda asociada al `store_id` y al dispositivo, y todo cambio se audita.
7. Dado el sitio físico, cuando se ejecuta la puesta en marcha, entonces el asistente **verifica la distancia mínima entre cámara y marca del suelo** y rechaza la instalación si es insuficiente, explicando el valor requerido ([13 § 7](13-stack-tecnologico.md)).
8. Dado el encuadre resultante, cuando se valida, entonces se confirma que cubre el cuerpo completo con margen y que **no incluye zonas de probador** (`EN-1410` CA-8).

---

# EP-20 · Publicidad y anunciantes

> **Origen:** modelo de monetización definido el 31-ago-2026 — banners fijos, ingresos por anunciantes
> y licencia por negocio o sucursal. Decisión y límites en
> [ADR-0014](adr/ADR-0014-monetizacion-publicidad.md).
>
> **Restricción que gobierna toda la épica:** la segmentación es **contextual**. Ninguna decisión
> publicitaria puede usar medidas, talla, silueta, avatar ni imágenes (RN-038).

---

## EN-2003 · Segmentación contextual sin datos corporales
**Must · 8 SP · Fase 3 (antes que cualquier anuncio)** · Depende de: `EN-1302`

> Como **responsable de privacidad**, necesito que el motor publicitario solo pueda leer señales de
> contexto, para que el modelo de ingresos no obligue a desmontar la arquitectura de privacidad.

**Criterios de aceptación**

1. Dado el servicio de decisión publicitaria, cuando solicita señales, entonces solo tiene acceso a: categoría en pantalla, marca, tienda, ciudad, temporada, idioma, moneda y preferencias de estilo declaradas con consentimiento vigente.
2. Dado un intento de leer medidas, talla recomendada, silueta, avatar o imágenes desde el motor publicitario, cuando se ejecuta, entonces **falla en compilación**: esas fuentes no están en el contrato del servicio (verificado por ArchUnit).
3. Dado un usuario que rechazó la personalización, cuando navega, entonces sigue viendo publicidad, pero únicamente contextual por categoría y tienda; el núcleo del producto no se degrada (RN-007).
4. Dado un anuncio servido, cuando se registra, entonces el evento guarda qué señales se usaron, de modo que la segmentación sea auditable.
5. Dada la suite de pruebas, cuando corre en CI, entonces incluye una prueba negativa por cada fuente prohibida de RN-038 y **bloquea el merge** si alguna es accesible.

---

## US-2001 · Espacios publicitarios y banners
**Should · 8 SP · Fase 3** · Depende de: `EN-2003`

> Como **plataforma**, quiero mostrar banners y productos patrocinados en zonas designadas, para
> generar ingresos sin degradar la experiencia de compra.

**Criterios de aceptación**

1. Dado el catálogo, cuando se renderiza, entonces los espacios publicitarios aparecen solo en las zonas designadas y **como máximo uno visible por pantalla**.
2. Dado el probador —móvil o espejo—, cuando está activo, entonces **no se muestra ningún anuncio**, en ninguna forma.
3. Dado un producto patrocinado, cuando aparece en un listado, entonces lleva la etiqueta visible de patrocinio (`SponsoredLabel`), no ocultable ni traducible a un texto ambiguo (RN-012).
4. Dado un producto patrocinado sin stock vigente, cuando se evalúa, entonces no se muestra (RN-011).
5. Dado un anuncio que contradice una restricción declarada por el usuario, cuando se evalúa, entonces se descarta antes de mostrarse.
6. Dado un banner, cuando se muestra, entonces nunca se superpone a la foto de un producto ni al canvas del probador.
7. Dado el espejo en tienda, cuando está en reposo entre sesiones, entonces puede mostrar publicidad; **al iniciar una sesión, desaparece**.

---

## US-2002 · Gestión de anunciantes y campañas
**Should · 8 SP · Fase 3** · Depende de: `US-2001`, `EN-1801`

> Como **administrador de plataforma**, quiero crear anunciantes y campañas con su presupuesto,
> vigencia y segmentación, para vender espacio publicitario sin intervención de ingeniería.

**Criterios de aceptación**

1. Dado el backoffice, cuando creo un anunciante, entonces registro razón social, contacto, datos de facturación y estado.
2. Dada una campaña, cuando la configuro, entonces defino creatividades, espacios, vigencia, presupuesto, límite de impresiones y **segmentación contextual** (categoría, marca, ciudad, tienda).
3. Dado que intento segmentar por talla, medida o tipo de cuerpo, cuando lo busco, entonces **esa opción no existe** en la interfaz ni en la API (RN-038).
4. Dada una creatividad cargada, cuando se valida, entonces se verifica formato, peso, dimensiones y contraste mínimo; si no cumple, se rechaza indicando la causa.
5. Dada una campaña que agota presupuesto o vigencia, cuando ocurre, entonces deja de servirse automáticamente y se notifica al anunciante.
6. Dada cualquier operación sobre anunciantes o campañas, cuando se ejecuta, entonces queda auditada (`EN-1801`).
7. Dada una campaña de un tenant, cuando se sirve, entonces respeta el aislamiento: un anunciante no puede dirigirse al catálogo de otro tenant sin acuerdo explícito registrado.

---

## US-2004 · Métricas de campaña para el anunciante
**Should · 5 SP · Fase 3** · Depende de: `US-2002`, `EN-1302`

> Como **anunciante**, quiero ver el desempeño de mi campaña, para decidir si renuevo.

**Criterios de aceptación**

1. Dado mi panel, cuando lo abro, entonces veo impresiones, clics, CTR y consumo de presupuesto de mis campañas, por día y por espacio.
2. Dadas las métricas, cuando se calculan, entonces provienen de agregados; **ningún dato permite identificar a un usuario** ni conocer su talla o medidas.
3. Dado un segmento con menos de 30 usuarios distintos, cuando se consulta, entonces se muestra solo el agregado, sin desglose.
4. Dado mi panel, cuando exporto, entonces obtengo CSV de mis propias campañas y solo de ellas; la exportación queda auditada.
5. Dado un anunciante, cuando accede, entonces no ve campañas, presupuestos ni métricas de otros anunciantes (RN-009).

---

## US-2005 · Facturación de campañas publicitarias
**Could · 5 SP · Fase 3** · Depende de: `US-2004`, `EN-0908`

> Como **plataforma**, quiero facturar el consumo publicitario, para cobrar lo que se sirvió.

**Criterios de aceptación**

1. Dado el cierre de un período, cuando se calcula, entonces se factura el consumo real de impresiones o clics según el modelo contratado, nunca el presupuesto reservado.
2. Dada una discrepancia entre lo servido y lo registrado, cuando se detecta, entonces se retiene la facturación y se genera alerta antes de emitir el cobro.
3. Dada una factura emitida, cuando se consulta, entonces incluye el desglose por campaña y período, y es descargable.
4. Dado un cobro, cuando se procesa, entonces es idempotente (RN-010).

---

# Historias sueltas derivadas de las respuestas del 31-ago-2026

---

## US-0209 · Perfiles de allegados
**Must · 5 SP · Fase 1 (Sprint 3)** · Depende de: `EN-0207`, `US-0202`
**Origen:** [ADR-0013](adr/ADR-0013-menores-y-ropa-intima.md)

> Como **persona adulta**, quiero guardar las medidas de mis hijos o de otras personas para las que
> compro, para acertar la talla sin tenerlos delante.

**Criterios de aceptación**

1. Dado mi perfil, cuando añado un allegado, entonces registro un nombre o alias, si es menor de edad, y sus medidas **introducidas a mano** o derivadas de edad y estatura.
2. Dado un allegado marcado como menor, cuando abro su ficha, entonces **ninguna función de cámara está disponible**: ni medición automática, ni probador sobre foto, ni avatar (RN-022 ampliada).
3. Dado un allegado, cuando consulto una prenda, entonces recibo la recomendación de talla para esa persona, con la misma explicación y confianza que para mí (RN-016).
4. Dados los datos de un allegado, cuando se persisten, entonces se cifran en el dispositivo igual que mi propio perfil y **no salen de él** ([ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md)).
5. Dado un allegado, cuando lo elimino, entonces sus datos se borran del dispositivo de inmediato.
6. Dada una prenda de categoría íntima, cuando el perfil activo es un allegado menor, entonces no se muestra el probador visual en ninguna forma (RN-037).
7. Dado el cambio de perfil activo, cuando ocurre, entonces la interfaz indica con claridad para quién se está comprando.

---

## US-1412 · Carrito anónimo en el espejo
**Must · 8 SP · Fase 3** · Depende de: `US-1407`, `US-1409`
**Origen:** marketplace confirmado + carrito en el espejo

> Como **cliente en una tienda física**, quiero añadir al carrito lo que me probé en el espejo y
> llevarlo a la caja o a mi teléfono, para comprarlo sin volver a buscarlo.

**Criterios de aceptación**

1. Dada una sesión activa, cuando añado una prenda al carrito, entonces se agrega con su talla y color, y veo el total actualizado.
2. Dado el carrito, cuando reviso el contenido, entonces cada línea muestra disponibilidad vigente en esa tienda; lo agotado se marca y no bloquea el resto (RN-003, RN-011).
3. Dado que quiero comprar, cuando termino, entonces elijo entre: **código para pagar en caja** (`US-1409`) o **QR para pasar el carrito a mi móvil** (`US-1402`).
4. Dado el espejo, cuando llego al pago, entonces **nunca se solicitan datos de tarjeta ni credenciales en la pantalla del espejo**: es una pantalla compartida en espacio público.
5. Dado el carrito, cuando la sesión termina o expira, entonces se borra por completo con el resto de la sesión (RN-013), sin dejar rastro para la persona siguiente.
6. Dado el carrito transferido al móvil, cuando el cliente lo abre, entonces contiene las mismas prendas y tallas, y a partir de ahí sigue el flujo de compra normal de la app.
7. Dado el evento de carrito del espejo, cuando se envía a analítica, entonces es anónimo y agregado igual que el resto de la sesión (`EN-1408`).

---

## SP-0619 · Estrategia de obtención de activos de prenda
**Must · 8 SP · Fase 2 (Sprint 8, junto a `SP-0606`)** · Depende de: `US-1103`

> Como **equipo**, necesito saber de dónde saldrán los activos visuales de cada prenda y cuánto
> cuestan, porque de ese número depende la viabilidad del probador más allá del 2D básico.

**Criterios de aceptación**

1. Dadas las cuatro vías posibles, cuando se evalúan sobre un conjunto de 10 prendas reales, entonces se documenta para cada una: costo por prenda, tiempo por prenda, calidad resultante y requisitos para el comercio.

   | Vía | Qué es | A evaluar |
   |---|---|---|
   | **A · Foto plana + deformación 2.5D** | Se genera el activo desde la foto de producto que el comercio ya tiene | Es la única con costo marginal cero. **Suficiente para un buen overlay 2D** |
   | **B · Activos abiertos o gratuitos** | Bibliotecas de modelos 3D libres | Casi con seguridad no cubre el catálogo real de una tienda concreta; sirve para prototipar, no para producción |
   | **C · Modelado pagado por prenda** | Proveedor externo modela cada referencia | Da la mejor calidad y es el mayor costo recurrente |
   | **D · Servicio propio de generación** | `asset-service` genera el activo desde fotos de la prenda | Inversión inicial alta, costo marginal bajo. Es la opción que el sponsor plantea construir |

2. Dado el resultado, cuando se compara, entonces se emite una recomendación explícita de vía por categoría de prenda, no una única para todo el catálogo.
3. Dada la vía A, cuando se prueba, entonces se mide qué categorías dan un overlay 2D aceptable solo con la foto de producto: es lo que determina el alcance real del MVP sin inversión.
4. Dada la vía D, cuando se estima, entonces se cuantifica el esfuerzo de construir el pipeline y el punto de equilibrio frente a la vía C, en número de prendas.
5. Dado el conjunto de resultados, cuando se presenta, entonces incluye una recomendación de **secuencia**: qué vía usar en Fase 2 y bajo qué condición cambiar a otra.
6. Dado el resultado, cuando llega al gate del Sprint 8, entonces se decide junto con `SP-0606` si AR continúa o si la Fase 2 se reorienta a mejorar el 2D ([ADR-0006](adr/ADR-0006-probador-progresivo.md)).

**Nota:** la vía A es la que hace que el MVP funcione sin invertir un peso en activos, y por eso es la
primera que hay que medir. Un buen overlay 2D desde la foto de producto cubre la promesa central del
producto; el 3D solo hace falta para AR.

---

## Resumen

| Épica | Historias | Puntos |
|---|---|---|
| EP-17 Super administración | 8 | 66 |
| EP-18 Auditoría | 4 | 26 |
| EP-19 Web y paridad | 4 | 39 |
| EP-04 Organización comercial | 4 | 34 |
| EP-13 Analítica predictiva | 5 | 39 |
| **EP-14 Espejo inteligente anónimo** | **7** | **68** |
| EP-15 Seguridad | 5 | 31 |
| EP-02/03 Perfil y avatar | 3 | 24 |
| **EP-20 Publicidad y anunciantes** | **5** | **34** |
| Varios (EP-01, EP-10, EP-16) | 4 | 16 |
| **Sueltas del 31-ago** (`US-0209`, `US-1412`, `SP-0619`) | **3** | **21** |
| **Total** | **52** | **398** |

> **Revisiones del 31-ago-2026.**
> 1. `US-0618` (probador web de consumidor, 13 SP) se retira; EP-19 baja de 5 a 4 historias.
> 2. Se añade **EP-14** con 7 historias del espejo anónimo (+68 SP).
> 3. Se añade **EP-20** con 5 historias de publicidad y anunciantes (+34 SP), tras definirse el modelo
>    de monetización ([ADR-0014](adr/ADR-0014-monetizacion-publicidad.md)).
> 4. Se añaden 3 historias sueltas: `US-0209` perfiles de allegados
>    ([ADR-0013](adr/ADR-0013-menores-y-ropa-intima.md)), `US-1412` carrito anónimo en el espejo y
>    `SP-0619` estrategia de activos de prenda (+21 SP).
>
> Backlog total: **111 ítems existentes + 52 nuevos = 163 ítems / 1.143 SP**.

## Reubicación propuesta en el calendario

No todo puede esperar a la Fase 3. Estos ítems deben adelantarse:

| Ítem | Sprint propuesto | Razón |
|---|---|---|
| `EN-1510` Fuerza bruta | **1** | Un endpoint de autenticación sin límite de tasa es explotable desde el primer día público |
| `EN-0207` Cifrado local del perfil | **2** | Cifrar después de haber persistido en claro obliga a migrar el dato más sensible |
| Modelo de datos con `tenant_id` | **0** | Ver ADR-0002; añadirlo después es una reescritura |
| `EN-1801` Auditoría | **7** | Debe existir *antes* del portal administrativo, no después |
| `EN-1901` Web administrativa | **6–7** | Ya estaba implícita; ahora es explícita y estimada |
| `US-0412` Importación masiva | **7** | Sin ella el portal administrativo no es usable por un almacén real |
| `EN-1608` Splash | **5** | Cierre visual del MVP beta |
| `US-1701`, `US-1705` | **17** | Se mantienen, pero con el modelo de datos ya preparado desde el Sprint 0 |
| `SP-1405` Spike de medición en espejo | **8–9 si hay comercio interesado** | Es el gate que decide si el espejo puede recomendar talla; conviene saberlo antes de comprometerlo comercialmente |

El plan recalculado está en [17-plan-de-trabajo.md](17-plan-de-trabajo.md).
