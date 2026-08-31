package com.synaptia.smartmoda.domain.sizing

/**
 * Nivel de confianza de una recomendacion.
 *
 * RN-016: una talla recomendada NUNCA se muestra sin este dato. Por eso no existe un valor nulo:
 * el tipo obliga a decidirlo.
 */
enum class Confidence {
    /** Todas las medidas clave caen dentro de una misma talla. */
    HIGH,

    /** Las medidas se reparten entre dos tallas contiguas, o falta una medida clave. */
    MEDIUM,

    /** Faltan varias medidas, o las que hay senalan tallas no contiguas. */
    LOW,

    /** No hay tabla aplicable. No se recomienda nada (RN-001). */
    NONE,
}

/** Por que no se pudo recomendar. Se muestra al usuario, asi que es informacion, no un codigo interno. */
enum class UnavailableReason {
    /** La marca no ha cargado tabla de tallas para esta categoria. */
    NO_SIZE_CHART,

    /** La tabla existe pero no tiene tallas cargadas. */
    EMPTY_SIZE_CHART,

    /** El perfil no tiene ninguna de las medidas que esta categoria necesita. */
    NO_RELEVANT_MEASUREMENTS,
}

/**
 * Resultado del motor de tallas.
 *
 * Es un tipo sellado y no una talla anulable a proposito: obliga a que la interfaz maneje
 * explicitamente el caso "no puedo recomendarte", que es justo el que RN-001 protege y el que
 * un `String?` invita a olvidar.
 */
sealed interface SizeRecommendation {

    /** Version de tabla y algoritmo con que se produjo. RN-015: sin esto no es reproducible. */
    val chartReference: String?

    data class Recommended(
        val size: String,
        val confidence: Confidence,
        /** Talla contigua sugerida cuando la persona esta entre dos. RN-017. */
        val alternative: String?,
        /** Explicacion legible por una persona, no un volcado de numeros. */
        val explanation: Explanation,
        override val chartReference: String,
        val algorithmVersion: String = SizeRecommendationPolicy.ALGORITHM_VERSION,
    ) : SizeRecommendation {
        init {
            require(confidence != Confidence.NONE) {
                "Una recomendacion emitida no puede tener confianza NONE; usa Unavailable"
            }
        }
    }

    data class Unavailable(
        val reason: UnavailableReason,
        override val chartReference: String? = null,
    ) : SizeRecommendation
}

/**
 * Explicacion estructurada.
 *
 * Se devuelve estructurada y no como texto ya redactado porque la traduccion y el tono son
 * responsabilidad de la capa de presentacion (RNF-14: cero cadenas embebidas en el dominio).
 */
data class Explanation(
    /** Medidas que encajaron limpiamente y en que talla. */
    val matched: Map<MeasurementKey, String>,
    /** Medidas que apuntaban a otra talla. Es lo que hace util la explicacion. */
    val conflicting: Map<MeasurementKey, String>,
    /** Medidas clave que el perfil no tiene. */
    val missing: Set<MeasurementKey>,
    /** Si la preferencia de ajuste desempato entre dos tallas contiguas. RN-002. */
    val fitPreferenceApplied: FitPreference?,
) {
    val isClean: Boolean get() = conflicting.isEmpty() && missing.isEmpty()
}
