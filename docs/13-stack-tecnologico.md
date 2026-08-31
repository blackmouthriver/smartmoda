# 13 · Stack tecnológico

> **Advertencia sobre los niveles gratuitos:** los límites de los planes gratuitos cambian con
> frecuencia. Las cifras que aparecen abajo son órdenes de magnitud para decidir, **no** cotizaciones.
> Verifica los límites vigentes antes de comprometerte, y ejecuta la tarea de verificación del Sprint 0
> ([17-plan-de-trabajo.md](17-plan-de-trabajo.md)).

---

## 1. Elecciones por capa

| Capa | Elección | Alternativa considerada | Por qué esta |
|---|---|---|---|
| **Móvil** | Kotlin + Jetpack Compose | Flutter, React Native | AR y ML on-device son ciudadanos de primera clase en Android nativo. ARCore, ML Kit y CameraX no tienen equivalente estable en multiplataforma. Ya decidido en `D-04` |
| **Web** | React 18 + TypeScript + Vite | Angular, Vue, Next.js | Ecosistema y talento disponible. Vite en lugar de Next porque el portal administrativo es una SPA autenticada: el renderizado en servidor no aporta y añade infraestructura |
| **Backend** | Kotlin + Spring Boot 3 | Node/NestJS, Go, Java | Mismo lenguaje que el móvil: el dominio (reglas de talla, políticas) se comparte como módulo Kotlin puro. Spring Boot tiene el ecosistema de seguridad y datos más maduro para un SaaS multi-tenant |
| **Base de datos** | PostgreSQL 16 | MySQL, MongoDB | **Row Level Security** es la razón principal (doc 10). Además: JSONB para atributos flexibles, PostGIS para tiendas cercanas, particionado para auditoría, `pgvector` para embeddings del Stylist. Una sola tecnología cubre cuatro necesidades |
| **Caché** | Redis | Memcached | Estructuras de datos para límites de tasa por ventana deslizante y colas ligeras |
| **Cola** | PostgreSQL (`pg-boss` o tabla outbox) en Fase 2 → broker dedicado en Fase 3 | RabbitMQ, Kafka desde el inicio | Con el volumen de las Fases 1–2, un broker es infraestructura que operar sin necesidad. La abstracción de puerto permite cambiar sin tocar el dominio |
| **Object storage** | Compatible S3 | — | Estándar de facto; permite cambiar de proveedor |
| **Warehouse** | ClickHouse o DuckDB/Parquet en Fase 3 | Solo PostgreSQL | El embudo prueba→compra sobre millones de eventos degrada el OLTP. Ver doc 14 |
| **ML on-device** | ML Kit Pose Detection (Android), MediaPipe (web) | Modelo propio | El riesgo `R-05` (ML Kit Pose en beta) se mitiga con una interfaz `PoseDetector` propia, no evitando la biblioteca |
| **3D / AR** | ARCore + Filament/SceneView, activos glTF/GLB | Unity, Sceneform | Sin motor de juego completo: el peso del APK y la complejidad no se justifican para superponer prendas |
| **IA generativa** | Gateway proveedor-agnóstico (`D-09`) | Acoplarse a un proveedor | Los precios y las capacidades cambian cada trimestre. El gateway permite cambiar sin tocar el dominio y hace posible la evaluación comparativa |
| **CI/CD** | GitHub Actions | GitLab CI, Jenkins | Gratuito para repositorios públicos, generoso en privados, y ya está en `AUT-01..16` |
| **Observabilidad** | OpenTelemetry + Crashlytics (móvil) + backend a definir en Fase 2 | Acoplarse a un vendedor | OTel es el estándar; el destino se elige después sin reinstrumentar |

---

## 2. Fase 1 con inversión cero

Objetivo: MVP beta distribuible sin pagar infraestructura.

| Necesidad | Opción gratuita | Límite típico a vigilar | Plan de salida |
|---|---|---|---|
| Autenticación | Firebase Authentication | Muy generoso; el costo aparece con SMS (no lo usamos: TOTP, no SMS) | Migrar a Spring Security + JWT propio en Fase 2 |
| Base de datos | Firebase Firestore **o** Supabase (PostgreSQL gestionado) | Firestore: lecturas/escrituras diarias. Supabase: proyecto pausado por inactividad, límite de almacenamiento | **Recomiendo Supabase**: es PostgreSQL real, así que la migración a Fase 2 es un `pg_dump`, no una reescritura |
| Almacenamiento de activos | Firebase Storage / Supabase Storage | GB almacenados y transferidos | Migrar a S3 compatible |
| Funciones de servidor | Cloud Functions / Supabase Edge Functions | Invocaciones al mes | Migrar a endpoints de Spring Boot |
| Hosting web | Páginas estáticas gratuitas (GitHub Pages, Cloudflare Pages, Netlify) | Ancho de banda | Mismo hosting con plan pago o CDN propia |
| CI/CD | GitHub Actions | Minutos/mes en repositorio privado | Runners propios o plan pago |
| Distribución beta | Firebase App Distribution / Play Console pista interna | Cuota única de registro de desarrollador Play (~25 USD, **único costo real de Fase 1**) | — |
| Pruebas en dispositivos | Firebase Test Lab (cuota gratuita diaria) | Dispositivos/día | Plan pago o granja propia |
| Crash y rendimiento | Crashlytics + Performance Monitoring | Gratuito | — |
| Analítica de producto | Google Analytics for Firebase | Gratuito, con límites de parámetros | Warehouse propio en Fase 3 |
| Modelos de IA | Niveles gratuitos de proveedores; y **en el MVP no hay IA generativa en el flujo crítico** | Peticiones/minuto | Gateway con presupuesto por tenant (EN-1606) |
| Mapas | OpenStreetMap + Leaflet | Política de uso de teselas | Proveedor pago si el volumen lo exige |

### La decisión que más ahorra

**El MVP no tiene IA generativa en el camino crítico.** La recomendación de talla es un cálculo
determinista contra tablas versionadas; el overlay 2D es composición gráfica sobre landmarks
detectados on-device. Ninguna de las dos cuesta por invocación. El Stylist IA —que sí cuesta— llega
en Fase 3, cuando ya hay un modelo de negocio que lo pague.

Esto no es una limitación del MVP: una talla explicable y reproducible es **mejor producto** que una
talla generada por un modelo que no puede justificar su respuesta.

### La segunda decisión que más ahorra

**El procesamiento de imagen ocurre en el dispositivo.** Es el control de privacidad de RN-014, y
además elimina el costo de GPU en el servidor, que es el rubro que hunde el margen de los productos
de probador virtual.

### Costo real estimado de Fase 1

| Concepto | Costo |
|---|---|
| Infraestructura | **USD 0** |
| Cuenta de desarrollador de Google Play | ~USD 25 (pago único) |
| Dominio (opcional en Fase 1) | ~USD 12/año |
| **Total** | **≈ USD 25–40, pago único** |

### Cuándo deja de ser gratis

Señales que indican que Fase 1 llegó a su techo:

- Más de ~5.000 usuarios activos al mes.
- Más de ~10 GB de activos de catálogo.
- Más de 3 tenants reales operando en paralelo.
- El primer cliente que exige SLA, exportación de datos o integración con su ERP.

Ninguna es urgente. Todas son señales de que el producto funciona y de que Fase 2 se puede financiar.

---

## 3. Costos previstos por fase

| Fase | Concepto | Rango mensual estimado |
|---|---|---|
| **1** | Nada | USD 0 |
| **2** | PostgreSQL gestionado pequeño + 1–2 contenedores + object storage + CDN | USD 50–150 |
| **2** | *Opcional*: modelador 3D para activos de prendas (riesgo `R-01`) | Variable — es el mayor costo no técnico del proyecto |
| **3** | Infraestructura escalada + warehouse + broker + WAF | USD 300–800 |
| **3** | IA generativa (Stylist) | Variable por uso; controlada por cuota (EN-1606) |
| **3** | Pentest externo (`EN-1507`) | USD 3.000–8.000, una vez |
| **3** | Pasarela de pago | % por transacción |

> **El costo que más suele subestimarse en este tipo de producto no es la nube: son los activos 3D
> por prenda.** El riesgo `R-01` está bien identificado en tu backlog. El spike `SP-0606` debe
> responder cuántas horas y cuántos dólares cuesta preparar una prenda para AR, porque de ese número
> depende si la Fase 2 es viable.

---

## 4. Dependencias principales

### Android
```
Kotlin · Coroutines · Flow
Jetpack Compose (BOM) · Navigation · Material 3
Hilt (inyección de dependencias)
Room + SQLCipher (persistencia local cifrada)
Retrofit/Ktor + kotlinx.serialization (cliente generado de OpenAPI)
CameraX · ML Kit Pose Detection
ARCore + SceneView/Filament (Fase 2)
Coil (imágenes) · DataStore (preferencias)
androidx.security.crypto (Keystore)
WorkManager (sincronización y colas offline)
Test: JUnit5 · Turbine · MockK · Compose UI Test · Robolectric · Espresso · Paparazzi
```

### Backend
```
Kotlin · Spring Boot 3 (Web, Security, Validation, Data JPA)
PostgreSQL + Flyway
springdoc-openapi (contrato)
Resilience4j (circuit breaker, retry, rate limit)
Micrometer + OpenTelemetry
Argon2 (passwords)
Test: JUnit5 · MockK · Testcontainers · RestAssured · ArchUnit · Pitest
```

### Web
```
React 18 · TypeScript · Vite
TanStack Query (estado de servidor) · Zustand (estado de UI)
React Hook Form + Zod (formularios y validación)
Cliente generado desde OpenAPI (openapi-typescript-codegen)
MediaPipe Tasks Vision (espejo en tienda, US-1406/US-1407)
Test: Vitest · Testing Library · Playwright · axe-core · Storybook
```

---

## 5. Estructura del repositorio

**Monorepo.** Con un equipo pequeño, tener el contrato de API, el móvil, la web y el backend en un
solo repositorio hace que un cambio de contrato se vea inmediatamente en sus tres consumidores.

```
smartmoda/
├── docs/                     esta documentación (fuente de verdad viva)
├── contracts/
│   ├── openapi.yaml          contrato de API — fuente de verdad
│   ├── events/               esquemas de eventos de analítica (EN-1302)
│   └── design-tokens.json    tokens de diseño — fuente de verdad (EN-1903)
├── android/
├── web/
├── backend/
├── shared-domain/            Kotlin puro: sizing, políticas, tipos comunes
├── tools/
│   ├── token-generator/      tokens → CSS + Compose Theme
│   ├── tenant-isolation-test/ generador de la suite negativa desde el OpenAPI
│   └── traceability/         verifica que cada RN-xxx tiene su prueba
└── .github/workflows/
```

`shared-domain` es la pieza que justifica Kotlin en ambos lados: la política de recomendación de talla
se escribe **una vez**, se prueba una vez, y corre igual en el dispositivo (para funcionar sin
conexión) y en el servidor (para la web y el kiosco). Sin esto habría dos implementaciones que
divergen, y una talla distinta según el canal es un defecto que el usuario nota.

---

## 6. Automatización (extiende `AUT-01..16`)

| ID | Disparador | Acción | Gate |
|---|---|---|---|
| AUT-01..16 | *(los 16 del `Backlog_Ejecutable`)* | — | — |
| **AUT-17** | PR | Suite negativa de aislamiento multi-tenant generada desde OpenAPI | **Bloquea merge** |
| **AUT-18** | PR | Verificación de trazabilidad `RN-xxx` ⇄ prueba | Bloquea merge |
| **AUT-19** | PR | Pruebas de fuerza bruta y límite de tasa (EN-1510) | Bloquea merge |
| **AUT-20** | PR web | Lighthouse CI (LCP, INP) + axe | Bloquea merge |
| **AUT-21** | PR | Verificación de tokens de diseño regenerados y contraste WCAG | Bloquea merge |
| **AUT-22** | Migración | Verifica `tenant_id NOT NULL` + política RLS en toda tabla nueva de negocio | **Bloquea deploy** |
| **AUT-23** | PR | ArchUnit: sin dependencias entre `domain` de módulos distintos | Bloquea merge |
| **AUT-24** | Diario | Verificación de integridad de la cadena de auditoría (EN-1804) | Alerta crítica |
| **AUT-25** | PR backend | Pruebas de mutación (Pitest) sobre módulos de dominio | Informativo, umbral en Fase 3 |

---

## 7. Hardware del espejo en tienda

> Sugerencias, no cotización. Los modelos concretos y los precios cambian; lo que no cambia es el
> orden de importancia de los componentes. Confirmar todo con `SP-1405` en el montaje real.

### El orden que importa

La precisión del espejo depende, por peso:

```
1. Cámara con profundidad     ██████████  determinante
2. Ropa que lleva la persona  ███████     alto (se mitiga por ubicación)
3. Iluminación y fondo        █████       alto
4. Potencia de cómputo        ███         medio (afecta a FPS, no a precisión)
5. Modelo / algoritmo         ██          menor de lo que parece
```

Gastar en GPU antes que en cámara es el error clásico: da un espejo fluido que mide mal.

### Configuración recomendada

| Componente | Recomendación | Notas |
|---|---|---|
| **Cámara** | **RGB-D (profundidad)**: Intel RealSense D435i / D455, Orbbec Femto, Luxonis OAK-D o equivalente | Es la pieza que decide si el espejo puede recomendar talla. La profundidad da **escala real** sin depender de suposiciones. La D455 tiene mayor alcance, útil si el espacio obliga a separar la cámara |
| **Cómputo** | Mini-PC con GPU: integrada moderna (Intel Iris Xe, AMD Radeon 780M) o dedicada de entrada. 16 GB RAM, SSD | Debe sostener pose + segmentación + render a ≥ 24 FPS. La GPU dedicada da margen para crecer a prendas 3D |
| **Sistema** | Linux o Windows con navegador en modo quiosco, WebGPU habilitado, arranque automático | El navegador de escritorio tiene el soporte de WebGPU y de dispositivos que un panel embebido no tiene |
| **Pantalla** | Táctil comercial 43–55", **orientación vertical**, brillo ≥ 400 nits, antirreflejo | Un espejo de cuerpo entero es vertical. En horizontal la persona sale pequeña y se pierde la ilusión |
| **Iluminación** | Panel LED difuso frontal, temperatura neutra (~4000–5000 K), sin contraluz | Mejora la segmentación **y** hace que el color de la prenda se vea real |
| **Fondo** | Superficie lisa, mate, color uniforme contrastante con la piel y la ropa habitual | Ayuda a la silueta. Evitar espejos reales, vidrio y estampados detrás |
| **Suelo** | Marca física a distancia fija de la cámara | Parte de la calibración (`EN-1411`) |
| **Montaje** | Estructura rígida, cámara solidaria a la pantalla | Si la cámara se mueve, la calibración se pierde (`R-27`) |
| **Red** | Cableada preferible | El catálogo y el stock se consultan en tiempo real |

**Orden de magnitud por instalación completa:** entre USD 2.000 y 4.000, dominado por la pantalla
táctil comercial y la cámara de profundidad. Verificar precios locales antes de presupuestar.

### Lo que descartaría, y por qué

| Opción | Por qué no |
|---|---|
| **Panel de señalización todo-en-uno con Android** | Es la opción que parece más simple: una sola pieza, montaje limpio, gestión remota. Pero su GPU y su navegador embebido no sostienen visión en tiempo real, y WebGPU rara vez está disponible. Termina cancelando la funcionalidad principal |
| **Webcam RGB corriente** | Sin profundidad, la escala se estima, y el error se va al rango `LOW`. Si el objetivo es la máxima precisión, esta es la decisión que lo impide |
| **TV de consumo + Android TV box** | Sin táctil, potencia insuficiente, navegador limitado |
| **Tableta grande** | Pantalla demasiado pequeña para cuerpo entero a 2,5 m |
| **Cámara con gran angular fuerte** | Soluciona la falta de espacio pero introduce distorsión de barril, que degrada la medición justo donde más importa (extremos del cuerpo) |

### Requisitos del espacio físico

Verificar **antes de comprar nada**:

| Requisito | Valor de referencia | Si no se cumple |
|---|---|---|
| Distancia cámara ↔ marca del suelo | 2,5 – 3 m | Cambiar de ubicación. El gran angular no es una solución equivalente |
| Ancho libre | ≥ 1,5 m | La persona debe poder girar |
| Altura de techo | ≥ 2,4 m | Montaje de cámara y pantalla |
| Campo de visión | **Sin puertas ni cortinas de probador en el encuadre** | Requisito de privacidad, no una preferencia (`EN-1410`) |
| Luz solar directa | Ausente o controlable | Iluminación cambiante degrada la medición |
| Toma eléctrica y red | Disponibles en el punto | — |

La primera fila es la que más veces bloquea el proyecto: los pasillos de vestier suelen ser
estrechos. **Medir el sitio real es la primera tarea de la instalación**, antes que cualquier compra.

### Prototipo antes de comprar

Para `SP-1405` no hace falta la instalación definitiva. Con una cámara de profundidad de desarrollo,
un portátil con GPU, un trípode, una lámpara y una tela lisa de fondo se puede montar el banco de
pruebas en un día y obtener el número de precisión real. **Ese número decide la compra**, no al revés.

---

## 8. Riesgos del stack y sus salidas

| Riesgo | Salida preparada |
|---|---|
| ML Kit Pose cambia o se retira (`R-05`) | Interfaz `PoseDetector` propia; MediaPipe como implementación alterna ya probada |
| ARCore no cubre los dispositivos objetivo (`R-06`) | Matriz de compatibilidad + fallback 2D garantizado y probado |
| El proveedor gratuito cambia condiciones (`R-14`) | Capa Repository + Supabase (PostgreSQL real) hace la migración un `pg_dump` |
| Un proveedor de IA sube precios o cambia el modelo (`R-09`) | Gateway agnóstico + suite de evaluación que permite comparar candidatos objetivamente |
| No hay activos 3D estandarizados (`R-01`) | Spike temprano `SP-0606`; solo entran a AR las categorías viables; el resto se queda en 2D |
| El equipo es una sola persona (`R-08`) | Ver los escenarios de capacidad en [17-plan-de-trabajo.md](17-plan-de-trabajo.md) |
