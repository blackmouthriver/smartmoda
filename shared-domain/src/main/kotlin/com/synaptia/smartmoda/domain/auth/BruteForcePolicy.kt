package com.synaptia.smartmoda.domain.auth

import com.synaptia.smartmoda.domain.common.Timestamp

/**
 * Lo que el sistema hace ante un intento de autenticacion.
 *
 * DONDE SE EVALUA IMPORTA MAS QUE LA REGLA. Esta politica solo protege si corre en el
 * SERVIDOR. Un retroceso aplicado en el cliente no defiende de nada: quien ataca llama a la
 * API directamente y nunca ejecuta la aplicacion. Se implementa aqui, en shared-domain, para
 * que la misma regla gobierne el hook de Supabase en la Fase 1 y core-api en la Fase 2, pero
 * el cliente no la usa para decidir: como mucho, para explicar al usuario lo que el servidor
 * ya decidio.
 */
sealed interface AuthGate {

    data object Allow : AuthGate

    /** Retroceso exponencial: hay que esperar antes de volver a intentar. */
    data class Delay(val millis: Long) : AuthGate

    /** Bloqueo temporal de la cuenta. Temporal a proposito: ver [LOCKOUT_MILLIS]. */
    data class Locked(val until: Timestamp) : AuthGate

    /** Desafio adicional (CAPTCHA o prueba de trabajo) por patron sospechoso. */
    data object Challenge : AuthGate
}

/** Un intento de autenticacion, con su resultado. */
data class AuthAttempt(
    val at: Timestamp,
    val succeeded: Boolean,
)

/**
 * Defensa contra fuerza bruta y credential stuffing.
 *
 * EN-1510. Las capas estan en docs/12-seguridad-y-privacidad.md seccion 3.
 *
 * Dos decisiones que conviene entender:
 *
 * 1. El retroceso y el bloqueo se cuentan POR CUENTA, no por IP. Quien ataca con una botnet
 *    rota la IP en cada intento, pero apunta siempre a la misma cuenta.
 *
 * 2. El bloqueo es TEMPORAL. Un bloqueo permanente convierte el ataque de fuerza bruta en un
 *    ataque de denegacion de servicio: basta con fallar diez veces contra la cuenta de alguien
 *    para dejarlo fuera indefinidamente.
 */
object BruteForcePolicy {

    /** Fallos dentro de [WINDOW_MILLIS] que disparan el bloqueo. */
    const val LOCKOUT_THRESHOLD = 10

    /** Ventana en la que se cuentan los fallos. */
    const val WINDOW_MILLIS: Long = 15 * Timestamp.MINUTE

    /**
     * Duracion del bloqueo, medida desde el ultimo fallo.
     *
     * Igual a [WINDOW_MILLIS] a proposito: el bloqueo termina exactamente cuando los fallos
     * salen de la ventana de conteo. Eso lo hace auto-reparable sin ningun trabajo programado
     * que limpie bloqueos caducados.
     *
     * Y es temporal por diseno. Uno permanente convertiria la fuerza bruta en denegacion de
     * servicio: bastaria fallar diez veces contra la cuenta de alguien para dejarlo fuera.
     */
    const val LOCKOUT_MILLIS: Long = 15 * Timestamp.MINUTE

    /** Tope del retroceso. Mas alla deja de disuadir y solo estorba al usuario legitimo. */
    const val MAX_BACKOFF_MILLIS: Long = 30 * Timestamp.SECOND

    /** Fallos consecutivos a partir de los cuales se exige un desafio adicional. */
    const val CHALLENGE_THRESHOLD = 5

    /**
     * Que hacer ante el proximo intento de esta cuenta.
     *
     * @param history intentos de ESTA cuenta, en cualquier orden
     * @param now momento de la evaluacion
     */
    fun evaluate(history: List<AuthAttempt>, now: Timestamp): AuthGate {
        val recent = history
            .filter { now.since(it.at) in 0..WINDOW_MILLIS }
            .sortedBy { it.at.epochMillis }

        // Un acceso correcto limpia la cuenta: el usuario demostro ser quien dice.
        val sinceLastSuccess = recent.takeLastWhile { !it.succeeded }
        val consecutiveFailures = sinceLastSuccess.size

        if (consecutiveFailures == 0) return AuthGate.Allow

        if (consecutiveFailures >= LOCKOUT_THRESHOLD) {
            // El bloqueo se levanta solo: WINDOW_MILLIS y LOCKOUT_MILLIS son iguales, asi que
            // cuando expira el bloqueo los fallos ya salieron de la ventana y `recent` los
            // descarta, devolviendo Allow unas lineas mas arriba. Escribir aqui una rama de
            // expiracion seria codigo inalcanzable.
            //
            // `until` se devuelve igualmente porque es informacion util: dice al usuario
            // cuando puede reintentar en lugar de dejarlo adivinando.
            return AuthGate.Locked(sinceLastSuccess.last().at + LOCKOUT_MILLIS)
        }

        if (consecutiveFailures >= CHALLENGE_THRESHOLD) return AuthGate.Challenge

        return AuthGate.Delay(backoffFor(consecutiveFailures))
    }

    /**
     * Retroceso exponencial: 0, 1, 2, 4, 8... segundos, con tope.
     *
     * El primer fallo no espera. Equivocarse de tecla una vez es lo normal, y castigarlo
     * empeora la experiencia sin frenar a nadie: un atacante automatizado no nota un segundo.
     */
    fun backoffFor(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 1) return 0
        val exponent = (consecutiveFailures - 2).coerceAtMost(20)
        val millis = Timestamp.SECOND shl exponent
        return millis.coerceAtMost(MAX_BACKOFF_MILLIS)
    }

    /**
     * Deteccion de credential stuffing a nivel global.
     *
     * El patron caracteristico son muchas IP contra muchas cuentas con pocos intentos en cada
     * una: ningun contador individual llega a dispararse. Solo se ve en la tasa agregada.
     *
     * @param failureRate proporcion de intentos fallidos en la ventana observada
     * @param baseline proporcion habitual del sistema, medida, no supuesta
     */
    fun isUnderDistributedAttack(failureRate: Double, baseline: Double): Boolean {
        require(failureRate in 0.0..1.0) { "Tasa fuera de rango: $failureRate" }
        require(baseline in 0.0..1.0) { "Linea base fuera de rango: $baseline" }
        // Se exige ademas un minimo absoluto: con una linea base muy baja, cualquier ruido
        // multiplicaria por tres sin que pase nada raro.
        return failureRate > baseline * 3 && failureRate > 0.30
    }
}
