# ADR-0008 · Auditoría append-only como aspecto transversal

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + Seguridad

## Contexto

Pediste explícitamente *"auditorías para las funciones para los roles creados de trabajo dentro de
ella"*. En el `Backlog_Ejecutable` la auditoría aparece únicamente como una frase dentro de `RNF-04`
("auditoría de accesos"): sin historia, sin modelo de datos, sin consulta, sin retención, sin dueño.

En un SaaS multi-tenant la auditoría no es un registro más: es lo que permite responder *"quién borró
ese producto"*, *"quién cambió ese precio"*, *"quién exportó esa base de clientes"*. Sin ella, ante un
incidente con un cliente empresarial no hay nada que mostrar.

El error habitual es implementarla con llamadas dispersas en los controladores. Resultado predecible:
cobertura parcial, formato inconsistente, y funciones nuevas que nadie recuerda auditar.

## Decisión

**Un servicio de auditoría transversal, aplicado como aspecto sobre la capa de aplicación, con
almacenamiento append-only real y verificación de integridad encadenada.**

### 1. Aspecto, no llamadas dispersas

```kotlin
@Auditable(action = "product.publish", resource = ResourceType.PRODUCT)
fun publishProduct(command: PublishProductCommand): Result<Product>
```

El interceptor construye el registro: actor, rol, tenant, ámbito, recurso, IP, agente, `request_id`,
estado anterior y posterior, resultado. Un caso de uso administrativo **sin** `@Auditable` es
detectado por una prueba de ArchUnit que falla el build.

### 2. Se registran también las acciones denegadas

Un intento fallido de acceder al recurso de otro tenant es más interesante que un éxito rutinario.
`result` ∈ `SUCCESS | DENIED | FAILED`.

### 3. Si la auditoría falla en una acción crítica, la acción se revierte

Para cambio de rol, cambio de configuración de seguridad, impersonación, borrado y exportación
masiva, la escritura del registro es **transaccional**. No hay acción crítica sin rastro.
Para el resto, escritura asíncrona por patrón outbox: no se pierde el registro, pero no se penaliza
la latencia.

### 4. Append-only real, en la base de datos

```sql
REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM PUBLIC;
CREATE RULE audit_log_no_update AS ON UPDATE TO audit_log DO INSTEAD NOTHING;
CREATE RULE audit_log_no_delete AS ON DELETE TO audit_log DO INSTEAD NOTHING;
```

Permisos y reglas, no convención. Ninguna identidad de aplicación tiene permiso de borrado sobre esta
tabla (RN-032).

### 5. Integridad encadenada

Cada registro incluye el hash del anterior (cadena por tenant). Alterar o eliminar un registro rompe
la cadena. El hash raíz de cada día se sella en almacenamiento con retención inmutable. Una
verificación diaria (`AUT-24`) emite informe, y una cadena rota genera alerta crítica.

### 6. Sin datos sensibles

`before_state` y `after_state` se redactan: los campos clasificados como sensibles se sustituyen por
referencias y hashes, nunca por el contenido (RN-033). Los datos corporales nunca aparecen, ni
siquiera cifrados.

### 7. Particionado mensual

Consultar 12 meses debe seguir siendo rápido (p95 ≤ 2 s). Archivar es soltar una partición, no borrar
millones de filas.

### 8. Consultable por quien corresponde

- El tenant ve **lo suyo** (`US-1802`).
- La plataforma ve **todo** (`US-1803`).
- **La consulta y la exportación quedan auditadas.**

## Consecuencias

**Positivas**
- Cobertura completa por construcción: un caso de uso nuevo sin anotación rompe el build.
- Formato uniforme, consultable y exportable.
- Evidencia verificable ante un cliente, una auditoría o un incidente.
- Base para la detección de anomalías de seguridad (`US-1803` CA-5).
- Cumple el requisito de no repudio del modelo de amenazas (doc 12 § 1).

**Negativas**
- Volumen: en un SaaS activo el log crece rápido. Se controla con particionado, retención y archivado
  frío.
- Latencia: la escritura transaccional en acciones críticas añade unos milisegundos. Es el precio
  correcto para esas acciones concretas.
- El estado anterior/posterior en `jsonb` puede ser pesado. Se limita a los campos que cambiaron, no
  a la entidad completa.
- Complejidad adicional: 26 SP (`EN-1801`, `US-1802`, `US-1803`, `EN-1804`).

## Momento de implementación

**Sprint 7, antes del portal administrativo**, no después.

`US-1101`–`US-1104` crean las acciones administrativas. Si la auditoría llega después, hay que volver
a pasar por todos los casos de uso, y en la práctica algunos se quedan sin cubrir. Es más barato y más
seguro que el aspecto exista antes que las acciones que debe registrar.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Llamadas explícitas en cada controlador | Cobertura parcial garantizada; formato inconsistente; imposible de verificar |
| Solo logs de aplicación | Los logs se rotan, se pierden y no son consultables por el cliente. No sirven como evidencia |
| Triggers de base de datos | Capturan el cambio de datos pero no el **actor**, el **motivo** ni el **contexto** de negocio. Son un complemento útil, no un sustituto |
| Servicio externo de auditoría | Dependencia y costo en Fase 1; el volumen aún no lo justifica |
| Event sourcing como auditoría | Resuelve trazabilidad pero cambia todo el modelo de persistencia. Demasiado para el problema que se resuelve aquí |
