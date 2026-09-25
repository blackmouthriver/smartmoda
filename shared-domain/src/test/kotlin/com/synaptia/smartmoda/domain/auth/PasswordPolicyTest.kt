package com.synaptia.smartmoda.domain.auth

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PasswordPolicyTest {

    private val siempreFiltrada = BreachChecker { true }
    private val nuncaFiltrada = BreachChecker { false }

    @Test
    fun `una contrasena larga y sin patrones es aceptable`() {
        PasswordPolicy.evaluateLocally("caballo grapa bateria") shouldBe PasswordVerdict.Acceptable
    }

    @Test
    fun `por debajo de la longitud minima se rechaza`() {
        val v = PasswordPolicy.evaluateLocally("corta123")

        v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
        v.reasons shouldContain PasswordVerdict.Reason.TOO_SHORT
    }

    @Test
    fun `no se exigen reglas de composicion`() {
        // Sin mayusculas, sin digitos, sin simbolos: y aun asi vale. NIST SP 800-63B
        // desaconseja las reglas de composicion porque producen Password1! y similares.
        PasswordPolicy.evaluateLocally("verano en cartagena") shouldBe PasswordVerdict.Acceptable
    }

    @Test
    fun `se rechaza si contiene el correo o el nombre`() {
        val v = PasswordPolicy.evaluateLocally(
            "jhonnatan2026xyz",
            personalData = listOf("jhonnatan@ejemplo.com", "Jhonnatan Bocanegra"),
        )

        v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
        v.reasons shouldContain PasswordVerdict.Reason.CONTAINS_PERSONAL_DATA
    }

    @Test
    fun `un fragmento personal muy corto no dispara falsos positivos`() {
        // "de" o "la" aparecen en cualquier frase; exigir 4 caracteres evita el ruido.
        val v = PasswordPolicy.evaluateLocally(
            "montanas de colombia",
            personalData = listOf("de la cruz"),
        )

        v shouldBe PasswordVerdict.Acceptable
    }

    @Test
    fun `se rechazan los patrones triviales`() {
        listOf("aaaaaaaaaaaaaa", "123456789012345", "abcdefghijklmn").forEach { trivial ->
            val v = PasswordPolicy.evaluateLocally(trivial)
            v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
            v.reasons shouldContain PasswordVerdict.Reason.TRIVIAL_PATTERN
        }
    }

    @Test
    fun `se acumulan todos los motivos, no solo el primero`() {
        // Descubrir los problemas de uno en uno, reintento tras reintento, es la via mas
        // rapida a que alguien se rinda y elija lo minimo que pase.
        val v = PasswordPolicy.evaluateLocally("aaaa", personalData = listOf("aaaa@x.com"))

        v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
        v.reasons shouldContain PasswordVerdict.Reason.TOO_SHORT
        v.reasons shouldContain PasswordVerdict.Reason.TRIVIAL_PATTERN
    }

    // ---------------------------------------------------------------- filtraciones
    @Test
    fun `una contrasena filtrada se rechaza aunque cumpla todo lo demas`() = runTest {
        val v = PasswordPolicy.evaluate("caballo grapa bateria", breachChecker = siempreFiltrada)

        v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
        v.reasons shouldContain PasswordVerdict.Reason.BREACHED
    }

    @Test
    fun `si no esta filtrada y cumple lo demas, se acepta`() = runTest {
        PasswordPolicy.evaluate("caballo grapa bateria", breachChecker = nuncaFiltrada) shouldBe
            PasswordVerdict.Acceptable
    }

    @Test
    fun `si el servicio de filtraciones falla no se bloquea el registro`() = runTest {
        // Dejar a alguien sin poder registrarse porque un tercero esta caido es peor que
        // aceptar una contrasena que quiza este filtrada. Se degrada, no se rompe (RN-019).
        val roto = BreachChecker { error("servicio caido") }

        PasswordPolicy.evaluate("caballo grapa bateria", breachChecker = roto) shouldBe
            PasswordVerdict.Acceptable
    }

    @Test
    fun `sin comprobador de filtraciones se evalua solo lo local`() = runTest {
        val v = PasswordPolicy.evaluate("caballo grapa bateria", breachChecker = null)

        v shouldBe PasswordVerdict.Acceptable
    }

    @Test
    fun `una contrasena corta y filtrada acumula ambos motivos`() = runTest {
        val v = PasswordPolicy.evaluate("corta", breachChecker = siempreFiltrada)

        v.shouldBeInstanceOf<PasswordVerdict.Rejected>()
        v.reasons shouldContain PasswordVerdict.Reason.TOO_SHORT
        v.reasons shouldContain PasswordVerdict.Reason.BREACHED
    }

    // ---------------------------------------------------------------- k-anonimato
    @Test
    fun `solo los cinco primeros caracteres del hash salen del dispositivo`() {
        // Esta es la garantia de privacidad de la consulta: con 5 caracteres el servicio
        // devuelve cientos de coincidencias y no puede saber cual es la nuestra.
        val sha1 = "5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8"

        val (prefijo, sufijo) = PasswordPolicy.splitForKAnonymity(sha1)

        prefijo shouldBe "5BAA6"
        prefijo.length shouldBe 5
        sufijo shouldBe "1E4C9B93F3F0682250B6CF8331B7EE68FD8"
        (prefijo + sufijo) shouldBe sha1
    }

    @Test
    fun `el hash se normaliza a mayusculas porque la API responde asi`() {
        val (prefijo, sufijo) = PasswordPolicy.splitForKAnonymity(
            "5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8")

        prefijo shouldBe "5BAA6"
        sufijo.take(4) shouldBe "1E4C"
    }

    @Test
    fun `un hash que no es SHA-1 se rechaza en vez de producir una consulta invalida`() {
        try {
            PasswordPolicy.splitForKAnonymity("demasiado-corto")
            throw AssertionError("deberia haber fallado")
        } catch (e: IllegalArgumentException) {
            (e.message ?: "").contains("40") shouldBe true
        }
    }

    @Test
    fun `la longitud minima coincide con la configurada en Supabase`() {
        // Si alguien cambia una y no la otra, el usuario recibe mensajes contradictorios:
        // la app acepta la contrasena y el servidor la rechaza.
        PasswordPolicy.MIN_LENGTH shouldBe 12
    }
}
