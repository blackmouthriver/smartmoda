# Sprint 1 · Onboarding seguro

**Objetivo:** que alguien pueda registrarse, entrar y dar consentimiento granular, con el acceso
defendido de fuerza bruta.

**Carga:** 6 ítems · 32 SP · [plan completo](../17-plan-de-trabajo.md)

---

## Estado

| Ítem | SP | Estado | Qué falta |
|---|---|---|---|
| `US-0101` Onboarding orientado a valor | 3 | 🔴 Pendiente | Pantallas |
| `US-0102` Registro con correo | 5 | 🟡 En curso | Esquema listo; falta UI y wiring de Supabase Auth |
| `US-0104` Inicio, cierre y recuperación | 5 | 🔴 Pendiente | UI y wiring |
| `US-0105` Consentimiento explícito y versionado | 5 | 🟡 En curso | Dominio y esquema listos; falta UI |
| `EN-1502` Reglas de seguridad y App Check | 5 | 🔴 Pendiente | — |
| `EN-1510` Defensa contra fuerza bruta | 8 | 🟠 **Bloqueado por plan** | El hook exige Team/Enterprise. Ver abajo |

---

## Lo que ya está

### Dominio de consentimiento (`US-0105`)

`shared-domain/consent/` con `ConsentPolicy`, que implementa cuatro reglas:

| Regla | Qué garantiza |
|---|---|
| RN-006 | Nada se captura antes del consentimiento **específico y vigente** |
| RN-007 | Rechazar lo opcional no bloquea el núcleo |
| RN-021 | Revocar identifica qué datos derivados hay que borrar, y en 72 h |
| RN-022 | Los flujos corporales están vetados para perfiles de menor |

Tres decisiones que quedaron en el tipo, no en la revisión:

- **`ConsentDecision` es sellado, no un booleano.** El motivo de la negativa determina qué hace
  la interfaz: pedir consentimiento, mostrar el texto nuevo, o explicar que esa función no
  existe para ese perfil. Un `false` obliga a adivinarlo.
- **Las finalidades se piden por separado.** Un consentimiento global no es específico, y bajo
  Ley 1581 el de dato sensible debe ser cualificado. Aceptar medidas no autoriza la cámara, y
  hay una prueba que lo fija.
- **`derivedDataToErase()` vive en el dominio** y sus identificadores son los de
  `contracts/data-map.yaml`. Eso permite probar que revocar borra lo que debe.

### Política de fuerza bruta (`EN-1510`)

`shared-domain/auth/BruteForcePolicy`: retroceso exponencial por cuenta, bloqueo temporal,
desafío por umbral y detección de credential stuffing por tasa agregada.

Dos decisiones que conviene entender:

- **Se cuenta por cuenta, no por IP.** Quien ataca con botnet rota la IP en cada intento pero
  apunta siempre a la misma cuenta.
- **El bloqueo es temporal a propósito.** Uno permanente convierte la fuerza bruta en
  denegación de servicio: bastaría fallar diez veces contra la cuenta de alguien para dejarlo
  fuera indefinidamente.

### Esquema (`0004_identity_consent.sql`)

`app_user`, `consent`, `erasure_task`, `auth_attempt`.

- El `consent` **no se actualiza ni se borra**: revocar es insertar una fila nueva. Reglas de
  base de datos lo impiden. Borrar la prueba del consentimiento perjudica a quien tendría que
  demostrarlo.
- `auth_attempt` guarda el **hash** del identificador, no el correo: esa tabla la podría leer
  quien logre una inyección, y una lista de correos registrados ya es aprovechable.

---

## Lo que falta, y una advertencia sobre `EN-1510`

**La política de fuerza bruta todavía no protege nada.** Está escrita y probada, pero no está
conectada al flujo de autenticación real.

Y aquí hay algo que no conviene disimular: **una defensa aplicada en el cliente no defiende de
nada.** Quien ataca llama a la API directamente y nunca ejecuta la aplicación. La política solo
sirve si corre en el servidor.

En Fase 1 el servidor de autenticación es Supabase, así que hay tres opciones:

| Opción | Qué protege de verdad |
|---|---|
| Configurar los límites propios de Supabase Auth | Algo. Es lo mínimo y hay que hacerlo ya |
| Conectar `BruteForcePolicy` a un Auth Hook de Supabase | La política completa, en el servidor |
| Esperar a `core-api` en el Sprint 6 | La política completa, pero seis sprints sin ella |

Lo honesto es hacer la primera ahora y la segunda en este sprint. Mientras tanto, **quien
protege el endpoint son los límites de Supabase, no nuestro código**, y conviene tenerlo claro
antes de dar `EN-1510` por cerrado.

### El hook no se puede registrar en el plan gratuito

Descubierto el 25-sep-2026 al intentar registrarlo: *Password Verification Attempt hook* está
marcado como **Team or Enterprise Plan required**. En FREE no aparece.

**Y la alternativa evidente no sirve.** Poner una Edge Function delante del login no protege
nada: el endpoint real (`/auth/v1/token`) sigue siendo accesible con la clave anon, que va
dentro del APK. Un atacante lo llama directo y se salta el proxy. Sería seguridad de adorno.

En el plan gratuito **no controlamos el endpoint de autenticación**, y sin eso no hay política
propia que se pueda imponer.

#### Lo que sí protege, y está aplicado

| Control | Dónde | Estado |
|---|---|---|
| CAPTCHA | *Authentication → Attack Protection* | **Activar** |
| Límite de intentos de acceso | *Authentication → Rate Limits* | Revisar |
| Longitud mínima 12 | Aplicado por script | ✅ |
| Contraseñas filtradas (HIBP) | Aplicado por script | ✅ |
| Rotación de refresh con detección de reuso | Por defecto en Supabase | ✅ |

Estos corren **en el endpoint de Supabase**, así que no se esquivan. No son nuestra política
completa, pero cubren el grueso de la amenaza.

#### La brecha que queda, declarada

Lo que **no** tenemos hasta el Sprint 6: bloqueo por cuenta con retroceso exponencial, y
respuestas indistinguibles entre "cuenta no existe" y "contraseña incorrecta".

El código está escrito y probado (`BruteForcePolicy.kt` y `0005_auth_hook.sql`, 20 pruebas
contra Postgres real). No se retira: se activa el día que ocurra lo primero de estas dos cosas.

| Disparador | Qué se hace |
|---|---|
| El negocio justifica el plan Team | Registrar el hook. Es un clic, el código ya existe |
| Llega `core-api` (Sprint 6, `EN-1201`) | Controlamos el endpoint: se aplica `BruteForcePolicy` directamente y el hook SQL se retira |

Riesgo `R-33`, aceptado con fecha de revisión.

### Consecuencia de activar CAPTCHA en las pantallas de acceso

Con CAPTCHA activo, `signInWithPassword` y `signUp` **exigen un token de captcha**. Sin él,
Supabase rechaza la petición.

Impacta a `US-0102` y `US-0104`: hay que integrar el SDK del proveedor en Android antes de que
registro e inicio de sesión funcionen. Es trabajo acotado, pero conviene tenerlo en la
estimación en vez de descubrirlo como un error opaco el día que la pantalla parezca terminada.

| Qué | Dónde |
|---|---|
| SDK del proveedor (hCaptcha o Turnstile) | `android/feature/auth` |
| Token adjunto en la petición | Al llamar a Supabase Auth |
| Camino cuando el captcha falla | RN-019: degradar con mensaje claro, no dejar la pantalla muerta |

Y una nota de accesibilidad: un CAPTCHA visual es una barrera para quien usa lector de
pantalla. El proveedor debe ofrecer alternativa accesible, y eso entra en la revisión de
`EN-1503` (RNF-06).

### Pendiente concreto

1. Configurar límites de tasa en el panel de Supabase.
2. Conectar `BruteForcePolicy` a un Auth Hook que lea `auth_attempt`.
3. Pantallas de `US-0101`, `US-0102`, `US-0104` y `US-0105`.
4. `EN-1502`: reglas de seguridad y atestación de cliente.
