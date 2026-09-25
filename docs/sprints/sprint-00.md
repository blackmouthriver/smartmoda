# Sprint 0 · Fundación y decisiones irreversibles

**Objetivo:** dejar montado el terreno para que todo lo demás se construya con calidad repetible, y
tomar las decisiones que son caras de revertir.

**Carga:** 6 ítems · 26 SP · [plan completo](../17-plan-de-trabajo.md)

> **Criterio de cierre del sprint:** un `git push` dispara CI, CI verifica trazabilidad y secretos, y
> el motor de tallas pasa sus pruebas. Si eso funciona, el Sprint 1 puede empezar sin fricción.

---

## Estado

| Ítem | SP | Estado | Qué falta |
|---|---|---|---|
| `US-0001` Contrato de producto y alcance MVP | 3 | 🟢 **Hecho** | — |
| `EN-0002` Arquitectura modular y offline-first | 5 | 🟢 **Hecho** | — |
| `EN-0003` Repositorio, estrategia Git y plantillas | 3 | 🟢 **Hecho** | — |
| `EN-0004` Integración continua | 5 | 🟢 **Hecho** | Validar el primer run en GitHub |
| `EN-0005` Entornos y gestión de secretos | 3 | 🟡 **Casi** | Crear el proyecto Supabase y cargar 2 secretos en GitHub |
| `EN-1501` Mapa de datos y modelo de amenazas | 5 | 🟢 **Hecho** | — |

---

## Lo que ya está en el repositorio

### `US-0001` · Contrato de producto

No hubo que escribirlo en este sprint: existe repartido en la documentación y las decisiones ya están
tomadas y firmadas.

- Visión, problema, propuesta de valor y alcance por fases → [doc 01](../01-contexto-y-vision.md)
- Métricas de éxito y North Star → [doc 01 § 5](../01-contexto-y-vision.md)
- Exclusiones explícitas del MVP → [ADR-0006](../adr/ADR-0006-probador-progresivo.md)
- 14 decisiones de arquitectura firmadas → [docs/adr](../adr/)

### `EN-0003` · Repositorio y estrategia Git

| Archivo | Qué resuelve |
|---|---|
| `CONTRIBUTING.md` | Ramas, commits con `AB#`, PR, definición de terminado, reglas que CI verifica |
| `.gitignore` | Secretos, artefactos de compilación, `.ado_ids.json`, temporales de Office |
| `.gitattributes` | Finales de línea normalizados — evita el ruido clásico de CRLF en Windows |
| `.editorconfig` | Formato uniforme sin depender del IDE |
| `.github/pull_request_template.md` | La definición de terminado como lista, no como buena intención |
| `.github/ISSUE_TEMPLATE/spike.md` | Un spike sin criterio de decisión previo no es un spike |

### `EN-0002` · Arquitectura — el dominio compartido

`shared-domain` en Kotlin puro: sin Android, sin Spring, sin red. Corre en el móvil (sin conexión),
en el servidor y compilado a WebAssembly en el navegador.

```
shared-domain/src/main/kotlin/com/synaptia/smartmoda/domain/
├── common/
│   ├── Millimeters.kt      medidas en enteros: comparaciones exactas en los bordes de talla
│   ├── TenantId.kt         ADR-0002 · existe desde el Sprint 0 aunque haya un solo tenant
│   └── Result.kt           resultado explícito; un fallo de negocio no es una excepción
└── sizing/
    ├── BodyProfile.kt              perfil, fuentes de medida y preferencia de ajuste
    ├── SizeChart.kt                tabla versionada · RN-015
    ├── SizeRecommendation.kt       tipo sellado con confianza obligatoria · RN-016
    └── SizeRecommendationPolicy.kt el motor · ADR-0007
```

**Por qué el motor de tallas se hizo primero**, antes que cualquier pantalla: es la promesa central del
producto y el hito M5 del plan. Si la talla no acierta, el probador no importa.

Decisiones que quedaron codificadas, no solo documentadas:

- `SizeRecommendation` es un **tipo sellado**, no una talla anulable. La interfaz está obligada a
  manejar el caso *"no puedo recomendarte"*, que es el que RN-001 protege y el que un `String?`
  invita a olvidar.
- `Confidence` **no tiene valor nulo**. RN-016 deja de depender de que alguien se acuerde.
- Las medidas son **enteros en milímetros**. Los decimales flotantes producen comparaciones
  inestables justo en el borde de un rango, que es donde se decide una talla.
- `BodyProfile.mergeWith` implementa RN-005 en el modelo: una estimación **no puede** pisar una
  medida manual, aunque el código que la llame se equivoque.

### `EN-0004` · Integración continua

`.github/workflows/ci.yml`, con cuatro gates que bloquean el merge:

| Gate | Verifica |
|---|---|
| `dominio` | Compila, pasa pruebas y cumple cobertura ≥ 85% |
| `trazabilidad` | **AUT-18** · toda `RN-xxx` documentada tiene prueba o waiver declarado |
| `secretos` | **AUT-10** · Gitleaks sobre todo el historial |
| `documentacion` | Cero enlaces rotos en los `.md` |

Herramientas propias en `tools/`:

- `check_traceability.py` — hoy: **39 reglas, 6 con prueba, 33 con waiver fechado, 0 sin cubrir**.
- `check_docs_links.py` — hoy: **168 enlaces, 0 rotos**.

Los waivers de `tools/traceability_waivers.txt` llevan el sprint en que se cubren. Es deuda declarada
con fecha, no una excusa: cada línea tiene que desaparecer.

### Contratos

- `contracts/openapi.yaml` — esqueleto con las reglas transversales documentadas. Se llena en el
  Sprint 6 con `EN-0006`, que es cuando aparece `core-api`.
- `contracts/design-tokens.json` — la paleta del [doc 16](../16-design-system.md) ya ejecutable, con
  `tenantOverridable` marcando qué puede tocar un tenant y qué no, y los pares de contraste que CI
  verificará.

---

## Lo que falta para cerrar el sprint

### 1. Generar el wrapper de Gradle
No está instalado Gradle en la máquina, así que `gradlew` no existe todavía y CI no puede correr.
Se resuelve abriendo el proyecto en Android Studio (lo genera solo) o instalando Gradle una vez:

```powershell
winget install Gradle.Gradle
```

```powershell
gradle wrapper --gradle-version 8.10
```

### 2. Esqueleto de la app Android
Módulos `core/` y `feature/` según [doc 09 § 4](../09-arquitectura.md), con Compose, Hilt, Room y la
abstracción `PoseDetector` desde el principio — ML Kit Pose está en beta y esa interfaz es el seguro
contra el riesgo `R-05`.

### 3. Entornos y secretos (`EN-0005`)
- Crear el proyecto en Supabase.
- Cargar los secretos en *GitHub → Settings → Secrets and variables → Actions*.
- Verificar que `.env` nunca sale del equipo: ya está en `.gitignore`, falta comprobarlo.

### 4. Mapa de datos y modelo de amenazas (`EN-1501`)
El modelo de amenazas ya está en [doc 12 § 1](../12-seguridad-y-privacidad.md). Falta el **inventario
de campos**: para cada dato, su clasificación, dónde se guarda, cuánto se retiene y con qué base
legal. Es lo que hace verificable el resto de controles de privacidad, y el artefacto que se enseña
en una auditoría.

---

## En paralelo, fuera del código

Dos trámites externos que tardan semanas. Conviene lanzarlos ya aunque el desarrollo avance:

| Trámite | Bloquea | Riesgo |
|---|---|---|
| Búsqueda marcaria de *SmartModa* en la SIC | Publicar la app | `R-12` |
| Concepto jurídico sobre imagen de menores en tienda | Desplegar el espejo (Fase 3) | `R-31` |

Y uno comercial que condiciona toda la Fase 2: **conseguir un comercio piloto** (`R-32`). Sin él no
hay dónde validar tallas, activos ni espejo.
