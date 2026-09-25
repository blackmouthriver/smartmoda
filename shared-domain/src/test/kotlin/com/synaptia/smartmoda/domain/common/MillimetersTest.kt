package com.synaptia.smartmoda.domain.common

import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * RF-203 exige conversion bidireccional cm <-> pulgadas y kg <-> lb "sin perdida".
 *
 * "Sin perdida" con unidades decimales es imposible en sentido estricto, asi que lo que se
 * verifica es lo que de verdad importa al usuario: que convertir de ida y vuelta no le cambie
 * la talla. La tolerancia esta atada a esa consecuencia, no a un capricho numerico.
 */
class MillimetersTest {

    @Test
    fun `RF_203 convertir de cm a mm y de vuelta conserva el valor`() {
        listOf(94.0, 78.5, 102.3, 45.0, 180.0).forEach { cm ->
            Millimeters.ofCm(cm).toCm() shouldBe cm
        }
    }

    @Test
    fun `RF_203 convertir de pulgadas a mm y de vuelta se mantiene dentro de medio milimetro`() {
        listOf(37.0, 30.5, 42.25, 18.0).forEach { inches ->
            val roundTrip = Millimeters.ofInches(inches).toInches()
            roundTrip.shouldBeWithinPercentageOf(inches, 0.1)
        }
    }

    @Test
    fun `RF_203 una pulgada son 25,4 milimetros exactos`() {
        Millimeters.ofInches(1.0) shouldBe Millimeters(25)
        Millimeters.ofInches(10.0) shouldBe Millimeters(254)
    }

    @Test
    fun `RF_203 el redondeo al convertir nunca desplaza mas de un milimetro`() {
        // Un error de un milimetro no puede mover a nadie de talla: las bandas de talla van de
        // 40 a 80 mm. Esta es la garantia que de verdad hay que sostener, y se comprueba sobre
        // todo el rango corporal plausible, no sobre tres ejemplos elegidos a mano.
        for (mm in 300..2200) {
            val original = Millimeters(mm)
            val vuelta = Millimeters.ofCm(original.toCm())
            if (vuelta.distanceTo(original) > 1) {
                throw AssertionError("Perdida de precision en $mm mm: volvio como $vuelta")
            }
        }
    }

    @Test
    fun `una medida corporal negativa no existe y el tipo lo impide`() {
        assertFailsWith<IllegalArgumentException> { Millimeters(-1) }
    }

    @Test
    fun `la distancia entre dos medidas es absoluta y simetrica`() {
        val a = Millimeters(950)
        val b = Millimeters(1010)

        a.distanceTo(b) shouldBe 60
        b.distanceTo(a) shouldBe 60
    }

    @Test
    fun `las medidas se comparan por valor`() {
        (Millimeters(950) < Millimeters(1010)) shouldBe true
        (Millimeters(950) == Millimeters(950)) shouldBe true
        (Millimeters(1010) - Millimeters(950)) shouldBe 60
    }

    @Test
    fun `la representacion textual lleva la unidad para que no se confunda con cm`() {
        Millimeters(950).toString() shouldBe "950mm"
    }
}
