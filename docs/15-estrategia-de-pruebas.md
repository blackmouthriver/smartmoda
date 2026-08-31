# 15 · Estrategia de pruebas

Pediste pruebas unitarias, de interfaz y de flujo. Este documento define qué se prueba en cada nivel,
con qué herramienta, y —lo más importante— **qué rompe el build**.

Principio rector: **una prueba que no puede fallar no es una prueba.** Si añadir un `if` mal escrito
no hace fallar nada, la cobertura es decorativa. Por eso hay pruebas de mutación en el dominio.

---

## 1. Pirámide

```
                    ╱╲
                   ╱E2E╲            ~20 flujos · lentos · frágiles · los críticos
                  ╱──────╲
                 ╱ Interfaz╲        ~150 · pantallas y componentes
                ╱────────────╲
               ╱ Integración   ╲    ~250 · API + base real + contratos
              ╱──────────────────╲
             ╱     Unitarias       ╲ ~1500 · dominio, políticas, reglas
            ╱────────────────────────╲
```

Distribución objetivo: 70% unitarias, 20% integración, 8% interfaz, 2% extremo a extremo.

Si esta forma se invierte, cada cambio tarda 40 minutos en validarse y el equipo deja de correr las
pruebas. Ese es el fallo real, no la cobertura.

---

## 2. Pruebas unitarias

**Qué se prueba:** lógica de dominio pura. Sin Android, sin Spring, sin base de datos, sin red.

| Módulo | Casos representativos |
|---|---|
| `sizing` | Cada regla RN-001, RN-002, RN-005, RN-016, RN-017. Tabla ausente, medidas incompletas, entre dos tallas, unidades mixtas, valores en el borde exacto del rango |
| `tenancy` | Derivación de contexto, cuotas, rechazo de tenant del cliente (RN-009) |
| `consent` | Vigencia, revocación, versión de política, propagación del borrado (RN-006, RN-021) |
| `commerce` | Cálculo de totales, cupones, idempotencia, revalidación de precio (RN-003, RN-010, RN-023) |
| `audit` | Construcción del registro, redacción de datos sensibles, cadena de hash (RN-031, RN-033) |
| `catalog` | Validación de publicación (RN-024), versionado de tablas de tallas (RN-015) |

**Herramientas:** JUnit 5, MockK, Kotest para propiedades, Turbine para `Flow`.

**Convenciones:**

```kotlin
@Test
fun `RN_001 sin tabla de tallas aplicable no recomienda talla`() {
    val result = policy.recommend(profile, variantSinTabla)
    assertThat(result).isInstanceOf<SizeRecommendation.Unavailable>()
}
```

- El nombre empieza por el ID de la regla que verifica. Un script de CI (`AUT-18`) comprueba que
  **cada `RN-xxx` del doc 06 tiene al menos una prueba que lo referencia**. Sin ese script, la
  trazabilidad se degrada en tres sprints.
- Un caso por comportamiento, no un test gigante con veinte aserciones.
- **Pruebas basadas en propiedades** donde el dominio lo permite: por ejemplo, "convertir cm→pulgadas→cm
  devuelve el valor original dentro de la tolerancia" para cualquier medida válida. Encuentra casos
  límite que nadie escribiría a mano.

**Gate:** cobertura ≥ 85% en módulos de dominio, ≥ 70% global. Pruebas de mutación (Pitest) sobre
`sizing`, `tenancy` y `commerce` con umbral acordado en Fase 2.

---

## 3. Pruebas de integración

**Qué se prueba:** los módulos hablando con infraestructura real.

| Tipo | Herramienta | Casos |
|---|---|---|
| API + base de datos | Testcontainers (PostgreSQL real, no H2) | CRUD, transacciones, migraciones Flyway |
| **Aislamiento multi-tenant** | Suite generada desde OpenAPI | Token de A + recurso de B ⇒ `404`, para **todos** los endpoints |
| RLS | Testcontainers | Sin `app.tenant_id` ⇒ 0 filas en toda tabla con `tenant_id` |
| Seguridad de auth | RestAssured | Los 9 criterios de `EN-1510`, incluida la comparación de tiempos de respuesta |
| Idempotencia | RestAssured | Misma `Idempotency-Key` dos veces ⇒ un solo efecto (RN-010) |
| Contrato de API | Verificación de compatibilidad OpenAPI | Un cambio incompatible rompe el build |
| Webhooks | Testcontainers + WireMock | Reintentos, orden, duplicados, cola de fallos |
| Auditoría | Testcontainers | `UPDATE`/`DELETE` sobre `audit_log` rechazado; cadena de hash íntegra |
| Persistencia móvil | Room in-memory + migraciones reales | Migraciones de Room verificadas, no asumidas |
| Cifrado E2E | Prueba negativa | El servidor recupera el blob y **no puede descifrarlo** (RN-020) |
| Fase 1 (BaaS) | Firebase Emulator Suite (`AUT-04`) | Reglas de seguridad, Auth, Storage |

**H2 no sustituye a PostgreSQL.** RLS, `jsonb`, particionado, PostGIS y columnas generadas no existen
o se comportan distinto. Probar contra H2 y desplegar contra PostgreSQL es probar otra aplicación.

---

## 4. Pruebas de interfaz

### Android (Compose)

| Nivel | Herramienta | Qué verifica |
|---|---|---|
| Componentes y pantallas | Compose UI Test | Estados `Loading` / `Content` / `Empty` / `Error` de cada pantalla |
| Instantáneas | Paparazzi | Regresión visual sin emulador; claro y oscuro; escala de texto 100% y 200% |
| Accesibilidad | Accessibility Scanner en CI | Contraste, área táctil ≥ 48 dp, etiquetas de contenido |
| Cámara y AR | Test instrumentado en Firebase Test Lab (`EN-0611`) | Permisos concedidos/denegados, dispositivo sin ARCore ⇒ fallback 2D |
| Rendimiento | Macrobenchmark | Arranque en frío p95 ≤ 3 s; fotogramas perdidos en scroll de catálogo |

### Web

| Nivel | Herramienta | Qué verifica |
|---|---|---|
| Componentes | Vitest + Testing Library | Comportamiento, no implementación: consultas por rol accesible, no por `data-testid` |
| Visual | Storybook + pruebas de instantánea | Sistema de diseño en claro y oscuro |
| Accesibilidad | axe-core en CI (`AUT-20`) | 0 violaciones serias o críticas en rutas críticas |
| Rendimiento | Lighthouse CI (`AUT-20`) | LCP ≤ 2,5 s, INP ≤ 200 ms |
| Responsividad | Playwright, viewports 360/768/1440 | Sin desbordamiento horizontal, sin acciones inalcanzables |

**Regla:** las pruebas de interfaz consultan por **rol accesible y texto visible**, no por
identificadores de prueba. Si una prueba no puede encontrar el botón como lo encontraría un lector de
pantalla, la pantalla tiene un problema de accesibilidad que la prueba está ocultando.

---

## 5. Pruebas de flujo (extremo a extremo)

Pocas, críticas, estables. Contra un entorno desplegado con datos sembrados de forma determinista.

| # | Flujo | Canal | Fase |
|---|---|---|---|
| F-01 | Registro → consentimiento → medidas manuales → talla recomendada | Móvil | 1 |
| F-02 | Catálogo → detalle → captura guiada → overlay 2D → guardar prueba | Móvil | 1 |
| F-03 | Rechazar todo consentimiento opcional → el núcleo sigue funcionando (RN-007) | Móvil | 1 |
| F-04 | Revocar consentimiento → borrado del perfil verificado | Móvil | 1 |
| F-05 | Modo avión → operar offline → reconectar → sincronizar sin pérdida | Móvil | 1 |
| F-06 | Login administrativo con MFA → crear producto → subir activo → publicar → aparece en la app | Web + Móvil | 2 |
| F-07 | Importación masiva: validación en seco → corregir errores → importar → revertir lote | Web | 2 |
| F-08 | **Aislamiento**: admin de A intenta ver, editar y exportar datos de B ⇒ `404` en los tres | Web | 2 |
| F-09 | Prueba AR en dispositivo compatible; y fallback 2D en dispositivo sin ARCore | Móvil | 2 |
| F-10 | Carrito → reserva en tienda → handoff a checkout | Móvil | 2 |
| F-11 | Stylist: solicitud → outfits con stock real → probar look → guardar | Móvil | 3 |
| F-12 | Super admin: crear tenant → tema → funciones → el tenant opera | Web | 3 |
| F-13 | Impersonación: motivo → MFA → solo lectura → expira → auditado y notificado | Web | 3 |
| F-14 | Kiosco: sesión invitada → QR → continuar en móvil → borrado verificado (RN-013) | Kiosco + Móvil | 3 |
| F-15 | Auditoría: acción administrativa → aparece en el log → exportación auditada | Web | 3 |
| F-16 | Pago → orden → devolución → conciliación de webhook | Móvil + Web | 3 |
| F-17 | Fuerza bruta: intentos fallidos → backoff → bloqueo → notificación → recuperación | API | 1 |
| F-18 | Probador web por webcam: consentimiento → cámara → overlay → denegar permiso ⇒ foto fija | Web | 3 |
| F-19 | Superar cuota del plan → degradación con aviso, sin pérdida de datos (RN-028) | Web | 3 |
| F-20 | Exportación de datos del usuario → descifrado local → paquete completo | Móvil | 2 |

**Herramientas:** Playwright (web), Maestro o Espresso (móvil), entorno de preproducción con datos
sembrados y reiniciados antes de cada corrida.

**Reglas contra la fragilidad:**
- Sin esperas fijas: siempre esperar a una condición observable.
- Datos sembrados de forma determinista, nunca dependientes de una corrida anterior.
- Una prueba intermitente se **arregla o se elimina** en 48 horas. Una suite con fallos aleatorios
  entrena al equipo a ignorar el rojo, y eso es peor que no tener la suite.

---

## 6. Pruebas no funcionales

| Tipo | Herramienta | Cuándo | Umbral |
|---|---|---|---|
| Carga | k6 | Antes de cada release de fase | RNF-01 y RNF-11 |
| Estrés | k6 | Antes de producción SaaS | Degradación elegante, sin corrupción |
| Resistencia | k6, 4 h | Antes de producción | Sin fugas de memoria ni de conexiones |
| Seguridad automática | OWASP ZAP, escaneo de dependencias, Gitleaks | Cada PR + diario | 0 crítico/alto |
| Penetración | Externa | `EN-1507`, antes de producción SaaS | Remediación de todo lo crítico/alto |
| Recuperación | Simulacro de restauración | Antes de lanzamiento + semestral | RPO ≤ 15 min, RTO ≤ 4 h |
| Accesibilidad | axe + Accessibility Scanner + revisión manual con lector de pantalla | Cada release | WCAG 2.1 AA |
| Batería y temperatura | Sesión AR de 10 minutos | Cada release con AR | Sin throttling térmico ni congelamiento |
| Evaluación de IA | Suite propia (`AUT-12`) | Cada cambio de modelo o prompt | Doc 14 § 5 |

---

## 7. Datos de prueba

| Necesidad | Enfoque |
|---|---|
| Catálogo | Generador determinista con semilla: 3 tenants, 2 marcas, 5 tiendas, 200 productos, 1.200 variantes |
| Perfiles corporales | Conjunto **diverso** de perfiles sintéticos: distintas proporciones, alturas y tipos de cuerpo (mitigación de `R-13`) |
| Fotos de prueba | Banco propio con consentimiento explícito y diversidad de tono de piel, iluminación, fondo y vestimenta |
| Tablas de tallas | Al menos 3 marcas con tablas deliberadamente inconsistentes entre sí (así es la realidad, `R-03`) |
| Datos personales | **Nunca datos reales en entornos que no sean producción.** Sintéticos o anonimizados irreversiblemente |

El banco de fotos diverso no es un detalle de cumplimiento: es la única forma de detectar que el
detector de pose funciona peor con cierta ropa, cierto fondo o cierto tono de piel antes de que lo
descubra un usuario.

---

## 8. Definición de terminado

Una historia está terminada cuando:

- [ ] Todos sus criterios de aceptación tienen una prueba automatizada que los verifica
- [ ] Las reglas de negocio que toca tienen prueba con el ID en el nombre
- [ ] Cobertura del dominio nuevo ≥ 85%
- [ ] Si añade un endpoint: está en el OpenAPI y tiene su prueba de aislamiento multi-tenant
- [ ] Si añade una tabla de negocio: tiene `tenant_id NOT NULL` y política RLS (`AUT-22`)
- [ ] Si añade acción administrativa: genera registro de auditoría, verificado por prueba
- [ ] Si añade pantalla: pruebas de estado, instantánea y accesibilidad
- [ ] 0 hallazgos críticos o altos de seguridad
- [ ] Lint, formato y ArchUnit en verde
- [ ] Documentación actualizada **en el mismo PR** (los `.md` de este repositorio)
- [ ] Revisada por otra persona, o —si el equipo es de uno— con lista de verificación de autorrevisión
      registrada

---

## 9. Gates de CI (resumen)

| Gate | Bloquea |
|---|---|
| Compilación + lint + formato | Merge |
| Unitarias + cobertura | Merge |
| Integración (Testcontainers) | Merge |
| **Aislamiento multi-tenant** (`AUT-17`) | Merge |
| **Trazabilidad `RN-xxx` ⇄ prueba** (`AUT-18`) | Merge |
| **Fuerza bruta y límite de tasa** (`AUT-19`) | Merge |
| Lighthouse + axe (web) (`AUT-20`) | Merge |
| Tokens de diseño y contraste (`AUT-21`) | Merge |
| ArchUnit (`AUT-23`) | Merge |
| Secretos y dependencias (`AUT-10`) | Merge |
| Contrato OpenAPI (`AUT-05`) | Merge |
| Migración sin `tenant_id`/RLS (`AUT-22`) | **Deploy** |
| Extremo a extremo | Release |
| Carga y seguridad | Release de fase |

En Fase 1, con un equipo pequeño, es tentador desactivar gates para avanzar. La regla es: **se pueden
posponer los gates de rendimiento; no se pueden posponer los de seguridad, aislamiento ni auditoría.**
Esos tres son los que no se pueden reparar retroactivamente.
