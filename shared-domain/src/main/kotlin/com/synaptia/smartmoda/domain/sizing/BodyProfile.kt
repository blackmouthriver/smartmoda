package com.synaptia.smartmoda.domain.sizing

import com.synaptia.smartmoda.domain.common.Millimeters

/** Dimension corporal medible. El conjunto relevante depende de la categoria de prenda. */
enum class MeasurementKey {
    HEIGHT, CHEST, WAIST, HIP, INSEAM, SHOULDER, SLEEVE, NECK, THIGH, FOOT_LENGTH
}

/** De donde salio una medida. Determina cual gana cuando hay dos (RN-005). */
enum class MeasurementSource {
    /** La escribio la persona. Es la fuente de mayor autoridad. */
    MANUAL,

    /** La estimo el sistema desde foto, camara o espejo. Siempre corregible. */
    ESTIMATED
}

data class Measurement(
    val value: Millimeters,
    val source: MeasurementSource,
)

/**
 * Preferencia de ajuste.
 *
 * RN-002: influye en la EXPLICACION y en el desempate entre dos tallas contiguas.
 * Nunca modifica las medidas de la persona.
 */
enum class FitPreference { SNUG, REGULAR, LOOSE }

/**
 * Perfil corporal.
 *
 * ADR-0003: este objeto vive cifrado en el dispositivo y no se envia al servidor.
 * El calculo de talla ocurre aqui, en el cliente, precisamente para que no tenga que viajar.
 */
data class BodyProfile(
    val measurements: Map<MeasurementKey, Measurement>,
    val fitPreference: FitPreference = FitPreference.REGULAR,
) {

    operator fun get(key: MeasurementKey): Millimeters? = measurements[key]?.value

    fun has(key: MeasurementKey): Boolean = measurements.containsKey(key)

    /**
     * Incorpora medidas nuevas respetando RN-005: una medida MANUAL nunca es reemplazada
     * por una ESTIMATED. Al reves si, porque corregir es el caso de uso esperado (US-0305).
     */
    fun mergeWith(incoming: Map<MeasurementKey, Measurement>): BodyProfile {
        val merged = measurements.toMutableMap()
        for ((key, new) in incoming) {
            val current = merged[key]
            val currentIsManual = current?.source == MeasurementSource.MANUAL
            val incomingIsEstimated = new.source == MeasurementSource.ESTIMATED
            if (currentIsManual && incomingIsEstimated) continue // RN-005
            merged[key] = new
        }
        return copy(measurements = merged)
    }

    companion object {
        val EMPTY = BodyProfile(emptyMap())

        fun ofManual(vararg pairs: Pair<MeasurementKey, Millimeters>): BodyProfile =
            BodyProfile(pairs.associate { it.first to Measurement(it.second, MeasurementSource.MANUAL) })
    }
}
