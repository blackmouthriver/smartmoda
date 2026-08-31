# Paquete de importación · Azure DevOps Boards

Generado el 31-ago-2026 desde la documentación del proyecto.

| Archivo | Qué es | Para qué |
|---|---|---|
| `SmartModa_AzureDevOps.xlsx` | Libro de 11 hojas con todo el backlog | **Planificar y revisar.** Léelo, edítalo, compártelo |
| `csv/` + `import_ado.py` | Configurados para org `proyectoOutfit`, proyecto `Outfit` | Cargar en Boards |
| `csv/01_epicas.csv` | 21 épicas | Importar en Boards |
| `csv/02_features_rf_rnf.csv` | 148 RF + 15 RNF como Features | Importar en Boards |
| `csv/03_historias.csv` | 163 historias con criterios de aceptación | Importar en Boards |
| `Outfit_ExcelTeam.xlsx` | Formato de lista de árbol para el complemento de Excel | Publicar desde Excel (pestaña Team) |
| `import_ado.py` | Importador por API con jerarquía completa | **La vía recomendada** |

> **Nota de nombres.** El proyecto en Azure DevOps se llama **Outfit** (organización
> `proyectoOutfit`). La documentación usa **SmartModa** como nombre de trabajo del producto
> ([ADR-0010](../docs/adr/ADR-0010-nombre-y-marca.md)). Son dos cosas distintas y pueden convivir: el
> nombre del proyecto en Azure es interno, el del producto es el que ve el usuario. Si quieres
> unificarlos, dilo y renombro la documentación.

**Contenido:** 21 épicas · 163 historias · 653 criterios de aceptación · 148 RF · 15 RNF ·
24 sprints · 39 reglas de negocio · 32 riesgos · 14 ADR · **1.143 story points**.

---

## Antes de importar: el proceso del proyecto

**El proyecto debe usar el proceso `Agile`.** Compruébalo con `python import_ado.py --check`.

Si usa `Basic` —el que Azure crea por defecto— la importación no puede funcionar:

| | Basic | Agile |
|---|---|---|
| Jerarquía | Epic → Issue → Task | Epic → Feature → **User Story** |
| **Acceptance Criteria** | **No existe** | Sí |
| **Story Points** | **No existe** | Sí |

Se perderían los 653 criterios de aceptación y los 1.143 puntos, que son el contenido del backlog.

**Cómo cambiarlo** (gratis mientras el proyecto esté vacío):
*Organization Settings → Boards → Process → `Basic` → pestaña **Projects** → fila del proyecto →
menú `⋮` → **Change process** → `Agile`.*

Si el proyecto ya tuviera trabajo dentro, sale más limpio crear uno nuevo eligiendo Agile.

---

## Antes de importar: crear las iteraciones

Los CSV apuntan a la organización **`proyectoOutfit`** y al proyecto **`Outfit`**, con
`Iteration Path = Outfit\Sprint N`. Si esas iteraciones no existen, la importación falla o deja todo
en el backlog raíz.

**Project Settings → Boards → Project configuration → Iterations** → crear `Sprint 0` … `Sprint 23`.

El propio script las crea y las asigna al equipo por defecto:

```powershell
python import_ado.py --setup-iterations
```

Es idempotente: las que ya existan se saltan. Si prefieres hacerlo a mano, es
**Project Settings → Boards → Project configuration → Iterations**, y luego asignarlas al equipo en
**Team configuration → Iterations**.

> **Si renombras el proyecto en Azure**, haz un buscar/reemplazar de `Outfit\` por `NuevoNombre\`
> en las columnas `Iteration Path` y `Area Path` de los tres CSV.

---

## Opción A — Importador por API (recomendada)

Es la única vía que crea la jerarquía **Epic → Feature → User Story** de una sola pasada. La
importación por CSV de Boards no crea relaciones padre-hijo.

No hay que instalar nada: el script usa solo la librería estándar de Python.

En PowerShell (la terminal por defecto de VS Code en Windows):

```powershell
$env:ADO_ORG = "https://dev.azure.com/proyectoOutfit"; $env:ADO_PROJECT = "Outfit"; $env:ADO_PAT = "pega-tu-token"
```

```powershell
python import_ado.py --check
```

Verifica cinco cosas **sin crear nada**: credenciales, que el proyecto exista, la plantilla de
proceso, las 24 iteraciones y las rutas de los CSV. Corrige lo que marque `[FALLA]` y repite hasta
ver `LISTO PARA IMPORTAR`. Luego:

```powershell
python import_ado.py --dry-run
```

Revisa la salida y, si te cuadra:

```powershell
python import_ado.py
```

- El PAT necesita alcance **Work Items (Read, write & manage)**.
- El script guarda `.ado_ids.json` con lo ya creado: si algo falla a mitad, **vuelve a ejecutarlo** y
  continúa donde quedó, sin duplicar.
- `--only epics|features|stories` importa una sola capa.

**No subas `.ado_ids.json` ni el PAT al repositorio.** El `.gitignore` que se propone abajo ya los
excluye.

---

## Opción B — Importación por CSV desde el navegador

Sirve si no quieres usar un PAT. **Limitación real:** los work items se crean sueltos, sin jerarquía;
tendrás que enlazar padres a mano o por edición masiva.

En **Boards → Work Items → Import Work Items**, en este orden:

1. `csv/01_epicas.csv`
2. `csv/02_features_rf_rnf.csv`
3. `csv/03_historias.csv`

Para enlazar después: filtra por la etiqueta de épica (`EP-04`, `EP-17`…), selecciona todo,
clic derecho → **Change parent**.

---

## Opción C — El complemento de Excel (pestaña Team)

Requiere **Visual Studio** completo con *Azure DevOps Office Integration*.

> **VS Code no sirve para esto.** No existe ninguna extensión de VS Code que añada la pestaña Team a
> Excel: es un add-in COM de Office que solo se distribuye con Visual Studio. Si lo que tienes es
> VS Code, usa la Opción A.

Comprobación: abre Excel y busca la pestaña **Team** en la cinta. Si no está, añádela desde
*Visual Studio Installer → Modificar → Componentes individuales*, buscando "Office".

Usa **`SmartModa_ExcelTeam.xlsx`**, no `SmartModa_AzureDevOps.xlsx`. Ese libro trae el formato de
**lista de árbol** que el complemento necesita (columnas `Title 1` / `Title 2`), que es lo que le
permite crear jerarquía padre-hijo — algo que la importación por CSV no hace:

| Hoja | Contenido | Orden |
|---|---|---|
| `INSTRUCCIONES` | Paso a paso y errores frecuentes | Léela primero |
| `1_Arbol_Historias` | 21 épicas + 163 historias, 184 filas | Publicar primero |
| `2_Arbol_Requisitos` | RF y RNF como Features, 180 filas | Publicar después, **rellenando el ID de las épicas ya creadas** |

Flujo: **Team → New List → Input list → Add Tree Level → Choose Columns**, pegar desde la fila 5 y
**Publish**. Publica en lotes de 50–100 filas, no las 184 de golpe.

**Cuándo usar esta vía y cuándo no.** Si ya trabajas con el complemento, es cómoda. Para 332 work
items es frágil: el paso de rellenar a mano los ID de las épicas antes del segundo árbol es donde se
generan las épicas duplicadas. La Opción A hace los tres niveles de una vez y reanuda si falla.

---

## Cómo queda el mapeo

| Documentación | Azure Boards | Nota |
|---|---|---|
| `EP-xx` épicas | **Epic** | 21 |
| `RF-xxx` requisitos funcionales | **Feature** | 148, etiquetados `RF` |
| `RNF-xx` requisitos no funcionales | **Feature** | 15, etiquetados `RNF`, bajo EP-15 |
| `US-xxxx` historias | **User Story** | Etiqueta `Historia` |
| `EN-xxxx` habilitadores | **User Story** | Etiqueta `Habilitador` |
| `SP-xxxx` spikes | **User Story** | Etiqueta `Spike` |
| Criterios Gherkin | **Acceptance Criteria** (HTML) | Lista numerada |
| Must/Should/Could | **Priority** 1/2/3 | |
| Story points | **Story Points** | |
| Sprint | **Iteration Path** | `Proyecto\Sprint N` |

Habilitadores y spikes van como *User Story* y no como *Task* a propósito: en el proceso Agile solo
las User Story tienen campo **Story Points**, y sin puntos no se puede planificar la capacidad de un
sprint. La etiqueta los distingue en el board.

---

## Después de importar

1. **Consultas útiles.** Crea tres: `Fase 1 - Must`, `Bloqueantes` (etiqueta `Spike` o `Gate`) y
   `Sin criterios de aceptación` (debería devolver cero).
2. **Ajusta el plan a la realidad.** Los 24 sprints de la línea base suponen ~4 personas. Con una
   persona, el calendario real es **8–9 meses solo para la Fase 1 recortada**
   ([docs/17](../docs/17-plan-de-trabajo.md)). Recalibra tras 3 sprints con la velocidad medida.
3. **Marca los gates.** `SP-0606`, `SP-0619` (activos), `SP-1405` (precisión del espejo),
   `EN-1206` (aislamiento), `EN-1410` (revisión jurídica). Son puntos de decisión, no tareas.
4. **Convierte los criterios en pruebas.** La hoja `Criterios` del xlsx tiene los 653 en filas
   independientes, listos para Azure Test Plans o para nombrar pruebas automatizadas.

---

# Subir el código a Azure Repos y GitHub en paralelo

Sí se puede, y hay una configuración que funciona mejor que las otras.

## Recomendación: GitHub para el código, Azure Boards para el trabajo

No por preferencia, sino porque el plan de automatización del proyecto (`AUT-01` a `AUT-16` en
[docs/13](../docs/13-stack-tecnologico.md)) ya está escrito sobre **GitHub Actions**, y Azure Boards
se integra con GitHub de forma nativa: los commits y PR quedan enlazados a los work items.

Con la app **Azure Boards** instalada en el repositorio de GitHub, basta escribir el ID en el mensaje
de commit:

```
git commit -m "US-0502: motor de talla determinista con nivel de confianza

Implementa RN-001 y RN-016.

AB#42"
```

`AB#42` enlaza ese commit con el work item 42. Los IDs reales los tendrás en `.ado_ids.json` tras la
importación.

## Configuración A — Un `git push`, dos destinos

La más cómoda para el día a día: un remoto con dos URL de escritura.

```bash
git remote add origin https://github.com/TU-USUARIO/smartmoda.git
git remote set-url --add --push origin https://github.com/TU-USUARIO/smartmoda.git
git remote set-url --add --push origin https://TU-ORG@dev.azure.com/TU-ORG/SmartModa/_git/SmartModa
```

Verifica antes de confiar en ello:

```bash
git remote -v
```

Debe mostrar **dos líneas `(push)`**. A partir de ahí, `git push` escribe en los dos.

`git pull` sigue leyendo solo de la primera URL (GitHub), que es lo correcto: un solo origen de
verdad para leer.

## Configuración B — Dos remotos con nombre

Más explícita. Útil si quieres controlar cuándo va cada uno.

```bash
git remote add origin https://github.com/TU-USUARIO/smartmoda.git
git remote add azure  https://TU-ORG@dev.azure.com/TU-ORG/SmartModa/_git/SmartModa
```

```bash
git push origin main
```

```bash
git push azure main
```

Y para empujar a ambos de una vez:

```bash
git push origin main && git push azure main
```

## Primera subida desde local

```bash
git init -b main
```

```bash
git add . && git commit -m "chore: estructura inicial del proyecto SmartModa"
```

Luego añade los remotos con la Configuración A o B y:

```bash
git push -u origin main
```

## Un aviso sobre trabajar en dos sitios

Dos remotos es replicación, **no** sincronización bidireccional. Si alguien empuja directamente a
Azure Repos y tú empujas a GitHub, divergen y el siguiente push falla.

Regla: **uno es el origen de verdad y el otro es un espejo.** Todo el trabajo —ramas, PR, revisiones—
ocurre en uno solo. Si más adelante quieres el flujo completo en Azure DevOps, se invierte la
configuración; lo que no funciona es abrir PR en los dos.

## `.gitignore` mínimo

```gitignore
# Azure DevOps
azure-devops/.ado_ids.json

# Secretos
.env
*.pem
local.properties
google-services.json

# Android
*.iml
.gradle/
build/
local.properties
.cxx/

# Web
node_modules/
dist/
.vite/

# IDE
.idea/
.vscode/
*.swp

# SO
.DS_Store
Thumbs.db
```

## Pipeline

Con GitHub como origen, `AUT-01` (build + lint + tests en cada PR) se implementa como GitHub Action.
Si prefieres Azure Pipelines, también puede construir desde un repositorio de GitHub sin mover el
código: **Pipelines → New pipeline → GitHub**.

Lo que **no** conviene es mantener dos pipelines equivalentes en las dos plataformas. Elige una; la
otra queda como espejo del código y nada más.
