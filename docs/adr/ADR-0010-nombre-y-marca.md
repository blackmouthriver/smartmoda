# ADR-0010 · Nombre del producto y de la empresa

- **Estado:** **Parcialmente resuelta** el 31-ago-2026 — nombre de trabajo fijado, marca sin validar
- **Fecha:** 2026-08-31
- **Decide:** Fundadores + PO
- **Recoge:** `D-01` y `D-02` del `Backlog_Ejecutable`

## Contexto

En los insumos del proyecto circulaban tres nombres: **VístIA** (provisional en el backlog),
**SMART IA** (en el prototipo) y **Synaptia Technologies S.A.S.** (empresa, provisional).

El riesgo `R-12` lo identifica: si el nombre no está disponible, hay retrabajo de marca.

## Decisión

**Nombre de trabajo del producto: `SmartModa`.**
Aplicado a toda la documentación el 31-ago-2026, sustituyendo a *VístIA*.

**Nombre de la empresa: `Synaptia Technologies S.A.S.`**, sin cambios y aún sujeto a validación legal.

### Lo que esta decisión sí resuelve

- La documentación deja de tener tres nombres compitiendo.
- El `applicationId` de trabajo queda fijado: `com.synaptia.smartmoda`.
- El paquete raíz de código: `com.synaptia.smartmoda`.
- Desbloquea el Sprint 0 y el Sprint 1.

### Lo que esta decisión NO resuelve

`SmartModa` es un **nombre de trabajo**, no una marca validada. Sigue pendiente, y sigue bloqueando la
**publicación**, no el desarrollo:

1. **Búsqueda de antecedentes marcarios** en la Superintendencia de Industria y Comercio, en las clases
   relevantes: software y comercio de prendas de vestir.
2. **Disponibilidad de dominio** `.com` y `.co`.
3. **Disponibilidad del `applicationId`** en Play Store (y App Store, aunque iOS sea posterior:
   reservarlo cuesta poco y evita el problema).
4. **Redes sociales**, si entran en el plan de lanzamiento.

### Observación sobre `SmartModa`

A favor: se escribe y se teclea sin ambigüedad —no lleva tilde ni caracteres que compliquen dominios o
búsquedas—, y comunica el dominio de forma directa en español.

En contra: `smart` es un prefijo muy usado en marcas de tecnología, lo que suele reducir la
distintividad ante una oficina de marcas y complicar el posicionamiento en buscadores. **Es
precisamente el tipo de nombre que la búsqueda marcaria puede rechazar.** Conviene tener una segunda
opción preparada.

## Por qué la publicación sigue bloqueada

El `applicationId` es el punto de no retorno:

| Artefacto | Consecuencia de cambiarlo tras publicar |
|---|---|
| `applicationId` de Android | **Irreversible.** Es una app nueva: se pierden instalaciones, valoraciones y usuarios |
| Dominio y subdominios | Correos, enlaces profundos, enlaces compartidos, certificados |
| Enlaces profundos | Los enlaces que los usuarios ya compartieron dejan de funcionar |
| Proyectos en la nube | Recreación y migración |
| Marca registrada | Si otro la registra primero, el cambio ocurre bajo presión legal |

## Reglas mientras la marca no esté validada

- `com.synaptia.smartmoda` se usa en **compilaciones internas y de depuración**.
- **No se publica ninguna versión**, ni siquiera beta cerrada, hasta cerrar la búsqueda marcaria.
- El nombre visible del producto vive como recurso de cadena externalizado (`RNF-14` ya lo exige:
  cero cadenas embebidas en código). Cambiar el nombre mostrado es editar un archivo, no barrer el
  repositorio.
- Logo e identidad visual son archivos sustituibles, no recursos incrustados.

Con esas cuatro reglas, un cambio de marca antes de publicar cuesta prácticamente nada. Después de
publicar, cuesta semanas.

## Consecuencias

**Positivas**
- Sprint 0 y 1 desbloqueados.
- Un solo nombre en toda la documentación y el código.
- El costo de un cambio posterior queda acotado por diseño mientras no se publique.

**Negativas**
- Si la búsqueda marcaria rechaza `SmartModa`, hay que renombrar paquetes y documentación. Barato si
  ocurre antes de publicar; caro si ocurre después.
- `smart` como prefijo es poco distintivo: aumenta la probabilidad de que la búsqueda marcaria
  encuentre conflictos.

## Acción pendiente

**Búsqueda marcaria antes de la primera publicación.** Es la única tarea que queda de este ADR, y su
resultado puede confirmar el nombre o abrir un ADR-0010-bis con el definitivo.
