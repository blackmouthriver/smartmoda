package com.synaptia.smartmoda.domain.auth

import com.synaptia.smartmoda.domain.common.Timestamp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class BruteForcePolicyTest {

    private val ahora = Timestamp(1_700_000_000_000)

    private fun fallos(n: Int, desde: Long = 60 * Timestamp.SECOND): List<AuthAttempt> =
        (1..n).map { AuthAttempt(ahora + (-desde + it * Timestamp.SECOND), succeeded = false) }

    @Test
    fun `sin intentos previos se permite entrar`() {
        BruteForcePolicy.evaluate(emptyList(), ahora) shouldBe AuthGate.Allow
    }

    @Test
    fun `el primer fallo no hace esperar`() {
        // Equivocarse de tecla una vez es normal. Castigarlo empeora la experiencia sin
        // frenar a nadie: un atacante automatizado no nota un segundo.
        val g = BruteForcePolicy.evaluate(fallos(1), ahora)

        g.shouldBeInstanceOf<AuthGate.Delay>()
        g.millis shouldBe 0
    }

    @Test
    fun `el retroceso crece de forma exponencial`() {
        BruteForcePolicy.backoffFor(1) shouldBe 0
        BruteForcePolicy.backoffFor(2) shouldBe 1_000
        BruteForcePolicy.backoffFor(3) shouldBe 2_000
        BruteForcePolicy.backoffFor(4) shouldBe 4_000
        BruteForcePolicy.backoffFor(5) shouldBe 8_000
    }

    @Test
    fun `el retroceso tiene tope`() {
        // Sin tope, el usuario legitimo que olvido su contrasena queda fuera durante horas
        // mientras el atacante sigue igual: solo estorba a quien no ataca.
        BruteForcePolicy.backoffFor(50) shouldBe BruteForcePolicy.MAX_BACKOFF_MILLIS
    }

    @Test
    fun `a partir del quinto fallo se exige un desafio adicional`() {
        BruteForcePolicy.evaluate(fallos(5), ahora) shouldBe AuthGate.Challenge
        BruteForcePolicy.evaluate(fallos(9), ahora) shouldBe AuthGate.Challenge
    }

    @Test
    fun `diez fallos en la ventana bloquean la cuenta`() {
        val g = BruteForcePolicy.evaluate(fallos(10), ahora)

        g.shouldBeInstanceOf<AuthGate.Locked>()
        g.until.isAfter(ahora) shouldBe true
    }

    @Test
    fun `el bloqueo se levanta cuando los fallos salen de la ventana`() {
        // Un bloqueo permanente convierte la fuerza bruta en denegacion de servicio: bastaria
        // fallar diez veces contra la cuenta de alguien para dejarlo fuera para siempre.
        //
        // La expiracion no necesita trabajo programado: al ser WINDOW y LOCKOUT iguales, el
        // bloqueo termina justo cuando los fallos dejan de contarse.
        val historia = fallos(10)
        val despues = ahora + BruteForcePolicy.LOCKOUT_MILLIS + Timestamp.MINUTE

        BruteForcePolicy.evaluate(historia, despues) shouldBe AuthGate.Allow
    }

    @Test
    fun `dentro del bloqueo se informa cuando se puede reintentar`() {
        val g = BruteForcePolicy.evaluate(fallos(10), ahora)

        g.shouldBeInstanceOf<AuthGate.Locked>()
        // No basta con negar el acceso: dejar al usuario adivinando cuando reintentar es
        // lo que hace que acabe llamando a soporte.
        (g.until.since(ahora) <= BruteForcePolicy.LOCKOUT_MILLIS) shouldBe true
    }

    @Test
    fun `un acceso correcto limpia los fallos previos`() {
        val historia = fallos(7) + AuthAttempt(ahora + (-Timestamp.SECOND), succeeded = true)

        BruteForcePolicy.evaluate(historia, ahora) shouldBe AuthGate.Allow
    }

    @Test
    fun `los fallos fuera de la ventana no cuentan`() {
        val antiguos = (1..12).map {
            AuthAttempt(ahora + (-2 * Timestamp.HOUR + it * Timestamp.SECOND), succeeded = false)
        }

        BruteForcePolicy.evaluate(antiguos, ahora) shouldBe AuthGate.Allow
    }

    @Test
    fun `los fallos se cuentan por cuenta, no por momento de llegada`() {
        // La historia llega desordenada: quien ataca no respeta el orden, y el almacen puede
        // devolver las filas como quiera.
        val desordenados = fallos(10).shuffled()

        BruteForcePolicy.evaluate(desordenados, ahora)
            .shouldBeInstanceOf<AuthGate.Locked>()
    }

    // ---------------------------------------------------------------- credential stuffing
    @Test
    fun `se detecta el ataque distribuido por tasa de fallo agregada`() {
        // Muchas IP contra muchas cuentas con pocos intentos cada una: ningun contador
        // individual se dispara. Solo se ve en la tasa global.
        BruteForcePolicy.isUnderDistributedAttack(failureRate = 0.65, baseline = 0.08) shouldBe true
    }

    @Test
    fun `un dia con mas fallos de lo normal no es un ataque`() {
        BruteForcePolicy.isUnderDistributedAttack(failureRate = 0.15, baseline = 0.08) shouldBe false
    }

    @Test
    fun `una linea base muy baja no dispara falsos positivos`() {
        // Con baseline 0.02, cualquier ruido multiplica por tres. El minimo absoluto lo evita.
        BruteForcePolicy.isUnderDistributedAttack(failureRate = 0.10, baseline = 0.02) shouldBe false
    }

    @Test
    fun `una tasa fuera de rango es un error de programacion, no un ataque`() {
        try {
            BruteForcePolicy.isUnderDistributedAttack(failureRate = 1.5, baseline = 0.1)
            throw AssertionError("deberia haber fallado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }
}
