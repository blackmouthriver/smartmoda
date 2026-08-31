# 01 · Contexto y visión

## 1. El problema

Comprar ropa sin probársela tiene tres costos que hoy nadie resuelve bien:

| Actor | Dolor | Evidencia observable |
|---|---|---|
| Persona | No sabe si le queda; probarse en tienda es lento e incómodo; devolver en online es fricción | Tasa de devolución por talla en moda online: 20–40% del ticket |
| Tienda / marca | Devoluciones destruyen margen; no sabe *qué se probó y no se compró* | Solo mide ventas, no intención |
| Tienda física | El vestier es cuello de botella y no da datos | Filas, prendas descartadas sin trazabilidad |

**Hipótesis central del producto:** si la persona puede ver la prenda sobre su cuerpo (o sobre un avatar
fiel a su cuerpo) y recibe una talla explicada, aumenta la conversión y baja la devolución. Y el
*subproducto* — saber qué se prueba y no se compra — es un dato que hoy no existe y que las marcas pagarían.

## 2. Propuesta de valor

### Para la persona (B2C, app gratuita)
- Mide su cuerpo **manual** (siempre disponible) o **automáticamente** por foto/cámara.
- Obtiene un **avatar** que se parece a su cuerpo, opcionalmente con su rostro.
- Prueba prendas del catálogo: primero sobre avatar, luego sobre su foto, luego en **tiempo real por cámara**.
- Recibe **talla recomendada con explicación y nivel de confianza**, no un número mágico.
- Un **Stylist IA** le arma looks según ocasión, clima, presupuesto, su estilo y **el stock real**.
- Un **asistente de localización** le dice en qué tienda, sucursal, piso y sección está la prenda.

> **Alcance del avatar (confirmado 31-ago-2026):** el avatar, las medidas y el perfil corporal son
> **exclusivamente del canal móvil**. La web es administrativa, no probador de consumidor.

### Para el comercio (B2B, SaaS)
- Portal web + app de administración: catálogo, variantes, activos, tallas, precios, stock por sucursal.
- **Marca propia** dentro de la app (colores, tipografía, logo, animaciones) sin tocar código.
- **Analítica predictiva**: embudo vista → prueba → compra, prendas más probadas, curva de tallas faltante,
  predicción de demanda, alertas de anomalía (mucha prueba, poca compra ⇒ problema de tallaje o de foto).
- **Espejo inteligente** en tienda física: mide automáticamente, muestra la prenda en tiempo real sobre
  el cliente y permite elegir tocando la pantalla — **sin registrar a nadie**. Lo que recoge alimenta
  de forma anónima la predicción de demanda y el contraste entre lo que se prueba y lo que se compra.
- Integración con su ERP/POS existente, o carga manual/masiva si no tiene sistema.

### Para el operador de la plataforma (super administración)
- Crear, aprovisionar, suspender y facturar empresas.
- Definir qué funcionalidades ve cada tenant (feature catalog comercial).
- Controlar consumo de IA, almacenamiento y costo variable por tenant.
- Auditoría global e impersonación segura para soporte.

## 3. Alcance por fases

### Fase 1 — MVP consumidor (Sprints 0–5, ~12 semanas) · **inversión objetivo: $0**
- Onboarding, registro, consentimiento versionado.
- **Medidas manuales** + validación + conversión de unidades.
- Catálogo con búsqueda, detalle, variantes, stock por tienda, favoritos.
- **Recomendación de talla explicable** contra tabla por marca.
- Captura guiada + detección de pose on-device + **overlay 2D** sobre la foto.
- Mi Closet, accesibilidad, observabilidad, distribución beta.
- Portal de administración **mínimo** (carga de catálogo) — se adelanta desde Sprint 7 propuesto.

**Explícitamente fuera:** AR real, física de tela, pago integrado, avatar 3D, body scan.

### Fase 2 — AR + backend propio (Sprints 6–13)
- API Spring Boot + PostgreSQL + contrato OpenAPI, migración desde BaaS.
- Portal administrativo completo (CRUD prendas, activos, tallas, inventario, publicación).
- Pipeline glTF/GLB, avatar 3D paramétrico, **AR sobre el cuerpo** con fallback 2D garantizado.
- Estimación automática de medidas (etiqueta *beta*, siempre corregible).
- Conversión omnicanal: carrito, reserva, deep link a checkout del retailer.
- Piloto controlado en una tienda real.

### Fase 3 — SaaS + IA (Sprints 14–23)
- Stylist IA contextual, ranking híbrido, evaluación continua de recomendaciones.
- Pago integrado, órdenes, devoluciones.
- **Núcleo multi-tenant completo**: roles, aislamiento verificado, marca por tienda, planes y cuotas.
- Analítica B2B, mapas de talla, exportación.
- Integración ERP con webhooks idempotentes y reconciliación.
- Espejo inteligente y modo kiosco.
- I+D: body scan avanzado y física de tela, **con gate go/no-go basado en benchmark**.
- Pentest, recuperación, auditoría de privacidad, SLA, lanzamiento.

## 4. Modelo de negocio (hipótesis)

| Fuente | Descripción | Fase |
|---|---|---|
| Suscripción SaaS por tenant | Plan por # de SKU activos, sucursales y pruebas/mes | 3 |
| Consumo IA | Cuota incluida + excedente por prueba/consulta al Stylist | 3 |
| Espejo inteligente | Licencia por pantalla instalada | 3 |
| Datos agregados anonimizados | Informes de tendencia y ajuste por categoría/región | 3+ |
| App de consumidor | Gratuita siempre. Cupones y suscripción premium opcional | 3 |

## 5. Métricas de éxito (North Star + soporte)

- **North Star:** *pruebas virtuales completadas que terminan en acción comercial* (carrito, reserva, visita).
- Contraste probado-vs-comprado disponible por tienda con espejo instalado (anónimo, agregado).
- Conversión registro → consentimiento ≥ 70%
- Perfil corporal completo ≥ 80% de usuarios activados
- Confirmación de talla recomendada ≥ 75% en piloto
- Prueba virtual completada ≥ 85% de intentos iniciados
- CTR probador → acción comercial ≥ 15%
- Reducción de devolución por talla en tenant piloto ≥ 25% vs baseline
- Crash-free sessions ≥ 99,8% en producción
- 0 accesos cruzados entre tenants (verificado por prueba negativa automática)

## 6. Principios de producto

1. **Nunca prometer lo que no está validado.** Un overlay 2D no es una garantía de caída de tela.
   Cada visualización lleva su etiqueta de confianza.
2. **La medida que el usuario corrige, gana.** La estimación automática es una sugerencia.
3. **Consentimiento antes de cámara.** Ninguna captura ocurre sin consentimiento específico y vigente.
4. **Degradar, nunca romper.** Sin AR → 2D. Sin tabla de tallas → se informa, no se inventa.
5. **La UI no compite con la ropa.** 90% neutro, 7% acento, 3% semántico (ver doc 16).
6. **El inventario es la fuente de verdad de la IA.** No se recomienda lo que no existe.
