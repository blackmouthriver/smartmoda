# Cómo se trabaja en este repositorio

> `EN-0003` · Repositorio, estrategia Git y plantillas · Sprint 0

Con una sola persona, gran parte de esto puede parecer burocracia. No lo es: son los hábitos que
hacen que dentro de seis meses se pueda saber por qué existe una línea de código, y que una segunda
persona pueda incorporarse sin arqueología.

---

## 1. Estructura del repositorio

Monorepo. El contrato de API, el móvil, la web y el dominio compartido viven juntos, para que un
cambio de contrato se vea de inmediato en sus consumidores.

```
smartmoda/
├── docs/                  documentación viva · fuente de verdad del análisis
├── contracts/             fuente de verdad de los contratos
│   ├── openapi.yaml       API — los clientes se generan desde aquí
│   ├── design-tokens.json tokens de diseño → CSS + Compose
│   └── events/            esquemas de eventos de analítica
├── shared-domain/         Kotlin puro · reglas de negocio · sin Android, sin Spring
├── android/               app de consumidor + módulo de administración
├── web/                   portal administrativo (Fase 2)
├── tools/                 generadores y verificadores
├── azure-devops/          paquete de importación del backlog
└── .github/workflows/     CI
```

**`shared-domain` es la pieza clave.** La política de recomendación de talla se escribe una vez y
corre en el móvil (sin conexión), en el servidor y en el navegador. Sin ella habría tres
implementaciones divergiendo, y una talla distinta según el canal es un defecto que el usuario nota.

---

## 2. Ramas

```
main ─────────────────────────────────────────────►  siempre desplegable
  └── feat/US-0502-motor-de-talla
  └── fix/US-0603-overlay-desalineado
  └── chore/EN-0004-cache-de-gradle
```

- **`main` siempre compila y pasa CI.** No se rompe "un momentito".
- Una rama por work item. El nombre lleva el ID: `<tipo>/<ID>-<descripción-corta>`.
- Tipos: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `spike`.
- Ramas cortas. Si una rama vive más de una semana, el ítem estaba mal partido.
- Merge con **squash**: un work item, un commit en `main`. El historial se lee como el backlog.

Sin `develop`. Con una persona y despliegue continuo, una rama de integración añade ceremonia sin
resolver nada. Lo que protege `main` es CI, no una rama intermedia.

---

## 3. Commits

```
<tipo>(<ámbito>): <qué cambia, en imperativo>

<por qué, si no es obvio>

<referencias>
```

Ejemplo real:

```
feat(sizing): recomendar talla con nivel de confianza

Implementa el cálculo determinista contra tabla versionada.
La confianza se deriva de la dispersión entre medidas clave,
no de un umbral fijo: ver ADR-0007.

Cumple RN-001, RN-002, RN-016, RN-017.
AB#170
```

Reglas:

- **`AB#<id>`** enlaza el commit con el work item de Azure Boards. Los IDs están en
  `azure-devops/.ado_ids.json`.
- Cita las reglas de negocio que implementa (`RN-xxx`). Es lo que permite auditar la trazabilidad.
- El *qué* va en la primera línea; el *por qué* en el cuerpo. El *cómo* ya está en el diff.
- Sin `--no-verify`. Si un hook falla, se arregla la causa.

---

## 4. Pull requests

Aunque trabajes solo, abre PR. No por proceso: porque el PR es donde CI corre y donde queda escrito
el razonamiento. Un `git push` directo a `main` se salta ambas cosas.

La plantilla está en `.github/pull_request_template.md`.

---

## 5. Definición de terminado

Un ítem está terminado cuando se cumple todo esto, no cuando "funciona":

- [ ] Cada criterio de aceptación tiene una prueba automatizada que lo verifica
- [ ] Las reglas de negocio que toca tienen prueba con el ID en el nombre (`RN_009_...`)
- [ ] Cobertura del dominio nuevo ≥ 85%
- [ ] Si añade endpoint: está en `contracts/openapi.yaml` y tiene prueba de aislamiento multi-tenant
- [ ] Si añade tabla de negocio: `tenant_id NOT NULL` + política RLS
- [ ] Si añade acción administrativa: genera registro de auditoría, verificado por prueba
- [ ] Si añade pantalla: pruebas de estado, instantánea y accesibilidad
- [ ] 0 hallazgos de seguridad críticos o altos
- [ ] Lint, formato y verificación de arquitectura en verde
- [ ] **Documentación actualizada en el mismo PR**
- [ ] Autorrevisión hecha (lee tu propio diff completo antes de fusionar)

Detalle en [docs/15-estrategia-de-pruebas.md](docs/15-estrategia-de-pruebas.md).

---

## 6. Reglas de arquitectura que CI verifica

No son recomendaciones. Rompen la compilación:

| Regla | Por qué |
|---|---|
| `shared-domain` no importa Android, Spring ni Jackson | Debe correr en los tres canales |
| Un módulo solo depende de otro por su paquete `api` | Es lo que permite extraer servicios después ([ADR-0012](docs/adr/ADR-0012-microservicios-acotados.md)) |
| Toda tabla de negocio tiene `tenant_id NOT NULL` | Añadirlo tarde es una reescritura ([ADR-0002](docs/adr/ADR-0002-estrategia-multitenant.md)) |
| Toda regla `RN-xxx` tiene una prueba que la nombra | Trazabilidad verificable con `grep`, no con buena voluntad |
| 0 secretos en el repositorio | Obvio, y aun así pasa |
| Sin `!!` en Kotlin fuera de tests | |
| Sin `TODO` sin ID de work item asociado | Un TODO sin ticket es un TODO eterno |

---

## 7. Qué NO se hace

- No se sube nada a producción sin pasar CI.
- No se desactivan gates de seguridad, aislamiento ni auditoría para ir más rápido. Los de
  rendimiento sí se pueden posponer; esos tres no se pueden reparar retroactivamente.
- No se commitean secretos, ni siquiera "temporalmente". Un secreto en el historial es un secreto
  filtrado, aunque se borre después.
- No se persiste ningún dato corporal sin cifrar ([ADR-0003](docs/adr/ADR-0003-almacenamiento-perfil-corporal.md)).
- No se publica la app hasta cerrar la búsqueda marcaria ([ADR-0010](docs/adr/ADR-0010-nombre-y-marca.md)).
