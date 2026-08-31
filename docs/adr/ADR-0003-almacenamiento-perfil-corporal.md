# ADR-0003 · Perfil corporal: local cifrado, con sincronización E2E opcional

- **Estado:** Propuesta · **revisada el 2026-08-31** tras aclaración de alcance del usuario
- **Fecha:** 2026-08-31
- **Decide:** Arquitectura + Privacidad + Sponsor
- **Sustituye a:** la versión anterior de este ADR, que recomendaba cifrado E2E obligatorio

## Nota de revisión

La primera versión de este ADR argumentaba contra "solo local" porque rompía cuatro casos de uso.
El usuario aclaró el alcance real y **dos de esos cuatro casos desaparecen**:

| Objeción original | Estado tras la aclaración |
|---|---|
| La web necesita el perfil | **Ya no aplica.** La web es administrativa. No hay probador web de consumidor |
| El espejo en tienda necesita el perfil | **Ya no aplica.** El espejo mide en el momento y **no guarda perfil** por diseño ([ADR-0011](ADR-0011-espejo-inteligente-anonimo.md)) |
| Cambio de dispositivo | Sigue aplicando, pero es un caso menor y resoluble como opción |
| Analítica de curva de tallas | Sigue aplicando, y ahora tiene una **fuente mejor**: las mediciones anónimas del espejo |

Con eso, la propuesta del usuario —perfil solo en el móvil— pasa a ser la opción correcta.

## Contexto

El planteamiento original fue: *"para el usuario pensaba que solo se guarda su contraseña y usuario,
para llenar su perfil con sus datos de medida y preferencias que sea guardada de manera local en su
móvil"*.

Alcance confirmado:

- **El avatar y las medidas son exclusivamente del canal móvil**, para la persona que quiere ver
  prendas en su teléfono.
- **La web no es probador de consumidor.** Es administración (tienda y plataforma).
- **El espejo en tienda es un canal aparte y anónimo**: mide en el momento, muestra, y no persiste
  identidad.

Medidas corporales, silueta, fotos de cuerpo completo y rostro son datos sensibles bajo la Ley 1581
de 2012, con exigencia de autorización previa, expresa, informada y cualificada. El riesgo `R-04`
está bien clasificado como crítico.

## Decisión

**El perfil corporal vive cifrado en el dispositivo del usuario. La sincronización es opcional,
apagada por defecto, y cuando se activa es de extremo a extremo.**

```
Por defecto (lo que obtiene el 100% de los usuarios)
    Medidas, silueta, avatar y fotos
        └─► Room + SQLCipher, clave en Android Keystore (respaldo por hardware)
        └─► NADA de esto sale del dispositivo
        └─► El servidor solo conoce: correo, hash de contraseña, preferencias de estilo

Opcional, si el usuario lo activa (US-0208)
    El perfil se cifra con una clave derivada del usuario
        └─► El servidor almacena un blob que NO puede descifrar
        └─► Frase de recuperación de 12 palabras
        └─► Sirve únicamente para restaurar en un dispositivo nuevo
```

### Qué se guarda dónde

| Dato | Dispositivo | Servidor por defecto | Servidor si sincroniza | Legible por la plataforma |
|---|---|---|---|---|
| Medidas corporales | Cifrado (Keystore) | **No** | Blob cifrado E2E | **No, nunca** |
| Silueta y landmarks | Cifrado, efímero | **No** | **No** | **No** |
| Foto de captura | Efímera, borrada ≤ 24 h | **No** | **No** | **No** |
| Avatar (parámetros) | Cifrado | **No** | Blob cifrado E2E | **No** |
| Rostro del avatar | Cifrado | **No** | Blob cifrado E2E | **No** |
| Preferencias de estilo | Local | Sí, en claro | Sí | Sí (no es dato sensible) |
| Correo, contraseña | — | Sí (hash Argon2id) | Sí | Solo el hash |
| Talla recomendada | Local | Solo como evento agregado | Idem | Agregado |

### El cálculo de talla también es local

La política de recomendación vive en `shared-domain` (Kotlin puro) y corre **en el dispositivo**
([ADR-0007](ADR-0007-recomendacion-de-talla-determinista.md)). Las medidas no viajan para calcular la
talla: viaja, como mucho, el resultado agregado ("talla M en categoría camisas") como evento de
analítica.

Esto es lo que hace que "solo local" sea una garantía verificable y no una intención: se comprueba con
un capturador de tráfico, y hay una prueba automatizada que falla si aparece una petición saliente con
datos corporales en el flujo base (RN-014).

### Lo que la analítica pierde y cómo se compensa

Con el perfil solo local, el servidor **no puede** analizar distribuciones de medidas individuales.
La curva de tallas (`US-1310`) se alimenta de dos fuentes, ambas ya agregadas:

1. **Bandas de talla enviadas por el móvil** — el dispositivo calcula y envía "talla M en camisas",
   nunca "pecho 94 cm".
2. **Mediciones anónimas del espejo en tienda** ([ADR-0011](ADR-0011-espejo-inteligente-anonimo.md)) —
   agregadas en el borde, sin identidad asociada.

La segunda es, de hecho, **mejor fuente** que un perfil almacenado en el servidor: cubre a personas
que nunca instalarán la app, y llega ya anónima por construcción.

## Consecuencias

**Positivas**
- Es lo que pediste, y ahora es también lo técnicamente correcto.
- El servidor no custodia datos biométricos: **se elimina la clase de riesgo, no se gestiona**. Una
  fuga de la base de datos no es una fuga de datos corporales.
- La superficie regulatoria se reduce de forma drástica: lo que no se almacena no hay que proteger,
  retener, exportar ni borrar.
- Menos trabajo que la propuesta E2E obligatoria: la sincronización pasa a ser `Should`, no `Must`.
- Verificable con una prueba de red, no con una promesa.
- La app funciona completa sin conexión (RNF-07) porque el perfil y el cálculo son locales.

**Negativas — hay que aceptarlas conscientemente**
- **Si el usuario pierde el teléfono y no activó la sincronización, pierde su perfil.** Se comunica de
  forma explícita en el onboarding y se ofrece activar la sincronización.
- Si la activa y pierde contraseña **y** frase de recuperación, el perfil es irrecuperable. No puede
  haber recuperación del lado del servidor, porque si la hubiera el servidor podría descifrar.
- Soporte no puede diagnosticar mirando el perfil del usuario. Debe hacerlo con eventos agregados y
  con lo que el usuario reporte.
- No hay analítica individual de medidas en el servidor. Solo agregados. Es un costo deliberado.
- Si más adelante quisieras un probador web de consumidor, esta decisión habría que revisarla. Queda
  registrado como disparador de revisión de este ADR.

## Impacto en el backlog

| Ítem | Cambio |
|---|---|
| `EN-0207` Cifrado local del perfil | **Sigue siendo Must, Sprint 2.** Es ahora el control principal, no un complemento |
| `US-0208` Recuperación en dispositivo nuevo | Baja de `Must` a **`Should`**, Fase 2. Es una comodidad, no un requisito del núcleo |
| `US-0618` Probador web por webcam | **Se retira** como historia de consumidor. Su contenido pasa al espejo ([ADR-0011](ADR-0011-espejo-inteligente-anonimo.md)) |
| Sincronización E2E | Pasa de obligatoria a opcional, apagada por defecto |

**Ahorro neto:** ~13 SP, y una reducción importante de riesgo regulatorio.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| **E2E obligatorio** (versión anterior de este ADR) | Resolvía casos de uso que, según el alcance confirmado, no existen. Más trabajo y más complejidad sin beneficio |
| Servidor con cifrado en reposo estándar | La plataforma podría leer las medidas. Dado el perfil regulatorio, es la peor de las tres opciones |
| Solo local **sin** cifrado local | El dato más sensible del producto en claro en el dispositivo. Un teléfono perdido o una copia de seguridad del sistema lo expone. `EN-0207` no es negociable |
