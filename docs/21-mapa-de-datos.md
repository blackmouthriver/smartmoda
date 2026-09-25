# 21 · Mapa de datos

> `EN-1501` · Sprint 0 · Complementa el modelo de amenazas de
> [12 § 1](12-seguridad-y-privacidad.md)

El inventario de todo dato personal o de negocio que el sistema toca: qué es, dónde vive, cuánto
se retiene y con qué base legal.

**Este documento no es la fuente de verdad.** Lo es [`contracts/data-map.yaml`](../contracts/data-map.yaml),
que CI verifica contra el esquema real en cada PR. Aquí está el razonamiento; allí, el contrato.

---

## Por qué existe en formato verificable

Un mapa de datos escrito en prosa está desactualizado en tres sprints y nadie se entera hasta
que llega una auditoría o un incidente. `tools/check_data_map.py` comprueba en cada PR:

1. Toda tabla de `supabase/migrations/` está declarada en el mapa
2. Toda columna de esas tablas tiene clasificación
3. Toda `@Entity` de Room está declarada, con sus campos
4. **Ningún dato clasificado como crítico vive en un almacén del servidor**

El punto 4 convierte [ADR-0003](adr/ADR-0003-almacenamiento-perfil-corporal.md) de promesa en
garantía comprobable. Lo verifiqué inyectando una columna `chest_mm` en la tabla `stock`:

```
[FALLA] ADR-0003   stock.chest_mm   dato CRITICO en un almacen del servidor
```

Y si alguien intentara esquivarlo clasificando esa columna como *bajo*, el cambio queda en el
mapa firmado por una persona concreta en un commit. La decisión se vuelve visible, que es
exactamente lo que se puede auditar.

---

## Clasificación

| Nivel | Qué incluye | Dónde puede vivir |
|---|---|---|
| **Crítico** | Fotos de cuerpo, silueta, landmarks de pose, medidas, avatar, rostro | **Solo dispositivo**: Room cifrado, Keystore o memoria |
| **Alto** | Contraseñas, tokens, claves, datos de pago | Supabase Auth, bóveda, Android Keystore |
| **Medio** | Correo, nombre, historial de pruebas, favoritos, órdenes | Supabase, dispositivo, warehouse |
| **Bajo** | Catálogo, precios, tiendas | Sin restricción particular |

Bajo la Ley 1581 de 2012 y el criterio de la SIC, lo clasificado como crítico es dato sensible
con exigencia de autorización **previa, expresa, informada y cualificada**.

---

## Estado actual

| | Declarado | Real |
|---|---|---|
| Tablas en Supabase | 11 | 11 |
| Entidades de Room | 1 | 1 |
| Conjuntos previstos aún sin implementar | 6 | — |

Los cuatro conjuntos críticos, y ninguno está en el servidor:

| Dato | Dónde vive | Retención |
|---|---|---|
| `BodyProfileEntity` | Room cifrado en el dispositivo | Mientras exista la cuenta |
| Foto de captura | Solo memoria | Se descarta al terminar el procesamiento |
| Landmarks de pose | Solo memoria | Duración de la sesión |
| Sesión de espejo | Solo memoria | Se borra al terminar, con evidencia auditable |

---

## Tres decisiones del modelo que conviene entender

### La tabla de tallas no es un dato personal

`size_chart_entry` guarda rangos como *"pecho 920–979 mm"*. Eso es catálogo de una marca,
clasificado bajo. *"Tu pecho mide 950 mm"* es crítico y vive en otro sitio.

La distinción parece obvia escrita, pero es justo donde se confunden los inventarios: ambas
cosas son "medidas en milímetros".

### El perfil corporal no tiene columnas

`BodyProfileEntity` guarda `ciphertext`, `nonce`, `schemaVersion` y `updatedAt`. No hay
`chest`, `waist` ni `hip`.

Podría haberlas tenido, cifradas campo a campo. No las tiene porque **lo que no se puede
representar no se puede filtrar**: con columnas individuales, tarde o temprano alguien escribe
una consulta de depuración que las lee en claro.

### Los consentimientos se retienen más que el consentimiento

Cuando alguien revoca, sus datos derivados se borran en 72 horas (RN-021). Pero el **registro
del consentimiento** —finalidad, versión de política, texto mostrado, fecha, IP— se conserva
cinco años.

No es contradictorio: es la prueba de que en su momento hubo consentimiento válido. Sin esa
evidencia, ante una reclamación no se puede demostrar nada, y un consentimiento que no se puede
demostrar no existe a efectos prácticos. La base legal de esa retención es la obligación legal,
no el consentimiento.

---

## Retención, resumida

| Dato | Plazo | Mecanismo |
|---|---|---|
| Foto de captura | ≤ 24 h, en la práctica se descarta al instante | Nunca toca disco (RN-014) |
| Perfil corporal | Mientras exista la cuenta | Se borra con ella (US-0106) |
| Sesión de espejo | Duración de la sesión | Borrado verificable (`EN-1404`) |
| Consentimientos | 5 años tras la revocación | Obligación legal |
| Catálogo y tallas | No se borran | RN-015 exige reproducir recomendaciones antiguas |
| Datos de tenant purgado | Solo facturación y auditoría exigidas | `US-1702` |

---

## Lo que falta

Este mapa cubre lo que existe hoy más lo previsto con su sprint. Queda pendiente, y no bloquea
el Sprint 0:

1. **Revisión jurídica** del inventario completo antes de la Fase 2, cuando empiecen a tratarse
   imágenes de verdad.
2. **Registro de actividades de tratamiento** en el formato que exija la SIC, derivable de este
   archivo.
3. **Ampliar el mapa** conforme aparezcan datasets: cada tabla nueva lo exige, y CI lo recuerda.

---

## Cómo verificarlo

```bash
python tools/check_data_map.py
```

Corre también en CI en cada PR. Una tabla nueva sin clasificar rompe el build, que es la única
forma de que un inventario siga siendo cierto seis meses después.
