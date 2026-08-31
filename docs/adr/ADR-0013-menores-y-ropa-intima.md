# ADR-0013 · Menores de edad y ropa íntima: qué entra y qué no

- **Estado:** Propuesta — **requiere validación jurídica antes de implementar**
- **Fecha:** 2026-08-31
- **Decide:** PO + Privacidad + asesoría legal externa
- **Resuelve:** `D-08` del `Backlog_Ejecutable`, que estaba pendiente

## Contexto

El sponsor confirma el alcance comercial:

> *"si lo usará menos de 18 en tiendas físicas, todo tipo de clientes, desde ropa infantil hasta
> adultos, para ropa íntima revisar alcance"*

Es una decisión de negocio razonable: la ropa infantil es una categoría grande y ninguna tienda
familiar querría un espejo que solo sirve para la mitad de su catálogo. Y la ropa íntima es donde la
talla más falla, así que es donde el producto más valor podría dar.

Pero aquí conviene separar dos cosas que suenan iguales y no lo son:

| | Vender ropa infantil | Medir el cuerpo de un menor con una cámara |
|---|---|---|
| Qué implica | Catálogo, tallas, stock, compra por parte del adulto | Tratamiento de datos corporales de un menor |
| Marco legal | El habitual del comercio | Ley 1581 art. 7 + Decreto 1377: el tratamiento de datos de menores es **excepcional**, debe responder a su interés superior y respetar sus derechos fundamentales |
| Riesgo | Bajo | **El más alto de todo el proyecto** |

Lo mismo pasa con la ropa íntima: recomendar una talla de sujetador y superponer ropa interior sobre
la imagen real de una persona en una pantalla de tienda son dos productos distintos.

## Decisión

Se separa por **capacidad**, no por categoría. La categoría entra completa; lo que se restringe es
qué se puede hacer con el cuerpo de quién.

### Matriz de alcance

| Capacidad | Adulto, móvil | Adulto, espejo | Menor, móvil | Menor, espejo |
|---|---|---|---|---|
| Ver catálogo (incluida ropa infantil e íntima) | Sí | Sí | Sí | Sí |
| Recomendación de talla por **medidas introducidas a mano** | Sí | Sí | Sí (las introduce el adulto) | Sí (las introduce el adulto) |
| Recomendación de talla **por talla/edad/estatura** | Sí | Sí | Sí | Sí |
| **Medición automática por cámara** | Sí | Sí | **No** | **No** |
| Probador visual sobre **avatar** | Sí | Sí | **No** | **No** |
| Probador visual sobre **foto o cámara en vivo** | Sí | Sí | **No** | **No** |
| Lo anterior con **ropa íntima** | Solo avatar, nunca foto ni cámara | **No** | **No** | **No** |

### 1. Ropa infantil: la categoría entra, la cámara no

La forma en que realmente se compra ropa infantil resuelve el problema sin perder negocio: **el adulto
compra, y el adulto conoce las medidas del niño**.

- Nueva historia `US-0209` · **Perfiles de allegados**: el adulto guarda en su dispositivo las medidas
  de sus hijos u otras personas, introducidas a mano o por edad/estatura, y recibe recomendación de
  talla para ellos. Todo local y cifrado, igual que su propio perfil ([ADR-0003](ADR-0003-almacenamiento-perfil-corporal.md)).
- El catálogo infantil, sus tablas de talla por edad/estatura y su stock funcionan sin ninguna
  restricción.
- **Ningún flujo de cámara se activa para un perfil marcado como menor.** Es una regla de dominio
  (RN-022 ampliada), no una opción de configuración que un tenant pueda desactivar.

Con esto se vende ropa infantil desde el MVP, que es lo que el negocio necesita, sin procesar la
imagen de un solo niño.

### 2. El espejo y los menores

El espejo mide a quien se pare delante. Es su naturaleza, y es donde está el riesgo.

Controles:

1. **La cámara arranca solo tras aceptar el aviso**, y el aviso declara que el sistema es para
   personas mayores de edad (`EN-1410`).
2. **Modo acompañante**: si quien compra es un adulto para un menor, el espejo ofrece explícitamente
   elegir prendas infantiles **por talla, edad o estatura**, sin medición.
3. **Detección de indicios de menor** con degradación segura: si la estimación sugiere una estatura o
   proporciones fuera del rango adulto, el espejo **no calcula medidas ni recomienda talla**; pasa a
   modo catálogo. Es deliberadamente conservador: prefiere fallar con un adulto bajo que procesar a un
   niño.
4. La sesión es anónima y efímera de todos modos (`EN-1408`), lo que reduce —pero **no elimina**— la
   exposición: procesar la imagen sigue siendo tratamiento aunque no se almacene.

> **Este punto no se implementa sin concepto jurídico escrito.** Una tienda familiar con un espejo que
> mide cuerpos es exactamente el escenario que una autoridad de protección de datos revisa primero.
> `EN-1410` ya bloquea el despliegue sin revisión legal; este ADR añade el caso de los menores como
> parte obligatoria de esa revisión.

### 3. Ropa íntima

La talla es donde más falla y donde más valor daría. La imagen es donde más daño puede hacer.

**Entra:**
- Catálogo completo de ropa íntima, con sus tablas de talla (incluida la de sujetador, que es un
  sistema de tallas propio y merece su propio modelo).
- Recomendación de talla explicable, que es el valor real de la categoría.
- Visualización sobre **avatar estilizado y no anatómico**, en el dispositivo personal de un adulto.

**No entra:**
- Superposición sobre **foto real o cámara en vivo**, en ningún canal. Genera una imagen de una persona
  concreta en ropa interior; que se procese en el dispositivo no cambia lo que la imagen es.
- Ropa íntima **en el espejo de tienda**, en ninguna forma. Es una pantalla grande en un espacio
  compartido.
- Cualquier combinación de ropa íntima con un perfil de menor.

**Regla nueva RN-037:** *la ropa íntima se visualiza únicamente sobre avatar estilizado, en el
dispositivo personal de un usuario adulto. Nunca sobre foto, nunca sobre cámara en vivo, nunca en el
espejo de tienda, nunca con un perfil de menor.*

Esto se implementa como atributo de la categoría (`requires_stylized_only`), verificado en el dominio,
no en la interfaz. Un tenant no puede desactivarlo.

## Consecuencias

**Positivas**
- Se vende ropa infantil y ropa íntima desde el MVP: la categoría completa, que es lo que pide el
  negocio.
- Se evita el escenario de mayor riesgo regulatorio sin renunciar a mercado.
- `US-0209` (perfiles de allegados) es además una función útil por sí misma: comprar para la familia
  es un caso de uso frecuente que ningún competidor resuelve bien.
- La regla de ropa íntima da una posición defendible ante un cliente empresarial, que va a preguntar.

**Negativas**
- El espejo no mide a menores, así que en una tienda infantil da menos valor que en una de adultos.
  Es una limitación real, y hay que decirla al vender.
- La detección de indicios de menor tendrá falsos positivos: algún adulto de baja estatura no obtendrá
  recomendación. Se prefiere ese error al contrario.
- La ropa íntima sin probador visual sobre foto pierde parte del atractivo demostrativo.
- `US-0209` es trabajo nuevo: **5 SP**.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| Permitir medición de menores con consentimiento parental en pantalla | En una tienda no hay forma de verificar que quien acepta es el representante legal. Un consentimiento no verificable no es consentimiento |
| Prohibir la ropa infantil por completo | Renuncia a una categoría grande sin necesidad: el problema era la cámara, no la ropa |
| Permitir ropa íntima sobre foto solo para adultos con doble consentimiento | El consentimiento no cambia lo que es la imagen resultante, ni el riesgo si el dispositivo se pierde o la imagen se comparte por error |
| Dejar la decisión a cada tenant | Convierte una decisión de responsabilidad de la plataforma en una casilla de configuración. La plataforma responde igual |

## Acciones pendientes

1. **Concepto jurídico** sobre tratamiento de imagen de menores en establecimiento comercial. Bloquea
   el despliegue del espejo (`EN-1410`).
2. **Modelo de tallas de sujetador**, que no encaja en el esquema genérico de `size_chart` y necesita
   su propia estructura (banda + copa).
3. **Umbral de la detección de indicios de menor**: lo fija producto con criterio conservador, y se
   valida en `SP-1405`.
