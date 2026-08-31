# 03 · Actores, roles y permisos

## 1. Actores

| Actor | Tipo | Canal principal | Canal alterno |
|---|---|---|---|
| Persona consumidora | Humano externo | App móvil | Web pública (probador) |
| Administrador de tienda | Humano B2B | Web administrativa | App móvil de administración |
| Dueño de empresa | Humano B2B | Web administrativa | — |
| Super administrador de plataforma | Humano interno | Web backoffice | — |
| Soporte de plataforma | Humano interno | Web backoffice (lectura + impersonación) | — |
| Espejo inteligente | Dispositivo | Kiosco Android/Web | — |
| ERP / POS del comercio | Sistema | API + webhooks | Importación CSV |
| Pasarela de pago | Sistema | Webhooks | — |
| Proveedor de IA | Sistema | Gateway propio | — |

## 2. Modelo de autorización

**RBAC con ámbito jerárquico (scoped RBAC), no ABAC completo.** Razón: ABAC es más expresivo pero
mucho más difícil de auditar, y la auditoría es un requisito explícito del proyecto.

```
Plataforma
└── Tenant (empresa)
    └── Marca
        └── Tienda / Sucursal
            └── Sección
```

Un *role assignment* es la tupla:

```
(user_id, role, scope_type, scope_id, granted_by, granted_at, expires_at?)
```

Reglas duras:

- **RN-009** El `tenant_id` efectivo se deriva **siempre** del token del servidor. Un `tenant_id`
  enviado por el cliente se ignora o produce `403`. Nunca se confía en el cliente.
- Un permiso en un ámbito superior **hereda** hacia abajo (TENANT_ADMIN puede lo de STORE_MANAGER en
  todas sus tiendas), nunca al revés.
- Un usuario puede tener **varios** role assignments; el permiso efectivo es la unión.
- Los roles de plataforma (`PLATFORM_*`) **no** pertenecen a ningún tenant y exigen **MFA obligatorio**.
- Toda acción de un rol administrativo genera **registro de auditoría inmutable** (EN-1801).

## 3. Matriz de permisos

Leyenda: **C**rear · **L**eer · **A**ctualizar · **B**orrar · **—** sin acceso · **P**ropio únicamente

| Recurso | SUPER_ADMIN | SUPPORT | TENANT_OWNER | TENANT_ADMIN | BRAND_MANAGER | STORE_MANAGER | CATALOG_EDITOR | INVENTORY_OP | ANALYST | END_USER |
|---|---|---|---|---|---|---|---|---|---|---|
| Tenant (crear/suspender) | CLAB | L | — | — | — | — | — | — | — | — |
| Plan y facturación del tenant | CLAB | L | CLA | L | — | — | — | — | — | — |
| Configuración del tenant | CLAB | L | CLA | CLA | — | — | — | — | — | — |
| Tema visual / marca (colores, tipografía, logo) | CLAB | L | CLA | CLA | LA | — | — | — | — | — |
| Feature flags comerciales | CLAB | L | L | L | — | — | — | — | — | — |
| Usuarios y roles del tenant | CLAB | L | CLAB | CLA | — | — | — | — | — | — |
| Marcas | CLAB | L | CLAB | CLAB | LA | L | L | L | L | — |
| Tiendas / sucursales | CLAB | L | CLAB | CLAB | L | LA | L | L | L | — |
| Secciones y ubicaciones | CLAB | L | CLAB | CLAB | CLAB | CLAB | L | LA | L | — |
| Productos y variantes | CLAB | L | CLAB | CLAB | CLAB | L | CLAB | L | L | L (público) |
| Activos 2D/3D | CLAB | L | CLAB | CLAB | CLAB | L | CLAB | — | L | L (público) |
| Tablas de tallas | CLAB | L | CLAB | CLAB | CLAB | L | CLA | — | L | L (público) |
| Precios | CLAB | L | CLAB | CLAB | CLA | L | L | — | L | L (público) |
| Stock por tienda | CLAB | L | CLAB | CLAB | L | CLA | L | CLA | L | L (público) |
| Órdenes y reservas | L | L | CLAB | CLAB | L | CLA | — | L | L | CLA (P) |
| Analítica del tenant | L | L | L | L | L (su marca) | L (su tienda) | — | — | L | — |
| Analítica global de plataforma | L | L | — | — | — | — | — | — | — | — |
| Log de auditoría del tenant | L | L | L | L | — | — | — | — | — | — |
| Log de auditoría global | L | L | — | — | — | — | — | — | — | — |
| Impersonación ("ver como") | C (auditada) | C (auditada) | — | — | — | — | — | — | — | — |
| Perfil corporal | — | — | — | — | — | — | — | — | — | CLAB (P) |
| Avatar | — | — | — | — | — | — | — | — | — | CLAB (P) |
| Favoritos / closet / historial | — | — | — | — | — | — | — | — | — | CLAB (P) |
| Datos personales de usuarios finales | — | — | — | — | — | — | — | — | — | CLAB (P) |

> **Nota crítica:** ningún rol administrativo, incluido `PLATFORM_SUPER_ADMIN`, puede leer el
> **perfil corporal, fotos o avatar** de un usuario final. Son datos cifrados de extremo a extremo
> ([ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md)). La analítica solo ve agregados y
> segmentos, nunca medidas individuales identificables.

## 4. Reglas de impersonación (US-1707)

La impersonación es la vía más común de fuga de confianza en un SaaS. Reglas:

1. Solo `PLATFORM_SUPER_ADMIN` y `PLATFORM_SUPPORT`.
2. Requiere **motivo escrito** y **ticket asociado**.
3. Requiere **re-autenticación con MFA** en el momento (no basta la sesión abierta).
4. La sesión impersonada es de **solo lectura por defecto**; escritura requiere aprobación de un
   segundo super admin (*four-eyes*).
5. Duración máxima 30 minutos, no renovable sin nuevo motivo.
6. Banner permanente y no ocultable en la UI: *"Sesión impersonada por <usuario> · motivo <x>"*.
7. **Nunca** permite acceso a datos cifrados de usuario final (los E2E siguen ilegibles).
8. Se notifica por correo al `TENANT_OWNER` dentro de los 5 minutos siguientes.
9. Todo queda en el log de auditoría inmutable.

## 5. Requisitos de autenticación por rol

| Rol | Método | MFA | Duración de sesión | Rotación |
|---|---|---|---|---|
| `END_USER` | Correo+clave, Google, Apple | Opcional | 30 días (refresh) | Refresh rotativo |
| `ANALYST`, `INVENTORY_OP`, `CATALOG_EDITOR` | Correo+clave o SSO del tenant | Recomendado | 12 h | Refresh rotativo |
| `STORE_MANAGER`, `BRAND_MANAGER`, `TENANT_ADMIN` | Correo+clave o SSO | **Obligatorio** | 8 h | Refresh rotativo |
| `TENANT_OWNER` | Correo+clave o SSO | **Obligatorio** | 8 h | Refresh rotativo |
| `PLATFORM_*` | SSO interno + clave hardware/TOTP | **Obligatorio + step-up** | 4 h | Sin refresh, re-login |
| `KIOSK_DEVICE` | Credencial de dispositivo + atestación | N/A | Sesión de tienda | Rotación diaria |

Defensa contra fuerza bruta y credential stuffing: ver
[12-seguridad-y-privacidad.md § 3](12-seguridad-y-privacidad.md) y `EN-1510`.
