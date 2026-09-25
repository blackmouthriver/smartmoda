package com.synaptia.smartmoda.domain.consent

import com.synaptia.smartmoda.domain.common.Timestamp

/**
 * Finalidad concreta para la que se pide consentimiento.
 *
 * Se piden por separado a proposito. Un consentimiento global del tipo "acepto el tratamiento
 * de mis datos" no es especifico, y bajo Ley 1581 de 2012 el consentimiento para dato sensible
 * debe ser previo, expreso, informado y CUALIFICADO. Agrupar finalidades lo invalida.
 */
enum class ConsentPurpose {
    /** Captura de foto o video del cuerpo. Dato sensible. */
    BODY_IMAGE,

    /** Medidas corporales, derivadas o introducidas a mano. Dato sensible. */
    BODY_MEASUREMENT,

    /** Imagen facial para el avatar. Dato sensible, y el mas delicado (US-0307). */
    FACE_IMAGE,

    /** Comunicaciones comerciales. */
    MARKETING,

    /** Telemetria mas alla de la minima para operar. */
    OPTIONAL_ANALYTICS,

    /** Compartir una prueba fuera de la aplicacion. */
    SHARE_ASSETS,
    ;

    /**
     * RN-007: estas finalidades deben poder rechazarse sin bloquear el nucleo contratado.
     *
     * Las corporales no estan aqui, pero eso NO significa que sean obligatorias: significa que
     * sin ellas no hay probador. El usuario puede rechazarlas y seguir usando catalogo,
     * busqueda y tallas por tabla. Ver [coreExperienceAvailable].
     */
    val isOptional: Boolean
        get() = this == MARKETING || this == OPTIONAL_ANALYTICS || this == SHARE_ASSETS

    /** RN-022 y ADR-0013: finalidades vetadas para un perfil de menor de edad. */
    val isBlockedForMinors: Boolean
        get() = this == BODY_IMAGE || this == BODY_MEASUREMENT || this == FACE_IMAGE
}

/**
 * Version del documento de politica que el usuario acepto.
 *
 * Si la politica cambia, el consentimiento anterior deja de ser "informado" respecto al texto
 * nuevo. Por eso la version forma parte del registro y no es un detalle de auditoria.
 */
@JvmInline
value class PolicyVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "La version de politica no puede estar vacia" }
    }

    override fun toString(): String = value
}

/**
 * Registro de un consentimiento.
 *
 * [evidence] guarda lo que hace demostrable el consentimiento: el texto exacto que se mostro,
 * la IP y el dispositivo. Sin esa evidencia no se puede probar que hubo consentimiento valido,
 * y uno que no se puede probar no existe a efectos practicos.
 */
data class ConsentRecord(
    val purpose: ConsentPurpose,
    val policyVersion: PolicyVersion,
    val granted: Boolean,
    val grantedAt: Timestamp,
    val revokedAt: Timestamp? = null,
    val evidence: ConsentEvidence? = null,
) {
    val isRevoked: Boolean get() = revokedAt != null

    fun isActive(at: Timestamp): Boolean =
        granted && (revokedAt == null || revokedAt.isAfter(at))
}

data class ConsentEvidence(
    /** Texto literal que se mostro. Si cambia, cambia la version de politica. */
    val shownText: String,
    val ipHash: String?,
    val deviceLabel: String?,
)

/** Estado del titular respecto a todas las finalidades. */
data class ConsentState(
    val records: List<ConsentRecord>,
    /** RN-022: declarado en el registro. No se infiere de nada. */
    val isAdult: Boolean,
) {
    fun latestFor(purpose: ConsentPurpose): ConsentRecord? =
        records.filter { it.purpose == purpose }.maxByOrNull { it.grantedAt.epochMillis }

    companion object {
        fun empty(isAdult: Boolean) = ConsentState(emptyList(), isAdult)
    }
}
