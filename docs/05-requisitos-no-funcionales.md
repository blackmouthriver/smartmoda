# 05 · Requisitos no funcionales

Un RNF que no se puede medir no es un requisito, es un deseo. Cada uno lleva **umbral**, **método de
verificación** y **quién lo verifica**. Los que no tienen prueba automatizada son deuda desde el día uno.

---

## RNF-01 · Rendimiento

| Métrica | Umbral | Verificación |
|---|---|---|
| Arranque en frío con caché, móvil gama media | p95 ≤ 3,0 s hasta primer contenido útil | Macrobenchmark en CI nocturno |
| Arranque en caliente | p95 ≤ 1,2 s | Macrobenchmark |
| API de catálogo (listado paginado) | p95 ≤ 800 ms, p99 ≤ 1,5 s | Prueba de carga k6 en preproducción |
| API de detalle de producto | p95 ≤ 400 ms | k6 |
| Cálculo de talla (on-device) | p95 ≤ 150 ms | Test unitario con presupuesto |
| Overlay 2D (render inicial) | p95 ≤ 1,5 s desde selección de prenda | Test instrumentado |
| Web administrativa: LCP | ≤ 2,5 s en 4G simulada | Lighthouse CI, gate en PR |
| Web administrativa: INP | ≤ 200 ms | Lighthouse CI + RUM |
| Bloqueo del hilo principal | 0 operaciones de ML o red en el hilo de UI | Lint + StrictMode + revisión |

## RNF-02 · Rendimiento de AR y cámara

| Métrica | Umbral | Verificación |
|---|---|---|
| FPS en dispositivos de la matriz soportada | ≥ 24 FPS sostenidos | Test instrumentado en Firebase Test Lab |
| Latencia pose → render | ≤ 80 ms | Instrumentación en sesión AR |
| Temperatura / throttling | Degradar calidad antes de congelar o calentar en exceso | Prueba de sesión larga (10 min) |
| Fallback | 100% de dispositivos no compatibles caen a 2D sin error visible | Test de compatibilidad |

## RNF-03 · Confiabilidad

| Métrica | Umbral | Verificación |
|---|---|---|
| Crash-free sessions (beta) | ≥ 99,5% | Crashlytics |
| Crash-free sessions (producción) | ≥ 99,8% | Crashlytics + gate de release |
| ANR rate | ≤ 0,47% (umbral de Play) | Play Vitals |
| Disponibilidad de API (Fase 3) | ≥ 99,5% mensual | Uptime + SLO |
| Idempotencia de operaciones críticas | 100% de pagos, reservas y webhooks | Test de contrato con reintento duplicado |

## RNF-04 · Seguridad

| Control | Umbral | Verificación |
|---|---|---|
| Transporte | TLS 1.2+ obligatorio, HSTS, certificate pinning en móvil | Escaneo TLS en CI |
| Cifrado en reposo | AES-256 gestionado; datos corporales cifrados E2E | Revisión de arquitectura + auditoría |
| Contraseñas | Argon2id (o bcrypt cost ≥ 12); nunca reversible | Test unitario del encoder |
| **Fuerza bruta** | Ver [12 § 3](12-seguridad-y-privacidad.md); 0 endpoints de auth sin límite | Test de seguridad automatizado en CI |
| Secretos | 0 secretos en el repositorio | Gitleaks/TruffleHog, gate bloqueante |
| Dependencias | 0 vulnerabilidades críticas o altas sin excepción documentada | Dependabot + OWASP DC, gate bloqueante |
| Atestación de cliente | App Check / Play Integrity en todos los endpoints sensibles | Test de rechazo sin token |
| Mínimo privilegio | Cada servicio con su propia identidad y permisos mínimos | Revisión IAM por sprint |
| OWASP ASVS | Nivel 2 en Fase 3 | Pentest EN-1507 |
| Aislamiento multi-tenant | 0 accesos cruzados | Suite de pruebas negativas obligatoria en CI |

## RNF-05 · Privacidad

| Control | Umbral | Verificación |
|---|---|---|
| Minimización | Solo se recogen datos con finalidad declarada y consentida | Revisión de mapa de datos por sprint |
| Consentimiento | Granular, versionado, revocable, con evidencia de fecha y versión | Test de flujo + auditoría legal |
| Datos biométricos | Autorización **previa, expresa, informada y cualificada** (Ley 1581 CO + criterio SIC) | Revisión jurídica antes de Fase 2 |
| Retención de imágenes de captura | Borrado ≤ 24 h salvo consentimiento explícito de conservación | Job diario + reporte de huérfanos |
| Derechos del titular | Consulta, rectificación, revocación y supresión en ≤ 15 días hábiles | Flujo probado end-to-end |
| Logs | 0 datos personales o corporales en logs | Redacción automática + test de log |
| Compartir | Activos compartidos sin medidas, ubicación ni metadatos EXIF (RN-008) | Test de sanitización |
| Menores | Flujos corporales bloqueados para < 18 años | Test de flujo |

## RNF-06 · Accesibilidad

| Control | Umbral | Verificación |
|---|---|---|
| Contraste de texto | ≥ 4,5:1 normal, ≥ 3:1 grande | Test automático de tokens + axe |
| Elementos no textuales | ≥ 3:1 | axe |
| Área táctil | ≥ 48 dp / 44 px | Lint de UI |
| Lectores de pantalla | Flujos críticos navegables con TalkBack y NVDA | Prueba manual por release + accessibility scanner en CI |
| Orden de foco y trampas | Sin trampas de foco; orden lógico | Test instrumentado |
| Texto escalable | Hasta 200% sin pérdida de función | Test de UI con escala |
| Movimiento | Respetar `prefers-reduced-motion` / animaciones reducidas del SO | Test de UI |
| Alternativa no visual | Toda información por color lleva ícono y texto | Revisión de diseño |
| Estándar | WCAG 2.1 AA en web y equivalente en móvil | Auditoría EN-1509 |

## RNF-07 · Funcionamiento sin conexión

| Control | Umbral | Verificación |
|---|---|---|
| Disponible offline | Perfil, medidas, avatar, favoritos, closet y catálogo reciente | Test instrumentado en modo avión |
| Escrituras offline | En cola con estado visible; sin pérdida silenciosa | Test de sincronización |
| Conflictos | Resolución determinista y explicable (última escritura del usuario gana en perfil) | Test unitario del resolver |
| Probador 2D offline | Funciona con activos ya cacheados | Test instrumentado |

## RNF-08 · Compatibilidad

| Control | Umbral | Verificación |
|---|---|---|
| Android | minSdk 26, targetSdk vigente −0/−1 | Matriz definida en Sprint 0 |
| Matriz de dispositivos | ≥ 8 modelos representativos (gama baja/media/alta) | Firebase Test Lab |
| ARCore/Depth | Habilitado por capacidad detectada, nunca asumido | Test de compatibilidad |
| Navegadores web | 2 últimas versiones de Chrome, Edge, Safari, Firefox | Playwright matrix |
| Web móvil | 360 px de ancho mínimo | Playwright viewport matrix |
| iOS | Fuera de alcance en Fases 1–2; evaluar en Fase 3 | ADR pendiente |

## RNF-09 · Observabilidad

| Control | Umbral | Verificación |
|---|---|---|
| Logs | Estructurados JSON, con `trace_id`, `tenant_id`, `user_ref` (seudónimo) | Revisión + test de formato |
| Trazas | Distribuidas, con propagación entre servicios (OpenTelemetry) | Verificación en preproducción |
| Métricas | RED (rate, errors, duration) por endpoint; USE por recurso | Dashboard obligatorio por servicio |
| SLO y error budget | Definidos antes de producción; alerta al 50% de consumo | EN-1506 |
| Runbook | Toda alerta apunta a un runbook accionable | Revisión: 0 alertas sin runbook |
| Datos sensibles | 0 en telemetría | Test de redacción |

## RNF-10 · Mantenibilidad

| Control | Umbral | Verificación |
|---|---|---|
| Cobertura de pruebas del dominio | ≥ 85% en módulos de negocio (sizing, tenancy, pricing, inventory) | Gate en CI |
| Cobertura global | ≥ 70% líneas | Gate en CI |
| Complejidad ciclomática | ≤ 10 por método; excepciones justificadas | Detekt / SonarQube |
| Deuda técnica | 0 issues bloqueantes o críticos nuevos por PR | Sonar quality gate |
| Contratos de API | OpenAPI versionado; verificación de cambios incompatibles | Gate en CI (AUT-05) |
| Migraciones | Versionadas, reversibles, validadas antes del deploy | Flyway + gate |
| Documentación viva | Los `.md` de este repositorio se actualizan en el mismo PR que el cambio | Checklist de PR |
| Dependencias directas | Ninguna sin dueño identificado y política de actualización | Revisión trimestral |

## RNF-11 · Escalabilidad

| Control | Umbral | Verificación |
|---|---|---|
| Servicios de aplicación | Sin estado; escalado horizontal | Prueba de carga con 2+ réplicas |
| Almacenamiento de activos | Externo (object storage + CDN), nunca en el filesystem del servicio | Revisión de arquitectura |
| Paginación | Obligatoria por cursor en toda colección; sin `LIMIT` sin `ORDER BY` estable | Revisión de código + lint |
| Colas | Toda operación > 2 s va a cola asíncrona | Revisión |
| Límites por tenant | Cuotas aplicadas en el borde (rate limit por tenant y por plan) | Test de cuota |
| Objetivo de carga Fase 3 | 500 tenants, 200k usuarios activos/mes, 50 req/s sostenidos | Prueba de carga antes de lanzamiento |

## RNF-12 · Continuidad y recuperación

| Control | Umbral | Verificación |
|---|---|---|
| RPO | ≤ 15 min (Fase 3) | Configuración PITR + evidencia |
| RTO | ≤ 4 h (Fase 3) | **Simulacro de restauración obligatorio** antes del lanzamiento |
| Backups | Diarios cifrados + retención 30 días + verificación de integridad | Job + reporte |
| Simulacro | ≥ 1 por semestre con acta | EN-1508 |

## RNF-13 · Calidad de IA

| Control | Umbral | Verificación |
|---|---|---|
| Relevancia de outfits | ≥ 70% aprobados por panel de evaluación | Suite de evaluación (AUT-12) |
| Cumplimiento de restricciones | 100% (nunca recomienda algo excluido por el usuario) | Suite de evaluación, gate duro |
| Disponibilidad de stock | 100% de SKU recomendados con stock vigente | Test de integración |
| Seguridad de contenido | 0 salidas ofensivas o inapropiadas en el conjunto de evaluación | Suite de evaluación |
| Latencia p95 del Stylist | ≤ 4 s con respuesta parcial en streaming ≤ 1,5 s | k6 + instrumentación |
| Costo por consulta | Dentro del presupuesto del plan del tenant | EN-1606 |
| Sesgo | Métricas desagregadas por tipo de cuerpo, tono de piel y gama de dispositivo | EN-0616 |
| Reproducibilidad | Toda salida guarda versión de modelo, prompt y reglas (RN-015) | Test de trazabilidad |

## RNF-14 · Localización e internacionalización

| Control | Umbral | Verificación |
|---|---|---|
| Idioma | es-CO primero; arquitectura lista para en y pt | 0 cadenas embebidas en código (lint) |
| Unidades | cm/pulgadas y kg/lb configurables por usuario | Test de conversión |
| Moneda y formato | Por tenant y por país (COP, EUR, USD) | Test de formato |
| Tallas | Sistema de tallas por país (CO/EU/US/UK) declarado por tabla | Modelo de datos |
| Zona horaria | Todo en UTC en persistencia; conversión en presentación | Lint + revisión |

## RNF-15 · Costo (FinOps)

| Control | Umbral | Verificación |
|---|---|---|
| Fase 1 | **USD 0/mes** de infraestructura | Revisión mensual de facturación |
| Costo variable por usuario activo (Fase 3) | Dentro del margen objetivo por plan | EN-1606, tablero FinOps |
| Costo de IA por tenant | Cuota + alerta al 80% + corte configurable al 100% | Test de cuota |
| Almacenamiento de imágenes | Borrado automático reduce el crecimiento a activos de catálogo | Job de retención |

---

## Cómo se convierte esto en trabajo

Cada RNF con umbral debe tener **al menos una prueba automatizada** o un **gate de CI**. La tabla de
mapeo RNF → automatización está en [15-estrategia-de-pruebas.md](15-estrategia-de-pruebas.md).
Un RNF sin gate se registra como riesgo abierto en [19-riesgos.md](19-riesgos.md).
