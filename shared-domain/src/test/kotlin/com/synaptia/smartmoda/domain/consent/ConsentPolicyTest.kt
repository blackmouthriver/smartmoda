package com.synaptia.smartmoda.domain.consent

import com.synaptia.smartmoda.domain.common.Timestamp
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ConsentPolicyTest {

    private val ahora = Timestamp(1_700_000_000_000)
    private val politicaActual = PolicyVersion(ConsentPolicy.POLICY_VERSION_CURRENT)

    private fun otorgado(
        purpose: ConsentPurpose,
        version: PolicyVersion = politicaActual,
        revokedAt: Timestamp? = null,
    ) = ConsentRecord(
        purpose = purpose,
        policyVersion = version,
        granted = true,
        grantedAt = ahora + (-Timestamp.DAY),
        revokedAt = revokedAt,
    )

    // ---------------------------------------------------------------- RN-006
    @Test
    fun `RN_006 sin consentimiento previo no se puede capturar imagen corporal`() {
        val estado = ConsentState.empty(isAdult = true)

        val d = ConsentPolicy.canExercise(ConsentPurpose.BODY_IMAGE, estado, ahora)

        d.shouldBeInstanceOf<ConsentDecision.Denied>()
        d.reason shouldBe ConsentDecision.DenialReason.NEVER_GRANTED
    }

    @Test
    fun `RN_006 con consentimiento vigente si se puede capturar`() {
        val estado = ConsentState(listOf(otorgado(ConsentPurpose.BODY_IMAGE)), isAdult = true)

        ConsentPolicy.canExercise(ConsentPurpose.BODY_IMAGE, estado, ahora) shouldBe
            ConsentDecision.Allowed
    }

    @Test
    fun `RN_006 un consentimiento revocado deja de habilitar la captura`() {
        val estado = ConsentState(
            listOf(otorgado(ConsentPurpose.BODY_IMAGE, revokedAt = ahora + (-Timestamp.HOUR))),
            isAdult = true,
        )

        val d = ConsentPolicy.canExercise(ConsentPurpose.BODY_IMAGE, estado, ahora)

        d.shouldBeInstanceOf<ConsentDecision.Denied>()
        d.reason shouldBe ConsentDecision.DenialReason.REVOKED
    }

    @Test
    fun `RN_006 si la politica cambio el consentimiento anterior ya no es informado`() {
        val estado = ConsentState(
            listOf(otorgado(ConsentPurpose.BODY_MEASUREMENT, version = PolicyVersion("2025-01-01"))),
            isAdult = true,
        )

        val d = ConsentPolicy.canExercise(ConsentPurpose.BODY_MEASUREMENT, estado, ahora)

        d.shouldBeInstanceOf<ConsentDecision.Denied>()
        d.reason shouldBe ConsentDecision.DenialReason.POLICY_OUTDATED
    }

    @Test
    fun `RN_006 el consentimiento de una finalidad no habilita otra`() {
        // Aceptar medidas no autoriza a usar la camara: son finalidades distintas y por eso
        // se piden por separado.
        val estado = ConsentState(listOf(otorgado(ConsentPurpose.BODY_MEASUREMENT)), isAdult = true)

        ConsentPolicy.canExercise(ConsentPurpose.BODY_MEASUREMENT, estado, ahora) shouldBe
            ConsentDecision.Allowed
        ConsentPolicy.canExercise(ConsentPurpose.BODY_IMAGE, estado, ahora)
            .shouldBeInstanceOf<ConsentDecision.Denied>()
    }

    // ---------------------------------------------------------------- RN-007
    @Test
    fun `RN_007 rechazar todo lo opcional no bloquea el nucleo`() {
        val estado = ConsentState.empty(isAdult = true)

        ConsentPolicy.coreExperienceAvailable(estado) shouldBe true
    }

    @Test
    fun `RN_007 marketing y analitica opcional estan marcados como opcionales`() {
        ConsentPurpose.MARKETING.isOptional shouldBe true
        ConsentPurpose.OPTIONAL_ANALYTICS.isOptional shouldBe true
        ConsentPurpose.SHARE_ASSETS.isOptional shouldBe true

        // Las corporales no son opcionales en el sentido de que sin ellas no hay probador,
        // pero tampoco bloquean el nucleo: eso lo fija la prueba anterior.
        ConsentPurpose.BODY_IMAGE.isOptional shouldBe false
        ConsentPurpose.BODY_MEASUREMENT.isOptional shouldBe false
    }

    // ---------------------------------------------------------------- RN-021
    @Test
    fun `RN_021 revocar una finalidad identifica que datos derivados hay que borrar`() {
        ConsentPolicy.derivedDataToErase(ConsentPurpose.BODY_IMAGE)
            .shouldContainExactly("foto_de_captura", "landmarks_de_pose")

        ConsentPolicy.derivedDataToErase(ConsentPurpose.BODY_MEASUREMENT)
            .shouldContain("BodyProfileEntity")
    }

    @Test
    fun `RN_021 toda finalidad declara que borrar, ninguna se queda sin plan`() {
        ConsentPurpose.entries.forEach { purpose ->
            val datos = ConsentPolicy.derivedDataToErase(purpose)
            if (datos.isEmpty()) {
                throw AssertionError("$purpose no declara que datos derivados borrar al revocar")
            }
        }
    }

    @Test
    fun `RN_021 el plazo de borrado es de 72 horas`() {
        ConsentPolicy.ERASURE_DEADLINE_MILLIS shouldBe 72 * Timestamp.HOUR
    }

    // ---------------------------------------------------------------- RN-022
    @Test
    fun `RN_022 un perfil de menor no puede ejercer finalidades corporales`() {
        // Aunque hubiera un registro de consentimiento, no se habilita: para un menor la
        // funcion no existe, no es que falte permiso.
        val estado = ConsentState(
            listOf(
                otorgado(ConsentPurpose.BODY_IMAGE),
                otorgado(ConsentPurpose.BODY_MEASUREMENT),
                otorgado(ConsentPurpose.FACE_IMAGE),
            ),
            isAdult = false,
        )

        listOf(
            ConsentPurpose.BODY_IMAGE,
            ConsentPurpose.BODY_MEASUREMENT,
            ConsentPurpose.FACE_IMAGE,
        ).forEach { purpose ->
            val d = ConsentPolicy.canExercise(purpose, estado, ahora)
            d.shouldBeInstanceOf<ConsentDecision.Denied>()
            d.reason shouldBe ConsentDecision.DenialReason.MINOR_NOT_ALLOWED
        }
    }

    @Test
    fun `RN_022 un perfil de menor si puede recibir comunicaciones si las acepta`() {
        // El veto es sobre lo corporal, no sobre todo. Confundirlo llevaria a no poder
        // vender ropa infantil, que es justo lo que ADR-0013 evita.
        val estado = ConsentState(listOf(otorgado(ConsentPurpose.MARKETING)), isAdult = false)

        ConsentPolicy.canExercise(ConsentPurpose.MARKETING, estado, ahora) shouldBe
            ConsentDecision.Allowed
    }

    // ---------------------------------------------------------------- utilidades
    @Test
    fun `se puede saber que falta para usar el probador sin bloquear nada`() {
        val soloMedidas = ConsentState(
            listOf(otorgado(ConsentPurpose.BODY_MEASUREMENT)), isAdult = true,
        )

        ConsentPolicy.missingForTryOn(soloMedidas, ahora)
            .shouldContainExactly(ConsentPurpose.BODY_IMAGE)
    }

    @Test
    fun `el consentimiento mas reciente gana cuando hay varios de la misma finalidad`() {
        val viejo = ConsentRecord(
            ConsentPurpose.MARKETING, politicaActual, granted = true,
            grantedAt = ahora + (-10 * Timestamp.DAY),
            revokedAt = ahora + (-9 * Timestamp.DAY),
        )
        val nuevo = otorgado(ConsentPurpose.MARKETING)

        val estado = ConsentState(listOf(viejo, nuevo), isAdult = true)

        ConsentPolicy.canExercise(ConsentPurpose.MARKETING, estado, ahora) shouldBe
            ConsentDecision.Allowed
    }

    @Test
    fun `una version de politica vacia no se puede construir`() {
        try {
            PolicyVersion("")
            throw AssertionError("deberia haber fallado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }
}
