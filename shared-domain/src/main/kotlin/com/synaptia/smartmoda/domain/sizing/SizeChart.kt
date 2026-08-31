package com.synaptia.smartmoda.domain.sizing

import com.synaptia.smartmoda.domain.common.Millimeters

/** Rango cerrado de una medida para una talla concreta. */
data class SizeRange(val min: Millimeters, val max: Millimeters) {
    init {
        require(min < max) { "Rango invalido: $min no es menor que $max" }
    }

    operator fun contains(m: Millimeters): Boolean = m >= min && m <= max

    /** 0 si esta dentro; si no, cuantos mm sobran o faltan. */
    fun distanceFrom(m: Millimeters): Int = when {
        m < min -> min - m
        m > max -> m - max
        else -> 0
    }
}

/** Una talla dentro de una tabla: la etiqueta y los rangos que la definen. */
data class SizeEntry(
    val label: String,
    val ranges: Map<MeasurementKey, SizeRange>,
)

/**
 * Tabla de tallas versionada de una marca y categoria.
 *
 * RN-015: una tabla NO se edita. Se publica una version nueva y se conserva la anterior, para que
 * una recomendacion emitida hace seis meses se pueda reproducir exactamente.
 *
 * [sizesInOrder] va de la mas pequena a la mas grande. Ese orden es lo que da sentido a
 * "estar entre dos tallas" y a "la talla contigua".
 */
data class SizeChartVersion(
    val chartId: String,
    val version: Int,
    val brand: String,
    val category: String,
    val sizeSystem: String,
    val keyMeasurements: Set<MeasurementKey>,
    val sizesInOrder: List<SizeEntry>,
) {
    init {
        require(version > 0) { "La version de una tabla empieza en 1" }
        require(keyMeasurements.isNotEmpty()) { "Una tabla necesita al menos una medida clave" }
    }

    val isUsable: Boolean get() = sizesInOrder.isNotEmpty()

    /** Referencia corta que se guarda con cada recomendacion, para trazabilidad (RN-015). */
    val reference: String get() = "$chartId@v$version"
}
