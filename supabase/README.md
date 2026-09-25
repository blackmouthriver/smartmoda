# Supabase · puesta en marcha

> `EN-0005` · Entornos y gestión de secretos · Sprint 0

En Fase 1 Supabase **es** el backend. No hay servicio propio: la app móvil habla directamente
con Supabase a través de la capa Repository ([ADR-0004](../docs/adr/ADR-0004-backend-fase-1.md),
[ADR-0012](../docs/adr/ADR-0012-microservicios-acotados.md)). `core-api` aparece en el Sprint 6 y
entonces solo cambia una implementación de `RemoteDataSource`.

---

## Lo que tienes que hacer tú

No puedo crear la cuenta ni el proyecto: hacen falta tus credenciales. Son cinco minutos.

### 1. Crear el proyecto

En [supabase.com](https://supabase.com) → **New project**

| Campo | Valor |
|---|---|
| Name | `smartmoda-dev` |
| Region | La más cercana a Colombia (normalmente `us-east`) |
| Database password | Genera una larga y **guárdala en tu gestor de contraseñas** |

La contraseña de la base de datos no va en ningún archivo del proyecto. Solo la necesitas para
conectarte directamente con `psql` o desde el panel.

### 2. Aplicar las migraciones

En el panel → **SQL Editor** → pega y ejecuta **en orden**:

1. `migrations/0001_tenancy.sql`
2. `migrations/0002_catalog.sql`
3. `migrations/0003_sizing.sql`

Si prefieres la CLI:

```bash
npx supabase link --project-ref TU-PROJECT-REF
```

```bash
npx supabase db push
```

### 3. Copiar las claves

En el panel → **Project Settings → API**. Verás dos claves. **La diferencia importa mucho:**

| Clave | Qué es | Dónde va |
|---|---|---|
| `anon` / `publishable` | **Pública a propósito.** Va dentro del APK | `local.properties` y secretos de CI |
| `service_role` / `secret` | **Omite el RLS por completo** | **Nunca** en el cliente. Solo servidor, Fase 2 |

Si alguien copia la `service_role` por error, **el build se niega a compilar** y explica por qué
(`gradle/secrets.gradle.kts`). Las dos aparecen juntas en el panel y se copian igual de fácil;
ese guard existe justamente por eso.

### 4. Configurar tu máquina

Añade a `local.properties` (que está en `.gitignore`):

```properties
SUPABASE_URL=https://TU-PROJECT-REF.supabase.co
SUPABASE_ANON_KEY=tu-clave-anon
```

Comprueba que quedó bien:

```bash
./gradlew :android:core:common:assembleDebug
```

### 5. Configurar CI

En GitHub → *Settings → Secrets and variables → Actions → New repository secret*:

| Secreto | Valor |
|---|---|
| `SUPABASE_URL` | La URL del proyecto |
| `SUPABASE_ANON_KEY` | La clave anon |

---

## 6. Endurecer la autenticación (`EN-1510`)

Dos pasos. El primero es un script; el segundo hay que hacerlo en el panel porque los hooks no
se pueden registrar por API.

### 6.1 Límites de tasa

```powershell
$env:SUPABASE_ACCESS_TOKEN = "sbp_..."   # Account > Access Tokens
$env:SUPABASE_PROJECT_REF  = "tu-project-ref"
```

```powershell
python tools/configure_supabase_auth.py --check
```

Muestra qué cambiaría sin tocar nada. Si te cuadra, ejecuta sin `--check`.

Cada valor lleva su motivo al lado en el script. El más importante es
`rate_limit_verify`: el OTP de seis dígitos son un millón de combinaciones, y sin límite se
agotan en minutos.

> El **access token** de la cuenta no es la clave `anon` ni la `service_role`. Sirve para
> administrar proyectos y no debe acabar en ningún archivo del repositorio.

### 6.2 El Auth Hook: solo en plan Team o superior

*Authentication → Auth Hooks → **Password Verification Attempt***

> **En el plan gratuito esta opción aparece en gris.** Supabase la reserva a Team y Enterprise.
> Comprobado el 25-sep-2026 en `smartmoda-dev`.

Mientras tanto, lo que sí se puede y hay que hacer:

1. *Authentication → **Attack Protection*** → activar **CAPTCHA** (hCaptcha o Turnstile).
   Es el control más valioso disponible en FREE, porque se aplica en el endpoint de Supabase
   y no se puede esquivar.
2. *Authentication → **Rate Limits*** → revisar el límite de sign in / sign up.

**Por qué no basta con una Edge Function delante del login:** el endpoint real
`/auth/v1/token` sigue accesible con la clave anon, que va dentro del APK. Un atacante lo
llama directo y se salta cualquier proxy nuestro. En el plan gratuito no controlamos el
endpoint de autenticación, y sin eso no hay política propia que imponer.

El código del hook no se retira: se registra el día que el plan lo permita, o se aplica desde
`core-api` en el Sprint 6. Riesgo `R-33` en [docs/19](../docs/19-riesgos.md).

Con eso, cada verificación de contraseña pasa por
[`0005_auth_hook.sql`](migrations/0005_auth_hook.sql), que registra el intento y aplica el
retroceso, el desafío y el bloqueo.

**Por qué aquí y no en la app:** una defensa aplicada en el cliente no defiende de nada. Quien
ataca llama a la API directamente y nunca ejecuta la aplicación. Este hook corre *dentro* de
Supabase Auth, antes de conceder la sesión.

**Una deuda que conviene conocer.** El hook duplica en SQL la lógica de
`shared-domain/auth/BruteForcePolicy.kt`. Dos implementaciones de la misma regla pueden
divergir. Se asume porque es la única forma de tener la defensa en el servidor durante la
Fase 1, y se controla así:

- Las constantes viven en un solo sitio por lado (`app.bf_config` y el `object` de Kotlin).
- `supabase/tests/test_auth_hook.sql` comprueba los **mismos casos** que
  `BruteForcePolicyTest.kt`, **y además que las constantes coincidan**. Si alguien cambia un
  umbral en un lado y no en el otro, la prueba falla.
- En el Sprint 6, con `core-api`, el hook se retira y vuelve a haber una sola implementación.

```bash
python tools/run_sql_tests.py
```

Levanta PostgreSQL en Docker, aplica las cinco migraciones y ejecuta las pruebas. Es lo que
demuestra que el hook hace lo que dice.

---

## Una consecuencia de ADR-0004 que conviene tener presente

En Fase 1 la app consulta la base de datos **directamente**, con una clave pública que va
dentro del APK. Cualquiera puede extraerla con herramientas estándar.

Eso significa que aquí **el Row Level Security no es una red de seguridad: es la seguridad.**
Lo único que separa a un atacante de los datos de otro tenant son las políticas de
`0001_tenancy.sql`. En Fase 2, con `core-api` de por medio, el RLS vuelve a ser la segunda capa
detrás de la aplicación.

Tres consecuencias prácticas mientras dure la Fase 1:

1. **Ninguna tabla nueva sin RLS.** `tools/check_migrations.py` lo verifica y rompe el
   despliegue (`AUT-22`).
2. **Ninguna política solo con `USING`.** Sin `WITH CHECK` se puede leer bien pero *insertar*
   filas en otro tenant. Es el fallo silencioso más común y el checker lo detecta.
3. **`FORCE ROW LEVEL SECURITY` siempre.** Sin él, el rol propietario de la tabla ignora la
   política — y ese es justo el rol con el que corren las aplicaciones mal configuradas.

---

## Qué hay en las migraciones

| Archivo | Contenido |
|---|---|
| `0001_tenancy.sql` | Tabla `tenant`, función `app.current_tenant_id()` y el patrón de aislamiento |
| `0002_catalog.sql` | `brand`, `store`, `category`, `color`, `product`, `product_variant`, `stock` |
| `0003_sizing.sql` | Tablas de talla versionadas y la vista `public_catalog` |

`app.current_tenant_id()` resuelve las dos formas en que llega el tenant según la fase:

```
Fase 1 · Supabase  →  claim `tenant_id` dentro del JWT
Fase 2 · core-api  →  SET LOCAL app.tenant_id al abrir la transacción
```

Si no hay ninguno, devuelve `NULL` y las políticas no casan con nada: **falla cerrado**, que es
el comportamiento correcto.

---

## Verificación

```bash
python tools/check_migrations.py
```

Comprueba que toda tabla de negocio tiene `tenant_id NOT NULL`, RLS habilitado **y forzado**, y
al menos una política con `WITH CHECK`. Las excepciones legítimas (`tenant`, `category`, `color`)
están declaradas con su motivo en el propio script.

---

## Lo que NO se hace en Fase 1

- **No se crea un servicio propio.** Rompería la inversión cero sin aportar nada todavía.
- **No se usa la `service_role` en ningún sitio.** Ni en la app, ni en CI, ni en un script.
- **No se relaja el RLS "temporalmente" para depurar.** Si una consulta no devuelve datos,
  el problema es el contexto de tenant, no la política.
