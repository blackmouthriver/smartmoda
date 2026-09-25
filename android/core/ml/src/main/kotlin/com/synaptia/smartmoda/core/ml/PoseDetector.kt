package com.synaptia.smartmoda.core.ml

/**
 * Abstraccion propia de deteccion de pose.
 *
 * Existe por el riesgo R-05: ML Kit Pose Detection esta en beta y su API puede cambiar o
 * retirarse. Acoplar el probador directamente a esa biblioteca significaria que un cambio
 * suyo rompe la funcionalidad central del producto.
 *
 * Esta interfaz es el seguro. Las implementaciones (ML Kit hoy, MediaPipe como alterna ya
 * prevista) viven en el paquete `impl` y nadie fuera de este modulo las conoce.
 *
 * RN-014: toda implementacion procesa EN EL DISPOSITIVO. Ninguna puede enviar el fotograma
 * a un servidor. No es una recomendacion: es la razon de que el contrato reciba un bitmap
 * y devuelva landmarks, sin ningun punto donde quepa una llamada de red.
 */
interface PoseDetector {

    /**
     * Detecta la pose en una imagen ya decodificada.
     *
     * @param image pixeles en el dispositivo; nunca se persiste ni se transmite
     * @return la pose detectada, o [PoseResult.NotDetected] si no hay una persona reconocible
     */
    suspend fun detect(image: ImageFrame): PoseResult

    /** Libera los recursos nativos del detector. Obligatorio al salir de la pantalla. */
    fun close()
}

/**
 * Fotograma independiente de la plataforma.
 *
 * Se define aqui y no como `android.graphics.Bitmap` para que las pruebas del dominio de
 * pose no necesiten un emulador, y para que una implementacion futura en otro canal
 * (el espejo de tienda, que es web) pueda reutilizar el contrato.
 */
data class ImageFrame(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
) {
    init {
        require(width > 0 && height > 0) { "Fotograma invalido: ${width}x$height" }
        require(rotationDegrees in setOf(0, 90, 180, 270)) {
            "Rotacion no soportada: $rotationDegrees"
        }
    }
}

/** Punto detectado, en coordenadas normalizadas 0..1 respecto al fotograma. */
data class Landmark(
    val type: LandmarkType,
    val x: Float,
    val y: Float,
    /** Confianza del detector para este punto concreto, 0..1. */
    val inFrameLikelihood: Float,
)

/**
 * Subconjunto de los 33 landmarks que el producto usa.
 *
 * Se declara solo lo que se necesita para silueta y medidas: pedir 33 puntos y usar 12
 * obliga a mantener 21 que nadie mira, y ata el contrato a la taxonomia de ML Kit.
 */
enum class LandmarkType {
    NOSE,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
}

sealed interface PoseResult {

    /**
     * Pose detectada con landmarks utilizables.
     *
     * @param confidence confianza agregada. Alimenta el nivel de confianza de la medida
     *   estimada, que a su vez alimenta el de la talla (RN-016). La cadena de confianza
     *   no se rompe en ningun punto: si aqui hay duda, llega hasta la pantalla.
     */
    data class Detected(
        val landmarks: List<Landmark>,
        val confidence: Float,
    ) : PoseResult {
        init {
            require(landmarks.isNotEmpty()) { "Una pose detectada necesita landmarks" }
            require(confidence in 0f..1f) { "Confianza fuera de rango: $confidence" }
        }

        operator fun get(type: LandmarkType): Landmark? = landmarks.firstOrNull { it.type == type }
    }

    /** No hay persona reconocible en el fotograma. Es un resultado esperado, no un error. */
    data object NotDetected : PoseResult

    /**
     * El detector no pudo ejecutarse.
     *
     * RN-019: quien recibe esto debe degradar a captura manual, nunca dejar al usuario
     * frente a una pantalla rota.
     */
    data class Failed(val reason: String) : PoseResult
}
