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
| `EN-1510` Defensa contra fuerza bruta | 8 | 🟡 En curso | Política lista; **falta conectarla al flujo real** |

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

### Pendiente concreto

1. Configurar límites de tasa en el panel de Supabase.
2. Conectar `BruteForcePolicy` a un Auth Hook que lea `auth_attempt`.
3. Pantallas de `US-0101`, `US-0102`, `US-0104` y `US-0105`.
4. `EN-1502`: reglas de seguridad y atestación de cliente.
