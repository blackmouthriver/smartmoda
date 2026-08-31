# ADR-0005 · Canales y plataformas

- **Estado:** Propuesta
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + PO
- **Refina:** `D-04` ("Android nativo con Kotlin y Jetpack Compose; iOS y web de consumidor quedan
  fuera del MVP")

## Contexto

`D-04` deja fuera del MVP la web de consumidor e iOS. Tu enunciado, en cambio, pide:

- App móvil para el consumidor.
- **Web** para super administración y administración de tiendas.
- **App de administración**, "por si no desean usar la web o por facilidad en el momento".
- Espejo inteligente en tienda, con cámara y pantalla táctil.

Son cuatro superficies. Construirlas todas en paralelo con un equipo pequeño no es viable; ignorar la
tensión tampoco.

## Decisión

Cuatro canales, con fases distintas y con una base compartida que evita construir cuatro productos.

| Canal | Tecnología | Fase | Audiencia |
|---|---|---|---|
| **App Android de consumidor** | Kotlin + Compose | 1 | Persona consumidora |
| **Web administrativa** | React + TypeScript, responsiva | 2 | Admin de tienda + super admin |
| **App Android de administración** | El mismo APK, módulo `feature/admin` | 2 | Admin de tienda en movimiento |
| **Espejo inteligente** | **Web en modo quiosco** + MediaPipe | 3 | Cliente **anónimo** en tienda física |

> **Revisión del 31-ago-2026.** El alcance confirmado por el usuario elimina el *probador web de
> consumidor*: el avatar y las medidas son exclusivamente del canal móvil. La pila web de visión que
> estaba prevista para ese canal se reutiliza en el espejo, que pasa de APK Android a **aplicación web
> en modo quiosco** — el hardware de señalización en tienda es heterogéneo y actualizar 50 pantallas
> es publicar, no distribuir. Ver [ADR-0011](ADR-0011-espejo-inteligente-anonimo.md).

### Por qué la web administrativa es una sola aplicación

Super administración y administración de tienda comparten el 70% de la interfaz: tablas, formularios,
autenticación, tema, navegación. Lo que cambia es el ámbito y el conjunto de rutas. Son dos secciones
de una aplicación, no dos aplicaciones. El módulo `features/platform` solo se carga para roles
`PLATFORM_*`, y el servidor autoriza de todos modos.

### Por qué la app de administración es el mismo APK

Publicar dos aplicaciones duplica el pipeline, la firma, las versiones y la fatiga de actualización.
El mismo APK muestra el módulo de administración cuando el usuario autenticado tiene un rol
administrativo (US-1904 CA-1). Un usuario administrativo también es consumidor: la separación de
módulos es por rol, no por instalación.

### Por qué iOS queda fuera hasta Fase 3

- ARCore y ML Kit Pose tienen su equivalente en iOS (ARKit, Vision), pero son implementaciones
  distintas: es un segundo desarrollo completo del probador, no un puerto.
- El mercado objetivo declarado (Colombia con salida a LATAM) es mayoritariamente Android.
- El costo del programa de desarrollador de Apple rompe la inversión cero de Fase 1.

**Sin mitigación, y hay que decirlo claro.** En la versión anterior de este ADR, el probador web
cubría a los usuarios de iPhone. Al retirarse ese canal, **una persona con iPhone no tiene ninguna
forma de usar el producto** hasta que exista app nativa de iOS.

Es una consecuencia real de la decisión de alcance, no un descuido. Las opciones son tres, y conviene
elegirla conscientemente:

| Opción | Costo | Cuándo |
|---|---|---|
| Aceptar que iOS queda fuera hasta Fase 3 | Cero ahora | Es el supuesto actual |
| App nativa iOS (SwiftUI + ARKit + Vision) | Segundo desarrollo completo del probador | Fase 3 |
| Reintroducir un probador web reducido | ~8–13 SP, reutilizando la pila del espejo | Fase 2 o 3, si el dato de mercado lo justifica |

La tercera es la más barata si aparece demanda de iPhone: la pila de visión en navegador ya se
construye para el espejo, así que sería reutilizarla, no crearla.

### Lo que hace viable tener cuatro canales

Tres piezas compartidas, y sin ellas esta decisión no se sostiene:

| Pieza | Qué evita duplicar |
|---|---|
| **`contracts/openapi.yaml`** | Los clientes de web y Android se **generan**. Nadie escribe un DTO a mano |
| **`contracts/design-tokens.json`** (`EN-1903`) | Un cambio de token llega a web y a Compose desde una sola fuente |
| **`shared-domain` (Kotlin puro)** | La política de tallas se escribe una vez y corre en el dispositivo, en el servidor y —compilada a WebAssembly— en el navegador |

`shared-domain` es la que más importa: sin ella habría tres implementaciones del cálculo de talla, y
una talla distinta según el canal es un defecto que el usuario nota de inmediato.

## Consecuencias

**Positivas**
- Cada requisito que planteaste tiene un canal asignado y una fase.
- La duplicación real es baja gracias a las tres piezas compartidas.
- Cuatro canales en lugar de cinco: el probador web de consumidor desaparece y su pila se reutiliza
  en el espejo, que es donde tiene un caso de uso real.
- El espejo como web se despliega a N pantallas publicando, no distribuyendo un APK a cada una.

**Negativas**
- La web administrativa (`EN-1901`, 13 SP) es trabajo nuevo que no estaba estimado en el backlog.
- Mantener paridad de componentes entre web y Compose exige disciplina; se verifica en CI (`EN-1903`
  CA-4), no por buena voluntad.
- El espejo es un pipeline distinto al de Android: MediaPipe en lugar de ML Kit, WebGL/WebGPU en lugar
  de Canvas nativo. Es una segunda implementación de visión que mantener.
- **iOS queda sin ninguna vía de acceso al producto** hasta Fase 3. Antes lo cubría el probador web;
  ahora no lo cubre nada. Hay que decirlo en voz alta ante un inversionista o un cliente.
- El espejo exige hardware con GPU en tienda y calibración física por instalación.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Flutter o React Native para todo | Resolvería iOS, pero AR y ML on-device requieren puentes nativos: se paga el costo multiplataforma sin obtener el beneficio en la parte que más importa |
| Solo web (PWA) | El probador en tiempo real es notablemente peor en navegador móvil, y la distribución en tienda de aplicaciones tiene valor comercial |
| Aplicación administrativa separada | Duplica pipeline, firma y versionado sin beneficio |
| Espejo como APK Android | Fue la propuesta inicial. El hardware de señalización en tienda es heterogéneo y actualizar N pantallas por APK es operación manual recurrente. Ver [ADR-0011](ADR-0011-espejo-inteligente-anonimo.md) |
| Mantener el probador web de consumidor | No corresponde al alcance confirmado: el avatar y las medidas son del canal móvil. Reintroducirlo es barato si aparece demanda de iPhone (~8–13 SP reutilizando la pila del espejo) |
