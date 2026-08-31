# 19 · Registro de riesgos

Conserva los 14 riesgos del `Backlog_Ejecutable` y añade 14 derivados del análisis de brechas y del
alcance del espejo aclarado el 31-ago-2026.
Revisión obligatoria al cierre de cada fase.

**Nivel:** Crítico (puede terminar el proyecto) · Alto (retrasa una fase) · Medio (retrasa un sprint)

---

## Riesgos heredados

| ID | Nivel | Riesgo | Impacto | Mitigación | Dueño |
|---|---|---|---|---|---|
| **R-01** | Crítico | No hay activos 3D estandarizados por prenda | AR se vuelve manual, costosa y lenta | Spike temprano `SP-0606` con **gate go/no-go en el Sprint 8**; contrato de activos; solo categorías viables ([ADR-0006](adr/ADR-0006-probador-progresivo.md)) | Arquitectura |
| **R-02** | Crítico | Se promete física de tela o talla exacta antes de validar | Pérdida de confianza y riesgo comercial | Etiquetas de confianza obligatorias (RN-004, RN-016), benchmark `EN-0616`, gate go/no-go | PO |
| **R-03** | Alto | Tablas de talla incompletas o inconsistentes | Recomendaciones erróneas | Validación en carga, versionado, fuente registrada, **fallback a `NONE` en lugar de inventar** (RN-001) | Catálogo |
| **R-04** | Crítico | Tratamiento inadecuado de datos biométricos | Sanción, cierre o daño reputacional | Consentimiento cualificado, minimización, retención automática, **el dato no sale del dispositivo** ([ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md), [ADR-0011](adr/ADR-0011-espejo-inteligente-anonimo.md)), auditoría legal | Privacidad |
| **R-05** | Alto | ML Kit Pose está en beta o cambia | Regresión técnica | Interfaz `PoseDetector` propia, versión fijada, MediaPipe como alterna probada, fallback manual | Móvil |
| **R-06** | Alto | ARCore/Depth no cubre todos los dispositivos | Usuarios excluidos | Matriz de compatibilidad + fallback 2D probado (RN-019) | Móvil |
| **R-07** | Alto | Sin API real de inventario del retailer | Stock no confiable y menor conversión | Piloto con importación validada (`US-0412`); contrato ERP en Fase 3 | Integraciones |
| **R-08** | Alto | Una sola persona intenta ejecutar los 24 sprints | Retraso y deuda | **Escenarios de capacidad explícitos** ([17 § 1](17-plan-de-trabajo.md)); limitar WIP a 2; priorizar Must; sumar backend/QA/3D por fase | Sponsor |
| **R-09** | Medio | Costos de IA, imagen y almacenamiento crecen | Margen SaaS negativo | Cuotas por tenant, caché, modelo por caso, FinOps (`EN-1606`); **sin IA generativa en el MVP** | FinOps |
| **R-10** | Alto | IA recomienda inventario inexistente o viola restricciones | Mala experiencia | Recuperación de candidatos del stock real + **validación determinista posterior** ([ADR-0009](adr/ADR-0009-gateway-ia-agnostico.md)) | IA |
| **R-11** | Crítico | Fallo de aislamiento multi-tenant | Exposición entre empresas | **Tres capas** (aplicación + RLS + suite negativa), `tenant_id` desde el Sprint 0 ([ADR-0002](adr/ADR-0002-estrategia-multitenant.md)), pentest con el aislamiento en el alcance | Seguridad |
| **R-12** | Medio | El nombre no está disponible | Retrabajo de marca | Búsqueda marcaria, dominio y `applicationId` **antes del Sprint 1** ([ADR-0010](adr/ADR-0010-nombre-y-marca.md)) | Fundadores |
| **R-13** | Alto | Sesgo por tipo de cuerpo, piel, ropa o dispositivo | Peor desempeño para grupos específicos | Banco de pruebas diverso desde el Sprint 4, métricas desagregadas, gate de evaluación | IA/Calidad |
| **R-14** | Medio | Dependencia de servicios gratuitos | Interrupción o migración temprana | Capa Repository + **PostgreSQL real desde Fase 1** ([ADR-0004](adr/ADR-0004-backend-fase-1.md)); plan de salida escrito en el Sprint 0 | Arquitectura |

---

## Riesgos nuevos

| ID | Nivel | Riesgo | Impacto | Mitigación | Dueño |
|---|---|---|---|---|---|
| **R-15** | **Crítico** | El alcance real (1.143 SP) excede varias veces la capacidad de **una sola persona**, confirmada el 31-ago | Fecha incumplida, o recortes improvisados que sacrifican seguridad | Fase 1 recortada a Must (~185 SP, 8-9 meses); limitar WIP a 2; recalibrar con velocidad real tras 3 sprints ([17 § 1](17-plan-de-trabajo.md)) | Sponsor |
| **R-16** | Alto | La auditoría se implementa después del portal administrativo | Cobertura parcial permanente; acciones sin rastro | `EN-1801` en el Sprint 7, **antes** de `US-1102`+; ArchUnit falla si un caso de uso administrativo no está anotado ([ADR-0008](adr/ADR-0008-auditoria-append-only.md)) | Arquitectura |
| **R-17** | **Crítico** | El perfil corporal se persiste en claro en Fase 1 y hay que migrarlo después | Migración del dato más sensible del producto, con ventana de exposición | `EN-0207` en el Sprint 2, no después. Ningún dato corporal se persiste antes de que exista el cifrado | Privacidad |
| **R-18** | Alto | Pérdida de perfil por olvido de la frase de recuperación | Frustración del usuario y abandono | Comunicación explícita al activar (US-0208), recordatorio de respaldo, opción de operar solo local | Producto |
| **R-19** | Alto | La personalización por tenant produce interfaces inaccesibles | Incumplimiento WCAG y daño de marca | Validación de contraste que **bloquea la publicación** (RN-026, US-1703 CA-2); tokens semánticos no personalizables | Diseño |
| **R-20** | Medio | Reidentificación de usuarios a partir de agregados analíticos | Incidente de privacidad | Umbral mínimo de 30 usuarios por celda; seudónimos con sal por tenant; evaluar privacidad diferencial si el volumen crece | Privacidad |
| **R-21** | Alto | El comercio carga catálogo y activos de baja calidad | El probador se ve mal y el usuario culpa al producto | Perfil de activo validado (RN-018), validación en la importación, alerta de anomalía por calidad de activo (`US-1309`) | Catálogo |
| **R-22** | Medio | La web y el móvil divergen en diseño y comportamiento | Producto que se siente como dos productos | Tokens con fuente única + lista de paridad de componentes verificada en CI (`EN-1903`) | Diseño |
| **R-23** | **Crítico** | La medición automática del espejo no alcanza precisión útil | El espejo no puede recomendar talla y pierde la mitad de su propuesta de valor | Spike `SP-1405` con gate: si falla, el espejo se reorienta a probador visual sin recomendación. Instalación calibrada (`EN-1411`) para eliminar el error de escala | Arquitectura |
| **R-24** | **Crítico** | Tratamiento de imagen en espacio físico sin base legal suficiente | Sanción y cierre de la línea de espejo | Cámara apagada hasta interacción, señalización previa, alternativa sin cámara, **revisión jurídica que bloquea el despliegue** (`EN-1410`) | Privacidad |
| **R-25** | Alto | Presión comercial para identificar clientes recurrentes en el espejo | Convierte un probador anónimo en un sistema de vigilancia, con otro marco legal | Prohibición de diseño verificada en revisión de arquitectura: `session_hash` aleatorio, sin conteo de únicos, sin correlación entre sesiones (`EN-1408`) | Privacidad |
| **R-26** | Alto | La atribución probado-vs-comprado sin escaneo en caja es solo correlación | El comercio toma decisiones de compra sobre un dato más débil de lo que cree | Etiquetar siempre la atribución como *exacta* o *estimada* en el tablero (`US-1409` CA-4 y CA-5); negociar el escaneo en caja con el comercio piloto | Datos |
| **R-27** | Medio | El espejo se descalibra sin que nadie lo note | Recomendaciones de talla erróneas durante días | Verificación periódica automática; al detectar desviación, el espejo **deja de recomendar talla** y alerta (`EN-1411` CA-3 y CA-4) | Operación |
| **R-29** | **Crítico** | Microservicios operados por una sola persona | El tiempo se va en infraestructura y despliegue en lugar de en producto; el MVP no llega | Máximo 5 servicios; aparición diferida según la funcionalidad; `core-api` sin fragmentar; una única plantilla de CI/CD ([ADR-0012](adr/ADR-0012-microservicios-acotados.md)). **Señal de revisión: >25% del esfuerzo en infraestructura al cerrar el Sprint 3** | Sponsor |
| **R-30** | Alto | Presión comercial para segmentar publicidad por datos corporales | Obliga a subir medidas al servidor y desmonta la arquitectura de privacidad y su defensa regulatoria | RN-038 como regla de dominio verificada por prueba negativa, no como política escrita ([ADR-0014](adr/ADR-0014-monetizacion-publicidad.md)) | Privacidad |
| **R-31** | **Crítico** | Tratamiento de imágenes de menores en el espejo de tienda | Es el escenario que una autoridad de protección de datos revisa primero. Sanción y cierre de la línea | Ni cámara ni probador para perfiles de menor; modo acompañante sin medición; degradación conservadora ante indicios; **concepto jurídico que bloquea el despliegue** ([ADR-0013](adr/ADR-0013-menores-y-ropa-intima.md)) | Privacidad |
| **R-32** | Alto | Sin comercio piloto identificado | No hay dónde validar talla, activos ni espejo, y la Fase 2 avanza a ciegas | Conseguir un piloto es tarea de Fase 1, no de Fase 2. La publicidad da ingresos mientras tanto ([ADR-0014](adr/ADR-0014-monetizacion-publicidad.md)) | Sponsor |
| **R-28** | Medio | iOS queda sin ninguna vía de acceso al producto | Exclusión de un segmento de mercado, visible ante inversionistas | Decisión consciente documentada en [ADR-0005](adr/ADR-0005-canales-y-plataformas.md); reintroducir probador web reducido cuesta ~8-13 SP si el dato de mercado lo justifica | PO |

---

## Los seis que vigilaría cada semana

1. **R-15 · Capacidad.** Es el que determina si todo lo demás es realista. Y es el único que se
   resuelve con una decisión, no con trabajo técnico.
2. **R-01 · Activos 3D.** El gate del Sprint 8 decide si la Fase 2 es viable. Adelanta el spike lo que
   puedas: no depende de que lo demás esté terminado.
3. **R-23 y R-24 · Espejo.** Precisión de la medición y base legal en espacio físico. Si cualquiera de
   los dos falla, la línea de espejo no existe — y es la que sostiene el contraste
   probado-vs-comprado, que es el diferenciador comercial.
4. **R-11 · Aislamiento.** Es el único fallo que puede terminar el negocio de un día para otro.
5. **R-04 / R-17 · Datos biométricos.** El riesgo regulatorio es el más caro de corregir tarde.
6. **R-03 · Tablas de tallas.** El producto entero depende de un dato que proveen terceros y que nadie
   ha verificado todavía.

## Cómo se gestiona un riesgo

Un riesgo sin dueño, sin fecha de revisión y sin señal de disparo no está gestionado: está anotado.
Para cada uno:

- **Dueño** — una persona, no un equipo.
- **Señal de disparo** — el hecho observable que indica que el riesgo se está materializando.
- **Acción preparada** — qué se hace ese día, decidido antes de necesitarlo.
- **Revisión** — al cierre de cada fase como mínimo; semanal para los cinco de arriba.

Ejemplo, `R-01`:

| Campo | Valor |
|---|---|
| Señal | El spike `SP-0606` reporta más de X horas por prenda |
| Acción | Se cancela AR; la Fase 2 se reorienta a mejorar el 2D; se comunica a stakeholders con el número |
| Revisión | Fin del Sprint 8 (gate formal) |

Decidir la acción **antes** de que se dispare la señal es lo que evita que la decisión se tome bajo
presión y con el sesgo del esfuerzo ya invertido.
