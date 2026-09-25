package com.synaptia.smartmoda.domain.auth

/**
 * Resultado de evaluar una contrasena propuesta.
 *
 * Los motivos se devuelven TODOS, no solo el primero: obligar a alguien a descubrir sus
 * problemas de uno en uno, reintento tras reintento, es la via mas rapida a que elija
 * "Password123!" y se rinda.
 */
sealed interface PasswordVerdict {
    data object Acceptable : PasswordVerdict
    data class Rejected(val reasons: List<Reason>) : PasswordVerdict

    enum class Reason {
        TOO_SHORT,

        /** Aparece en filtraciones conocidas. Es el motivo mas importante de todos. */
        BREACHED,

        /** Contiene el correo o el nombre de la persona. Lo primero que prueba un atacante. */
        CONTAINS_PERSONAL_DATA,

        /** Repeticion o secuencia trivial: "aaaaaaaaaaaa", "123456789012". */
        TRIVIAL_PATTERN,
    }
}

/**
 * Si una contrasena aparece en filtraciones conocidas.
 *
 * La implementacion consulta la API de Pwned Passwords con k-anonimato: envia los 5 primeros
 * caracteres del hash SHA-1 y recibe todos los sufijos que empiezan igual. La contrasena
 * nunca sale del dispositivo, ni siquiera su hash completo.
 *
 * Se declara como interfaz para que el dominio no dependa de red ni de ninguna funcion de
 * hash de plataforma, y para que las pruebas no necesiten conexion.
 */
fun interface BreachChecker {
    /** @return true si la contrasena aparece en alguna filtracion conocida. */
    suspend fun isBreached(password: String): Boolean
}

/**
 * Reglas de contrasena.
 *
 * Deliberadamente SIN reglas de composicion (una mayuscula, un digito, un simbolo).
 * NIST SP 800-63B recomienda no usarlas: producen contrasenas predecibles del tipo
 * Password1! y no suben la entropia real. Lo que si funciona es longitud minima generosa
 * mas lista de contrasenas filtradas, que es lo que hay aqui.
 *
 * POR QUE ESTO EXISTE, ademas de la configuracion de Supabase: el rechazo de contrasenas
 * filtradas del panel solo se aplica en plan Pro o superior. En el plan gratuito el
 * interruptor se ve encendido pero no actua. Esta politica lo compensa desde el cliente.
 *
 * Y comprobarlo en el cliente SI es valido aqui, al reves que en [BruteForcePolicy]: el
 * proposito no es frenar a un atacante, es ayudar a quien elige la contrasena. En ese
 * momento el usuario no es el adversario.
 */
object PasswordPolicy {

    /** Coincide con password_min_length en Supabase. Ver tools/configure_supabase_auth.py. */
    const val MIN_LENGTH = 12

    /**
     * Evalua sin consultar filtraciones. Sirve para validar mientras se escribe, sin
     * lanzar una peticion de red en cada tecla.
     */
    fun evaluateLocally(password: String, personalData: Collection<String> = emptyList()): PasswordVerdict {
        val reasons = mutableListOf<PasswordVerdict.Reason>()

        if (password.length < MIN_LENGTH) {
            reasons += PasswordVerdict.Reason.TOO_SHORT
        }
        if (containsPersonalData(password, personalData)) {
            reasons += PasswordVerdict.Reason.CONTAINS_PERSONAL_DATA
        }
        if (isTrivialPattern(password)) {
            reasons += PasswordVerdict.Reason.TRIVIAL_PATTERN
        }

        return if (reasons.isEmpty()) PasswordVerdict.Acceptable else PasswordVerdict.Rejected(reasons)
    }

    /**
     * Evaluacion completa, incluida la consulta de filtraciones.
     *
     * Se llama al confirmar, no en cada pulsacion. Si la consulta falla, se acepta con lo
     * que se pudo comprobar: dejar a alguien sin poder registrarse porque un servicio de
     * terceros esta caido es peor que aceptar una contrasena que quiza este filtrada.
     */
    suspend fun evaluate(
        password: String,
        personalData: Collection<String> = emptyList(),
        breachChecker: BreachChecker? = null,
    ): PasswordVerdict {
        val local = evaluateLocally(password, personalData)
        val localReasons = (local as? PasswordVerdict.Rejected)?.reasons ?: emptyList()

        val breached = breachChecker?.let {
            runCatching { it.isBreached(password) }.getOrDefault(false)
        } ?: false

        val reasons = if (breached) localReasons + PasswordVerdict.Reason.BREACHED else localReasons
        return if (reasons.isEmpty()) PasswordVerdict.Acceptable else PasswordVerdict.Rejected(reasons)
    }

    private fun containsPersonalData(password: String, personalData: Collection<String>): Boolean {
        val lower = password.lowercase()
        return personalData
            .flatMap { it.lowercase().split("@", ".", " ", "-", "_") }
            .filter { it.length >= 4 }
            .any { lower.contains(it) }
    }

    private fun isTrivialPattern(password: String): Boolean {
        if (password.length < 2) return true

        // Un solo caracter, o dos alternandose: "aaaa", "ababab".
        if (password.toSet().size <= 2) return true

        // Una unidad corta repetida: "abcabcabcabc".
        for (unidad in 1..password.length / 3) {
            if (password.length % unidad != 0) continue
            val patron = password.take(unidad)
            if (password.chunked(unidad).all { it == patron }) return true
        }

        // Mayoritariamente secuencial. No basta exigir una secuencia estricta en todo el
        // texto: "123456789012345" da la vuelta en el 9 -> 0 y se escaparia, aunque es
        // exactamente el tipo de contrasena que hay que rechazar. Se mide la proporcion
        // de saltos de +-1 en lugar de exigir que lo sean todos.
        val saltos = password.map { it.code }.zipWithNext()
        val consecutivos = saltos.count { (a, b) -> b - a == 1 || a - b == 1 }
        return consecutivos.toDouble() / saltos.size >= 0.8
    }

    /**
     * Prefijo y sufijo del hash para la consulta con k-anonimato.
     *
     * Se separa del calculo del hash porque SHA-1 depende de la plataforma y el dominio debe
     * seguir siendo portable. La app calcula el hash; esta funcion decide que parte viaja.
     *
     * @param sha1Hex hash SHA-1 completo en hexadecimal
     * @return los 5 primeros caracteres (lo unico que sale del dispositivo) y el resto
     */
    fun splitForKAnonymity(sha1Hex: String): Pair<String, String> {
        require(sha1Hex.length == 40) { "Un SHA-1 en hexadecimal tiene 40 caracteres" }
        val upper = sha1Hex.uppercase()
        return upper.take(5) to upper.drop(5)
    }
}
