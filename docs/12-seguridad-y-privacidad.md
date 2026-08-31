# 12 · Seguridad y privacidad

Este producto trata **imágenes de cuerpo completo, medidas corporales y rostros**. Bajo la Ley 1581 de
2012 y el criterio de la Superintendencia de Industria y Comercio, eso es dato sensible con exigencia
de autorización **previa, expresa, informada y cualificada**. El riesgo `R-04` está clasificado como
crítico con razón: un incidente aquí no es una multa, es el cierre del producto.

La estrategia de fondo es **eliminar la clase de riesgo, no gestionarla**. Se aplica en dos canales:

- **Móvil:** el perfil corporal vive cifrado en el dispositivo y no se envía
  ([ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md)).
- **Espejo en tienda:** la sesión es anónima, las medidas viven solo en memoria y lo único que sale
  del dispositivo son bandas de talla agregadas
  ([ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md)).

Si el servidor nunca recibe el dato, una fuga de la base de datos no es una fuga de datos corporales.

---

## 1. Modelo de amenazas (STRIDE resumido)

| Amenaza | Vector concreto | Control |
|---|---|---|
| **Spoofing** | Credenciales robadas o reutilizadas | MFA en roles administrativos, detección de credential stuffing, notificación de acceso nuevo |
| **Tampering** | Manipulación de `tenant_id` o de precios en el cliente | Contexto derivado del servidor (RN-009), precio recalculado en el servidor siempre |
| **Repudiation** | "Yo no borré ese producto" | Auditoría append-only con cadena de hash (EP-18) |
| **Information disclosure** | Fuga entre tenants; fuga de datos corporales; enumeración de usuarios | RLS + pruebas negativas; cifrado E2E; respuestas indistinguibles |
| **Denial of service** | Fuerza bruta, scraping del catálogo, abuso de la IA | Límite de tasa por tenant/IP/cuenta, cuotas, circuit breaker |
| **Elevation of privilege** | Escalada por rol mal validado; impersonación abusada | RBAC con ámbito verificado en servidor, impersonación de solo lectura con four-eyes |

Amenazas específicas de este dominio, que no aparecen en un modelo genérico:

| Amenaza | Por qué es propia de este producto | Control |
|---|---|---|
| **Reidentificación por medidas** | Un conjunto de medidas corporales es casi un identificador único | Agregados con mínimo 30 usuarios por celda; ninguna medida individual sale del dispositivo |
| **Uso del rostro como plantilla biométrica** | Generar un avatar facial puede derivar en tratamiento biométrico de identificación | Solo aproximación estilizada, on-device, consentimiento separado, sin plantilla de reconocimiento (US-0307) |
| **Filtración por imagen compartida** | Una prueba compartida puede llevar EXIF con ubicación, o el rostro sin querer | Sanitización obligatoria, rostro oculto por defecto al compartir (RN-008) |
| **Kiosco con datos del cliente anterior** | La persona siguiente ve el cuerpo del anterior | Sesión efímera con borrado verificable y evidencia auditable (RN-013, EN-1404) |
| **Captura pasiva en espacio público** | Una cámara siempre encendida en una tienda mide a quien pase, sin que lo sepa | La cámara del espejo está **apagada hasta que alguien toca la pantalla y acepta**; señalización previa; alternativa sin cámara (`EN-1410`) |
| **Deriva del espejo hacia la vigilancia** | La tentación de contar visitantes únicos o reconocer recurrentes convierte un probador en un sistema de vigilancia | Prohibición explícita de diseño: `session_hash` aleatorio no derivado de nada estable, sin conteo de únicos, sin correlación entre sesiones (`EN-1408` CA-3 y CA-6) |
| **Correlación entre tenants** | Dos comercios cruzando datos para perfilar al mismo usuario | Seudónimo con sal distinta por tenant; ningún identificador común expuesto |

---

## 2. Clasificación de datos

| Nivel | Datos | Tratamiento |
|---|---|---|
| **Crítico** | Fotos corporales, silueta, landmarks de pose, medidas, avatar, rostro | **No salen del dispositivo.** Cifrado local (Keystore) en móvil; en el espejo viven solo en memoria durante la sesión. Si el usuario activa sincronización, blob cifrado E2E ilegible para la plataforma. Nunca en logs ni en analítica individual |
| **Alto** | Contraseñas, tokens, claves, datos de pago | Argon2id / bóveda de claves / tokenización en la pasarela. Nunca almacenamos PAN |
| **Medio** | Correo, nombre, historial de pruebas, favoritos, órdenes | Cifrado en reposo, acceso por rol, redactado en logs |
| **Bajo** | Catálogo público, precios, tiendas | Público por diseño |

Regla operativa: **antes de crear un campo nuevo se clasifica**. El mapa de datos (`EN-1501`) es un
artefacto vivo, revisado al final de cada fase, no un documento de Sprint 0 que nadie vuelve a abrir.

---

## 3. Defensa contra fuerza bruta y credential stuffing (EN-1510)

El requisito que pediste explícitamente. La implementación es en capas, porque cada una sola es
evadible:

### Capa 1 · Retroceso exponencial por cuenta
```
intento 1  → 0 s
intento 2  → 1 s
intento 3  → 2 s
intento 4  → 4 s
intento 5  → 8 s
...
tope       → 30 s
```
Aplicado **por cuenta**, no por IP: un atacante con botnet rota IP, pero apunta a la misma cuenta.

### Capa 2 · Bloqueo temporal
10 fallos en 15 minutos ⇒ 15 minutos de bloqueo + notificación al titular con IP y hora + enlace de
recuperación segura. El bloqueo es **temporal por diseño**: un bloqueo permanente convierte el ataque
de fuerza bruta en un ataque de denegación de servicio contra usuarios legítimos.

### Capa 3 · Límite por IP y global
Ventana deslizante por IP y contador global. Un pico global anómalo activa modo defensivo:
desafío adicional para todos los intentos nuevos hasta que baje.

### Capa 4 · Detección de credential stuffing
Patrón característico: muchas IP × muchas cuentas × pocos intentos cada una. Ningún contador
individual se dispara. Se detecta por **tasa de fallo agregada**: si la proporción de accesos fallidos
supera el umbral histórico, se eleva la fricción globalmente.

### Capa 5 · Contraseñas comprometidas
En registro y cambio de contraseña, verificación contra listas de credenciales filtradas usando
**k-anonimato**: se envían los 5 primeros caracteres del hash SHA-1, nunca la contraseña ni su hash
completo.

### Capa 6 · Respuestas indistinguibles
"Usuario no existe" y "contraseña incorrecta" devuelven el mismo mensaje **y el mismo tiempo de
respuesta**. Lo segundo se logra ejecutando siempre el cálculo del hash, incluso cuando el usuario no
existe (contra un hash señuelo). Hay una prueba automatizada que compara distribuciones de tiempo.

### Capa 7 · Cobertura completa de endpoints
No solo el login. También: registro, recuperación de contraseña, reenvío de verificación, validación
de código MFA, canje de código de emparejamiento, invitación de usuario. Cada uno con su propio
límite. **Un endpoint de autenticación sin límite de tasa rompe el build** (prueba de CI).

### Capa 8 · Protección de los administradores
Los roles administrativos tienen umbrales más estrictos, MFA obligatorio y, opcionalmente, lista de
IP permitidas por tenant. Una cuenta administrativa comprometida expone el catálogo completo de una
empresa.

---

## 4. Autenticación y sesiones

| Control | Implementación |
|---|---|
| Contraseñas | Argon2id (m=64MB, t=3, p=4) o bcrypt cost ≥ 12. Nunca MD5/SHA sin sal, nunca reversible |
| Tokens de acceso | JWT de corta vida (≤ 15 min), firmados asimétricamente, con `tenant_scope` y `roles` |
| Refresh tokens | Opacos, almacenados como hash, **rotativos**, agrupados en familia |
| Detección de robo de token | Reuso de un refresh ya rotado ⇒ invalidar toda la familia + alertar (EN-1512 CA-4) |
| MFA | TOTP (RFC 6238) o WebAuthn. Obligatorio en roles administrativos (EN-1511) |
| Step-up | Reconfirmación de MFA para operaciones de alto impacto |
| Recuperación | Enlace de un solo uso, expiración 30 min, invalida sesiones al usarse |
| Atestación de cliente | App Check / Play Integrity en endpoints sensibles (EN-1502) |
| Certificate pinning | En la app móvil, con pin de respaldo y plan de rotación documentado |

---

## 5. Autorización

Ver la matriz completa en [03-actores-roles-permisos.md](03-actores-roles-permisos.md). Principios:

1. **Se decide en el servidor, siempre.** La guarda del cliente es usabilidad.
2. **Falla cerrado.** Sin contexto de tenant, cero filas. Sin rol, `404`.
3. **`404` en lugar de `403`** para recursos de otro tenant: un `403` confirma existencia.
4. **Permiso mínimo por servicio.** Cada componente con su propia identidad y sus permisos exactos.
5. **Verificado por prueba, no por revisión.** La suite negativa de aislamiento es obligatoria.

---

## 6. Cifrado

### En tránsito
TLS 1.2+ obligatorio, HSTS con `preload`, sin suites débiles. Escaneo TLS como gate de CI.

### En reposo
Tres niveles, no uno:

```
Nivel 1 · Disco       Cifrado gestionado de base de datos, backups y object storage.
                      Protege contra robo del medio físico.

Nivel 2 · Aplicación  Campos sensibles (nivel Medio) cifrados con clave por tenant
                      desde la bóveda. Protege contra un volcado de la base.

Nivel 3 · Extremo     Perfil corporal, avatar y fotos cifrados con clave derivada
          a extremo   del usuario. Protege contra TODO lo anterior más un
                      administrador malicioso o una orden mal fundada.
```

### Cifrado de extremo a extremo del perfil corporal (EN-0207)

```
Contraseña del usuario ─┐
                        ├─► Argon2id (sal por usuario, parámetros públicos)
Sal del servidor ───────┘        │
                                 ▼
                          Clave maestra (nunca sale del dispositivo)
                                 │
                     ┌───────────┴───────────┐
                     ▼                       ▼
          Clave de cifrado del perfil   Frase de recuperación
          (AES-256-GCM)                 (12 palabras, BIP-39)
                     │
                     ▼
          Blob cifrado ──► servidor (almacena, no descifra)
```

Consecuencias que hay que aceptar conscientemente:

- **Perder la contraseña y la frase de recuperación significa perder el perfil.** No hay recuperación
  del lado del servidor, porque si la hubiera el servidor podría descifrar. Se comunica de forma
  explícita al activar la sincronización (US-0208 CA-1 y CA-4).
- **El cambio de contraseña re-cifra el blob en el dispositivo**, no en el servidor.
- **Las medidas no pueden usarse para analítica individual en el servidor.** Sí para agregados
  calculados **en el dispositivo** y enviados ya agregados. Esto restringe qué se puede analizar, y es
  un costo deliberado.

### Claves
Bóveda gestionada (KMS / Secret Manager). Nunca en el código, en variables de entorno en texto plano
ni en la base de datos. Rotación máxima 12 meses o inmediata ante incidente. Todo acceso a la bóveda
queda auditado.

---

## 7. Privacidad desde el diseño

### Consentimiento (RN-006, RN-007, RN-021)

| Finalidad | Obligatorio para | Revocable | Efecto de revocar |
|---|---|---|---|
| Imagen corporal | Captura de foto y cámara | Sí | Se borra la imagen y la silueta derivada |
| Medida corporal | Perfil y recomendación de talla | Sí | Se borra el perfil corporal y el avatar |
| Imagen facial | Rostro del avatar | Sí | Se elimina el rostro de todos los activos ≤ 72 h |
| Compartir activos | Compartir una prueba | Sí | Se revocan los enlaces compartidos |
| Marketing | Nada del núcleo | Sí | Cesan las comunicaciones |
| Analítica opcional | Nada del núcleo | Sí | Cesa la telemetría no esencial |

Cada consentimiento guarda: finalidad, versión de la política, texto exacto mostrado, fecha, IP y
dispositivo. Sin esa evidencia, el consentimiento no es demostrable y por tanto no existe.

**Ninguna finalidad opcional bloquea el núcleo del producto** (RN-007). Rechazar marketing y analítica
opcional deja el probador y la talla funcionando completos.

### Minimización
- La foto de captura se procesa y se descarta; lo que persiste es la silueta y las medidas, cifradas.
- El servidor **no recibe** fotos en el flujo base (RN-014), verificable con un capturador de tráfico.
- Los logs no llevan datos personales; hay redacción automática y una prueba que lo verifica.
- La analítica usa seudónimos rotativos con sal por tenant.

### Retención

| Dato | Retención | Mecanismo |
|---|---|---|
| Foto de captura temporal | ≤ 24 h | Job diario + detección de huérfanos (EN-1505) |
| Perfil corporal | Mientras exista la cuenta | Borrado con la cuenta |
| Prueba compartida | Vigencia definida por el usuario, máx. 90 días | Expiración del enlace |
| Eventos de analítica | 24 meses, seudonimizados | Purga programada |
| Auditoría | ≥ 12 meses, luego archivo frío | Partición + archivado |
| Sesión de kiosco | Duración de la sesión | Borrado verificable con evidencia (EN-1404) |
| Datos de tenant purgado | Solo facturación y auditoría exigidas por ley | US-1702 |

### Derechos del titular

| Derecho | Historia | Plazo |
|---|---|---|
| Consulta | US-1514 | 10 días hábiles |
| Rectificación | US-0305 (corregir medidas) | Inmediato |
| Revocación | US-0106 | Inmediato, efectos ≤ 72 h |
| Supresión | US-0106 | ≤ 15 días hábiles, con certificado |
| Portabilidad | US-1514 | ≤ 15 días hábiles |

### Menores (D-08, RN-022)
Los flujos corporales están **bloqueados para menores de 18 años** hasta contar con análisis jurídico
y controles específicos. No es una restricción técnica que se pueda relajar con un toggle: está en la
capa de dominio.

---

## 8. Seguridad de la aplicación

| Control | Aplicación |
|---|---|
| Validación de entrada | En el borde del dominio, con lista permitida. Nunca confiar en el cliente |
| Salida | Codificación contextual; CSP estricta en web; sin `dangerouslySetInnerHTML` |
| Inyección SQL | Consultas parametrizadas siempre; sin concatenación |
| SSRF | Lista permitida de destinos salientes; sin peticiones a URL provistas por el usuario |
| Carga de archivos | Tipo verificado por contenido (no por extensión), tamaño limitado, reescritura de imágenes, escaneo antimalware, almacenamiento fuera de la raíz web |
| Deserialización | Sin deserialización de tipos polimórficos desde entrada externa |
| Dependencias | Dependabot + escaneo OWASP; crítico/alto bloquea el merge (AUT-10) |
| Secretos | Gitleaks en cada PR; rotación inmediata ante exposición |
| Cabeceras | HSTS, CSP, X-Content-Type-Options, Referrer-Policy, Permissions-Policy |
| CORS | Lista explícita de orígenes; sin comodín con credenciales |
| Móvil | Sin datos sensibles en `SharedPreferences` sin cifrar, sin logs en release, ofuscación (R8), detección de root/depuración |

---

## 9. Auditoría (EP-18)

Detalle completo en [08-historias-nuevas.md](08-historias-nuevas.md#ep-18--auditoría-y-trazabilidad-de-acciones)
y el esquema en [11-modelo-de-datos.md § 9](11-modelo-de-datos.md).

Lo esencial:

1. **Toda acción administrativa se registra**, incluidas las denegadas.
2. **Append-only real**: reglas y permisos de base de datos, no solo convención.
3. **Integridad encadenada**: cada registro incluye el hash del anterior; el hash raíz diario se
   sella en almacenamiento inmutable.
4. **Si la auditoría falla en una acción crítica, la acción se revierte.** No hay excepción.
5. **Sin datos corporales en el registro**: referencias y hashes, nunca contenido (RN-033).
6. **Consultable por el tenant** (lo suyo) y por la plataforma (todo), con la consulta misma auditada.

---

## 10. Operación segura

| Práctica | Cadencia |
|---|---|
| Revisión del mapa de datos y del modelo de amenazas | Al cierre de cada fase |
| Revisión de accesos y roles | Trimestral, con revocación de inactivos > 90 días |
| Escaneo de dependencias y secretos | Cada PR + diario |
| Pentest externo | Antes de producción SaaS (EN-1507) y anual |
| Simulacro de restauración | Semestral, con acta (EN-1508) |
| Rotación de claves | ≤ 12 meses o ante incidente |
| Ejercicio de respuesta a incidentes | Anual |

### Respuesta a incidentes

```
1. Detectar   alerta automática o reporte
2. Contener   revocar sesiones/claves, aislar componente, activar kill switch
3. Evaluar    alcance con el log de auditoría y las trazas
4. Notificar  titulares y autoridad según plazo legal;
              cliente afectado (tenant) siempre
5. Erradicar  causa raíz, no síntoma
6. Recuperar  restaurar con evidencia de integridad
7. Aprender   post-mortem sin culpables, con acciones y responsables
```

El plazo de notificación y el responsable de comunicarlo se definen **antes** del lanzamiento, no
durante el incidente.
