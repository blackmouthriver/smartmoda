package com.synaptia.smartmoda.domain.consent

import com.synaptia.smartmoda.domain.common.Timestamp

/**
 * Decision sobre si una finalidad puede ejercerse ahora mismo.
 *
 * Es un tipo sellado y no un booleano porque el motivo de la negativa determina que hace la
 * interfaz: pedir consentimiento, mostrar el texto nuevo, o explicar que esa funcion no esta
 * disponible para ese perfil. Un `false` obliga a adivinarlo.
 */
sealed interface ConsentDecision {

    data object Allowed : ConsentDecision

    data class Denied(val reason: DenialReason) : ConsentDecision

    enum class DenialReason {
        /** Nunca se pidio. La interfaz debe pedirlo antes de continuar. */
        NEVER_GRANTED,

        /** Se concedio y luego se revoco. Se puede volver a pedir. */
        REVOKED,

        /** La politica cambio: el consentimiento anterior ya no es informado. */
        POLICY_OUTDATED,

        /** RN-022 y ADR-0013. No se puede pedir: la funcion no existe para este perfil. */
        MINOR_NOT_ALLOWED,
    }
}

/**
 * Reglas de consentimiento del producto.
 *
 * Implementa:
 *  - RN-006  nada se captura antes del consentimiento especifico y vigente
 *  - RN-007  las finalidades opcionales se rechazan sin bloquear el nucleo
 *  - RN-021  revocar obliga a borrar lo derivado de esa finalidad
 *  - RN-022  los flujos corporales estan vetados para perfiles de menor
 *
 * Vive en shared-domain, no en la aplicacion, porque la misma regla debe gobernar el movil, el
 * servidor y el espejo. Una comprobacion que solo existe en la interfaz es una comprobacion que
 * se salta llamando a la API.
 */
object ConsentPolicy {

    const val POLICY_VERSION_CURRENT = "2026-09-25"

    /**
     * RN-006: la pregunta que hay que hacer ANTES de abrir la camara o guardar una medida.
     *
     * @param currentPolicy version vigente del documento. Si el registro es de una anterior,
     *   el consentimiento existe pero ya no es informado respecto al texto nuevo.
     */
    fun canExercise(
        purpose: ConsentPurpose,
        state: ConsentState,
        at: Timestamp,
        currentPolicy: PolicyVersion = PolicyVersion(POLICY_VERSION_CURRENT),
    ): ConsentDecision {
        // RN-022: se comprueba primero. Para un menor no es que falte consentimiento, es que
        // la finalidad no esta disponible, asi que la interfaz no debe ni ofrecer pedirlo.
        if (!state.isAdult && purpose.isBlockedForMinors) {
            return ConsentDecision.Denied(ConsentDecision.DenialReason.MINOR_NOT_ALLOWED)
        }

        val record = state.latestFor(purpose)
            ?: return ConsentDecision.Denied(ConsentDecision.DenialReason.NEVER_GRANTED)

        if (!record.isActive(at)) {
            val reason = if (record.isRevoked) {
                ConsentDecision.DenialReason.REVOKED
            } else {
                ConsentDecision.DenialReason.NEVER_GRANTED
            }
            return ConsentDecision.Denied(reason)
        }

        if (record.policyVersion != currentPolicy) {
            return ConsentDecision.Denied(ConsentDecision.DenialReason.POLICY_OUTDATED)
        }

        return ConsentDecision.Allowed
    }

    /**
     * RN-007: el nucleo contratado sigue disponible aunque se rechace todo lo opcional.
     *
     * Devuelve siempre true a proposito, y tiene una prueba que lo fija. Puede parecer una
     * funcion vacia; no lo es: es el punto donde un cambio futuro que condicione el catalogo a
     * aceptar marketing rompe una prueba con nombre de regla, en vez de colarse en una revision.
     */
    fun coreExperienceAvailable(@Suppress("UNUSED_PARAMETER") state: ConsentState): Boolean = true

    /**
     * Finalidades que faltan para poder usar el probador.
     *
     * No bloquea nada: sirve para que la interfaz sepa que pedir en el momento oportuno, en
     * lugar de pedirlo todo de golpe en el registro, que es lo que hace que la gente acepte
     * sin leer.
     */
    fun missingForTryOn(state: ConsentState, at: Timestamp): Set<ConsentPurpose> =
        setOf(ConsentPurpose.BODY_IMAGE, ConsentPurpose.BODY_MEASUREMENT)
            .filter { canExercise(it, state, at) !is ConsentDecision.Allowed }
            .toSet()

    /**
     * RN-021: que hay que borrar cuando se revoca una finalidad.
     *
     * Los identificadores corresponden a los conjuntos declarados en contracts/data-map.yaml.
     * Tenerlo en el dominio y no en un documento aparte es lo que permite probar que la
     * revocacion borra lo que debe.
     */
    fun derivedDataToErase(purpose: ConsentPurpose): Set<String> = when (purpose) {
        ConsentPurpose.BODY_IMAGE -> setOf("foto_de_captura", "landmarks_de_pose")
        ConsentPurpose.BODY_MEASUREMENT -> setOf("BodyProfileEntity")
        ConsentPurpose.FACE_IMAGE -> setOf("avatar_face")
        ConsentPurpose.SHARE_ASSETS -> setOf("shared_try_on_assets")
        ConsentPurpose.MARKETING -> setOf("marketing_audience")
        ConsentPurpose.OPTIONAL_ANALYTICS -> setOf("optional_telemetry")
    }

    /** RN-021: plazo maximo para que el borrado sea efectivo. */
    const val ERASURE_DEADLINE_MILLIS: Long = 72 * Timestamp.HOUR
}
