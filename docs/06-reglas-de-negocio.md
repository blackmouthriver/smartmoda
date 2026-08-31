# 06 · Reglas de negocio

Una regla de negocio es un invariante del dominio: se cumple siempre, en todos los canales
(app, web, kiosco, API pública), y **cada una debe existir como prueba automatizada**.

Las reglas RN-001 a RN-015 vienen del `Backlog_Ejecutable` y se conservan textualmente.
Las RN-016 en adelante son nuevas, derivadas del análisis de las brechas.

---

## Talla y ajuste

| ID | Regla | Prueba |
|---|---|---|
| **RN-001** | Una recomendación de talla siempre se calcula contra una tabla versionada por marca/categoría; si no existe, se informa sin inventar. | Unitaria: sin tabla ⇒ `SizeRecommendation.Unavailable`, nunca un valor por defecto |
| **RN-002** | El usuario puede estar entre dos tallas; la preferencia de ajuste influye en la explicación, **no** modifica sus medidas. | Unitaria: mismo perfil con ajuste ceñido/holgado ⇒ mismas medidas persistidas, distinta explicación |
| **RN-005** | Las medidas manuales corregidas por el usuario prevalecen sobre las estimaciones automáticas. | Unitaria: `source = MANUAL` gana sobre `source = ESTIMATED` en el mismo campo |
| **RN-016** | Una talla recomendada nunca se muestra sin nivel de confianza asociado. | Contrato: el DTO no permite `confidence = null` |
| **RN-017** | Si la confianza es *baja*, se ofrece una alternativa segura (talla adyacente) y se indica por qué. | Unitaria + prueba de UI |

## Visualización y probador

| ID | Regla | Prueba |
|---|---|---|
| **RN-004** | Una visualización 2D/AR nunca se presenta como garantía de caída, comodidad o ajuste físico si no existe evidencia validada. | Prueba de UI: aviso de no-garantía presente en toda vista de probador |
| **RN-018** | Ninguna prenda entra al modo AR sin perfil de activo validado y prueba de compatibilidad. | Integración: activo sin perfil ⇒ AR deshabilitado, fallback 2D |
| **RN-019** | Todo modo avanzado (AR, estimación automática, física de tela) tiene un fallback funcional garantizado y probado. | Test de degradación forzada |

## Datos personales y consentimiento

| ID | Regla | Prueba |
|---|---|---|
| **RN-006** | No se captura imagen, pose, medidas ni avatar antes del consentimiento específico y vigente. | Integración: sin consentimiento ⇒ la cámara no se abre; el endpoint devuelve `403` |
| **RN-007** | Marketing, personalización avanzada y analítica opcional deben poder rechazarse sin bloquear el núcleo contratado. | Flujo: rechazar todo lo opcional ⇒ probador y talla siguen funcionando |
| **RN-008** | Los activos compartidos no contienen medidas, ubicación, identificadores internos ni metadatos sensibles. | Unitaria: sanitizador EXIF + inspección del binario resultante |
| **RN-014** | Las imágenes corporales se procesan on-device cuando sea viable; cualquier envío al servidor requiere finalidad y consentimiento explícitos. | Prueba de red: 0 peticiones salientes con la imagen en el flujo base |
| **RN-020** | El perfil corporal cifrado E2E no es legible por ningún rol de la plataforma, incluido el super administrador. | Prueba negativa: el super admin recupera el blob y no puede descifrarlo |
| **RN-021** | Revocar un consentimiento borra o anonimiza los datos derivados de esa finalidad en ≤ 72 h. | Job + prueba de integración con reloj simulado |
| **RN-022** | Los flujos corporales están bloqueados para usuarios que declaran ser menores de 18 años. **Ampliada:** tampoco se activa cámara, medición ni probador visual para un perfil de allegado marcado como menor. | Flujo: perfil menor ⇒ ruta sin cámara ni medición automática (`US-0209` CA-2) |
| **RN-037** | La ropa íntima se visualiza únicamente sobre **avatar estilizado**, en el dispositivo personal de un usuario adulto. Nunca sobre foto, nunca sobre cámara en vivo, nunca en el espejo de tienda, nunca con un perfil de menor. | Unitaria: categoría con `requires_stylized_only` ⇒ los modos foto, cámara y espejo devuelven `NotAvailable` ([ADR-0013](adr/ADR-0013-menores-y-ropa-intima.md)) |
| **RN-038** | Ninguna decisión publicitaria puede tomarse a partir de medidas corporales, talla recomendada, silueta, avatar o imágenes. La segmentación es contextual y, como máximo, por preferencias declaradas con consentimiento revocable. | Prueba negativa por cada fuente prohibida; el motor publicitario no compila si accede a ellas ([ADR-0014](adr/ADR-0014-monetizacion-publicidad.md)) |
| **RN-039** | En el espejo de tienda nunca se solicitan datos de pago ni credenciales: el carrito termina en un código para la caja o en un QR hacia el móvil del cliente. | Prueba de flujo: no existe ninguna pantalla de pago en el canal kiosco (`US-1412` CA-4) |

## Inventario y comercio

| ID | Regla | Prueba |
|---|---|---|
| **RN-003** | Stock, precio y disponibilidad se revalidan antes de reservar, pagar o abrir un canal externo. | Integración: stock cambia entre selección y confirmación ⇒ se bloquea y se avisa |
| **RN-010** | Webhooks, pagos, reservas e integraciones se procesan de forma idempotente. | Contrato: mismo `idempotency_key` dos veces ⇒ un solo efecto |
| **RN-011** | Un outfit de compra solo incluye SKU disponibles; lo agotado se reemplaza o se marca explícitamente. | Integración: SKU agotado ⇒ sustitución o marca visible, nunca silencio |
| **RN-012** | La recomendación patrocinada o influenciada por regla comercial se identifica y nunca viola restricciones del usuario. | Unitaria: item patrocinado que viola restricción ⇒ excluido |
| **RN-023** | El precio mostrado al usuario es siempre el precio vigente del tenant para esa tienda y ese momento; nunca un precio cacheado sin marca de frescura. | Integración: precio con `staleness > umbral` ⇒ se revalida antes de mostrar |
| **RN-024** | Un producto no se publica sin: al menos un activo válido, una tabla de tallas aplicable y stock declarado (aunque sea cero). | Validación de publicación en el portal admin |

## Multi-tenancy

| ID | Regla | Prueba |
|---|---|---|
| **RN-009** | Cada operación multi-tenant requiere `tenant_id` derivado del contexto autenticado; **nunca** se confía en un tenant enviado libremente por el cliente. | **Suite de pruebas negativas obligatoria**: token de tenant A + recurso de tenant B ⇒ `404` (no `403`, para no filtrar existencia) |
| **RN-025** | Ninguna consulta a una tabla con `tenant_id` puede ejecutarse sin filtro de tenant. | Lint de repositorio + Row Level Security en PostgreSQL como red de seguridad |
| **RN-026** | La personalización visual de un tenant no puede producir combinaciones que incumplan contraste WCAG AA. | Validación automática en el editor de tema (US-1703) |
| **RN-027** | Cada marca del marketplace puede tener color propio dentro de su tienda, pero **navegación, carrito y pago conservan siempre la paleta base**. | Revisión de diseño + test visual |
| **RN-028** | Superar una cuota del plan degrada la funcionalidad afectada con aviso claro; nunca produce pérdida silenciosa de datos. | Test de cuota |

## Kiosco y tienda física

| ID | Regla | Prueba |
|---|---|---|
| **RN-013** | Las sesiones de kiosco son temporales, se borran al terminar y no restauran contenido del cliente anterior. | Integración: segunda sesión en el mismo dispositivo ⇒ estado vacío verificado |
| **RN-029** | El catálogo del kiosco se restringe al inventario en tiempo real de esa tienda. | Integración: SKU sin stock en esa tienda ⇒ no aparece o aparece marcado |
| **RN-030** | El borrado de sesión de kiosco produce evidencia auditable con marca de tiempo. | Registro de auditoría verificado |

## Trazabilidad y auditoría

| ID | Regla | Prueba |
|---|---|---|
| **RN-015** | Todo cambio de tabla de talla, prompt, modelo, regla, activo o algoritmo conserva versión para reproducir resultados. | Unitaria: entidad versionada; `UPDATE` crea versión, no sobrescribe |
| **RN-031** | Toda acción administrativa (crear, modificar, borrar, publicar, exportar, impersonar) genera un registro de auditoría inmutable. | Aspecto transversal + test: acción sin registro ⇒ falla la prueba |
| **RN-032** | El log de auditoría es **append-only**: ningún rol puede modificarlo ni borrarlo antes del vencimiento de retención. | Prueba negativa: `UPDATE`/`DELETE` sobre `audit_log` rechazado a nivel de base de datos |
| **RN-033** | Un registro de auditoría nunca contiene el contenido de datos corporales, solo referencias y metadatos. | Test de redacción |

## IA

| ID | Regla | Prueba |
|---|---|---|
| **RN-034** | El inventario es la fuente de verdad de la IA: el modelo propone candidatos, las reglas deterministas filtran. | Integración: SKU inexistente devuelto por el modelo ⇒ descartado antes de mostrar |
| **RN-035** | Ningún cambio de modelo o prompt llega a producción sin pasar la suite de evaluación (relevancia, restricciones, stock, seguridad, latencia, costo). | Gate de CI (AUT-12) |
| **RN-036** | La IA nunca afirma un ajuste físico ni una talla; sugiere y remite al motor de tallas determinista. | Suite de evaluación con casos adversos |

---

## Convención de implementación

Estas reglas viven en el **dominio**, no en la UI ni en el controlador:

```
domain/
  sizing/SizeRecommendationPolicy.kt      → RN-001, RN-002, RN-005, RN-016, RN-017
  consent/ConsentPolicy.kt                → RN-006, RN-007, RN-021, RN-022
  tenancy/TenantContextPolicy.kt          → RN-009, RN-025, RN-028
  commerce/AvailabilityPolicy.kt          → RN-003, RN-011, RN-023
  audit/AuditablePolicy.kt                → RN-031, RN-032, RN-033
```

Cada archivo de política lleva en su KDoc/Javadoc el ID de la regla que implementa, y cada regla tiene
un test cuyo nombre empieza por el ID (`RN_009_rejects_cross_tenant_access`). Así la trazabilidad
regla ⇄ código ⇄ prueba es verificable con un `grep`, no con buena voluntad.
