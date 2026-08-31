package com.synaptia.smartmoda.domain.common

/**
 * Toda medida corporal se guarda en milimetros enteros.
 *
 * Por que milimetros y no centimetros con decimales: las tablas de tallas se expresan en rangos
 * cerrados y los decimales flotantes producen comparaciones inestables justo en los bordes, que es
 * donde se decide una talla. Un entero en mm da precision de sobra y comparaciones exactas.
 *
 * La conversion a cm o pulgadas es asunto de presentacion (RF-203), no de dominio.
 */
@JvmInline
value class Millimeters(val value: Int) : Comparable<Millimeters> {

    init {
        require(value >= 0) { "Una medida corporal no puede ser negativa: $value" }
    }

    override fun compareTo(other: Millimeters): Int = value.compareTo(other.value)

    operator fun minus(other: Millimeters): Int = value - other.value

    /** Distancia absoluta a otra medida, en mm. */
    fun distanceTo(other: Millimeters): Int = kotlin.math.abs(value - other.value)

    override fun toString(): String = "${value}mm"

    companion object {
        fun ofCm(cm: Double): Millimeters = Millimeters(Math.round(cm * 10).toInt())
        fun ofInches(inches: Double): Millimeters = Millimeters(Math.round(inches * 25.4).toInt())
    }
}

fun Millimeters.toCm(): Double = value / 10.0
fun Millimeters.toInches(): Double = value / 25.4
