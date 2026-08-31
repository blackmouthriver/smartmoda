# Decisiones de arquitectura (ADR)

Un ADR registra una decisión con su **contexto**, sus **alternativas** y sus **consecuencias** —
incluidas las negativas. No es documentación de lo que se hizo: es el razonamiento de por qué,
para que dentro de un año se pueda revisar con la información que había entonces.

**Un ADR no se edita cuando la decisión cambia.** Se escribe uno nuevo que lo reemplaza, y el anterior
pasa a estado `Reemplazada por ADR-XXXX`. El historial es el valor.

## Índice

| ID | Decisión | Estado | Impacto |
|---|---|---|---|
| [0001](ADR-0001-monolito-modular-vs-microservicios.md) | Monolito modular antes que microservicios | **Rechazada** — reemplazada por 0012 | — |
| [0002](ADR-0002-estrategia-multitenant.md) | Multi-tenancy desde el Sprint 0 | Propuesta | **Crítico · irreversible si se pospone** |
| [0003](ADR-0003-almacenamiento-perfil-corporal.md) | Perfil corporal local cifrado, sincronización E2E opcional | **Revisada 31-ago** · coincide con tu planteamiento | **Crítico · regulatorio** |
| [0004](ADR-0004-backend-fase-1.md) | Supabase detrás de una capa Repository en Fase 1 | Propuesta | Alto · costo de migración a Fase 2 |
| [0005](ADR-0005-canales-y-plataformas.md) | Cuatro canales con base compartida | **Revisada 31-ago** | Alto · alcance y calendario |
| [0006](ADR-0006-probador-progresivo.md) | Probador progresivo 2D → AR con fallback | Aceptada (formaliza `D-05`, `D-10`) | **Crítico · viabilidad de la Fase 2** |
| [0007](ADR-0007-recomendacion-de-talla-determinista.md) | Recomendación de talla determinista, no ML | Propuesta | Alto · promesa central del producto |
| [0008](ADR-0008-auditoria-append-only.md) | Auditoría append-only como aspecto transversal | Propuesta | Alto · requisito explícito, difícil de añadir tarde |
| [0009](ADR-0009-gateway-ia-agnostico.md) | Gateway de IA agnóstico con validación determinista | Aceptada (formaliza `D-09`) | Medio · costo y calidad de IA |
| [0010](ADR-0010-nombre-y-marca.md) | Nombre del producto y de la empresa | **Pendiente — bloquea el Sprint 1** | **Crítico · irreversible tras publicar** |
| [0011](ADR-0011-espejo-inteligente-anonimo.md) | Espejo inteligente: medición automática anónima y analítica sin perfil | Propuesta | Alto · canal nuevo, riesgo regulatorio en espacio físico |
| [0012](ADR-0012-microservicios-acotados.md) | **Microservicios acotados: máximo 5, aparición diferida** | **Aceptada por el sponsor** | **Alto · toda la arquitectura y el calendario** |
| [0013](ADR-0013-menores-y-ropa-intima.md) | Menores de edad y ropa íntima: qué entra y qué no | Propuesta · **requiere validación jurídica** | **Crítico · regulatorio** |
| [0014](ADR-0014-monetizacion-publicidad.md) | Publicidad contextual + licencia por negocio | Propuesta | Alto · modelo de ingresos y postura de privacidad |

## Estado tras las respuestas del 31-ago-2026

**Resueltas:** 0001 (rechazada → 0012), 0003 (perfil solo local), 0005 (canales), 0010 (nombre de
trabajo `SmartModa`), 0011 (las cuatro preguntas del espejo), 0012, 0014.

**Lo único que queda bloqueando algo:**

1. **ADR-0013** — menores y ropa íntima. Requiere **concepto jurídico escrito** antes de desplegar el
   espejo en una tienda familiar. Bloquea la Fase 3, no la Fase 1.
2. **ADR-0010** — búsqueda marcaria de `SmartModa`. Bloquea **publicar**, no desarrollar.

## Decisiones del `Backlog_Ejecutable` que siguen vigentes

`D-03` (referencia AI Mirror: tomar patrones, no copiar activos ni código), `D-04` (Android nativo),
`D-05` (alcance del MVP), `D-06` (backend por fases — refinada por ADR-0004), `D-07` (biometría como
dato de alto impacto), `D-09` (gateway de IA — formalizada en ADR-0009), `D-10` (3D/AR con gate —
formalizada en ADR-0006).

Pendientes: `D-01`, `D-02` (ADR-0010), `D-08` (menores de edad), `D-11` (línea base temporal),
`D-12` (capacidad del equipo — ver [20-preguntas-abiertas.md](../20-preguntas-abiertas.md)).
