# Anexos · Insumos originales

Los archivos fuente de este proyecto no se copian a este repositorio para no duplicar la fuente de
verdad. Ubicación original:

| Archivo | Rol | Estado |
|---|---|---|
| `HISTORIAS_Y_HABILITADORES_MVP_IA_MODA_COMPLETO (1).xlsx` | 35 HU + 6 habilitadores, enfoque MVP consumidor | **Archivado** — absorbido en el doc 07 con mapeo de IDs |
| `Backlog_Ejecutable.xlsx` | 111 ítems, 17 épicas, 24 sprints, 335 criterios | **Fuente de verdad del backlog** |
| `Sistema de color - Marketplace de moda.doc` | Paleta OKLCH v1, WCAG AA, modo oscuro | Absorbido íntegro en el doc 16 |

## Qué hacer con ellos

1. **`Backlog_Ejecutable.xlsx` sigue siendo la fuente de verdad de las 111 historias existentes.**
   Los criterios de aceptación detallados viven en su hoja `Criterios`.
2. Las 52 historias nuevas están en [08-historias-nuevas.md](../08-historias-nuevas.md).
   Cuando se importen a la herramienta de gestión (Jira, Linear, GitHub Projects), deben cargarse
   junto con las 111 existentes en un solo backlog.
3. **No mantener dos backlogs.** El primer Excel se archiva; su numeración `H-00x`/`HB-00x` solo se
   usa para trazabilidad histórica.

## Importación a herramienta de gestión

El `0_LEEME` del `Backlog_Ejecutable` indica el mapeo para importar el CSV: `Epic Link`,
`Story Points`, `Sprint` y `Labels`. Las historias nuevas siguen la misma estructura de columnas.
