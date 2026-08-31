## Qué cambia

<!-- Una o dos frases. El "cómo" ya está en el diff. -->

**Work item:** AB#

## Por qué

<!-- El contexto que no se ve en el código. Si es obvio, borra esta sección. -->

## Reglas de negocio que toca

<!-- RN-001, RN-016... o "ninguna" -->

## Definición de terminado

- [ ] Cada criterio de aceptación tiene prueba automatizada
- [ ] Las `RN-xxx` que toca tienen prueba con el ID en el nombre
- [ ] Cobertura del dominio nuevo ≥ 85%
- [ ] Documentación actualizada **en este mismo PR**
- [ ] Autorrevisión del diff completo hecha

### Solo si aplica

- [ ] Endpoint nuevo → está en `contracts/openapi.yaml` + prueba de aislamiento multi-tenant
- [ ] Tabla nueva → `tenant_id NOT NULL` + política RLS
- [ ] Acción administrativa → genera registro de auditoría, verificado por prueba
- [ ] Pantalla nueva → pruebas de estado, instantánea y accesibilidad
- [ ] Toca datos corporales → siguen cifrados y sin salir del dispositivo

## Riesgo

<!-- Qué se puede romper y cómo se revierte. "Ninguno" es una respuesta válida si es cierta. -->
