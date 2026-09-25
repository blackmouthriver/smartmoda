package com.synaptia.smartmoda.domain.common

/**
 * Marca de tiempo en milisegundos desde epoch, siempre UTC.
 *
 * Se define aqui en vez de usar java.time porque shared-domain debe poder compilarse fuera de
 * la JVM. Un Long envuelto no depende de ninguna plataforma y no pierde nada: el dominio no
 * hace aritmetica de calendario, solo compara instantes y mide intervalos.
 *
 * El formateo para mostrar es asunto de la capa de presentacion, que si conoce la zona horaria
 * y el idioma del usuario (RNF-14).
 */
@JvmInline
value class Timestamp(val epochMillis: Long) : Comparable<Timestamp> {

    override fun compareTo(other: Timestamp): Int = epochMillis.compareTo(other.epochMillis)

    operator fun plus(millis: Long): Timestamp = Timestamp(epochMillis + millis)

    /** Milisegundos transcurridos desde [other]. Negativo si [other] es posterior. */
    fun since(other: Timestamp): Long = epochMillis - other.epochMillis

    fun isAfter(other: Timestamp): Boolean = epochMillis > other.epochMillis

    override fun toString(): String = "Timestamp($epochMillis)"

    companion object {
        val EPOCH = Timestamp(0)

        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
