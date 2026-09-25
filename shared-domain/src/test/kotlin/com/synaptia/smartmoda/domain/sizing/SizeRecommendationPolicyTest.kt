package com.synaptia.smartmoda.domain.sizing

import com.synaptia.smartmoda.domain.common.Millimeters
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * Cada prueba lleva en el nombre el ID de la regla de negocio que verifica.
 *
 * No es cosmetica: `tools/check_traceability.py` comprueba en CI que toda RN documentada
 * tiene al menos una prueba que la nombra, y rompe el build si alguna se queda sin cubrir.
 */
class SizeRecommendationPolicyTest {

    // ---------------------------------------------------------------- fixtures
    private fun mm(v: Int) = Millimeters(v)

    /** Tabla realista de camisa de hombre. Rangos sin solape, que es como deben cargarse. */
    private val camisas = SizeChartVersion(
        chartId = "acme-camisa-hombre-co",
        version = 3,
        brand = "ACME",
        category = "camisas",
        sizeSystem = "CO",
        keyMeasurements = setOf(MeasurementKey.CHEST, MeasurementKey.WAIST, MeasurementKey.SHOULDER),
        sizesInOrder = listOf(
            SizeEntry("S", mapOf(
                MeasurementKey.CHEST to SizeRange(mm(880), mm(919)),
                MeasurementKey.WAIST to SizeRange(mm(720), mm(779)),
                MeasurementKey.SHOULDER to SizeRange(mm(420), mm(439)),
            )),
            SizeEntry("M", mapOf(
                MeasurementKey.CHEST to SizeRange(mm(920), mm(979)),
                MeasurementKey.WAIST to SizeRange(mm(780), mm(839)),
                MeasurementKey.SHOULDER to SizeRange(mm(440), mm(459)),
            )),
            SizeEntry("L", mapOf(
                MeasurementKey.CHEST to SizeRange(mm(980), mm(1039)),
                MeasurementKey.WAIST to SizeRange(mm(840), mm(899)),
                MeasurementKey.SHOULDER to SizeRange(mm(460), mm(479)),
            )),
            SizeEntry("XL", mapOf(
                MeasurementKey.CHEST to SizeRange(mm(1040), mm(1099)),
                MeasurementKey.WAIST to SizeRange(mm(900), mm(959)),
                MeasurementKey.SHOULDER to SizeRange(mm(480), mm(499)),
            )),
        ),
    )

    /** Persona claramente talla M en las tres medidas. */
    private fun perfilM(fit: FitPreference = FitPreference.REGULAR) = BodyProfile(
        measurements = mapOf(
            MeasurementKey.CHEST to Measurement(mm(950), MeasurementSource.MANUAL),
            MeasurementKey.WAIST to Measurement(mm(800), MeasurementSource.MANUAL),
            MeasurementKey.SHOULDER to Measurement(mm(450), MeasurementSource.MANUAL),
        ),
        fitPreference = fit,
    )

    /** Persona entre M y L: pecho de M, cintura y hombro de L. */
    private fun perfilEntreMyL(fit: FitPreference) = BodyProfile(
        measurements = mapOf(
            MeasurementKey.CHEST to Measurement(mm(970), MeasurementSource.MANUAL),
            MeasurementKey.WAIST to Measurement(mm(860), MeasurementSource.MANUAL),
            MeasurementKey.SHOULDER to Measurement(mm(465), MeasurementSource.MANUAL),
        ),
        fitPreference = fit,
    )

    // ---------------------------------------------------------------- RN-001
    @Test
    fun `RN_001 sin tabla de tallas no recomienda ninguna`() {
        val r = SizeRecommendationPolicy.recommend(perfilM(), chart = null)

        r.shouldBeInstanceOf<SizeRecommendation.Unavailable>()
        r.reason shouldBe UnavailableReason.NO_SIZE_CHART
    }

    @Test
    fun `RN_001 con tabla sin tallas cargadas no recomienda ninguna`() {
        val vacia = camisas.copy(sizesInOrder = emptyList())

        val r = SizeRecommendationPolicy.recommend(perfilM(), vacia)

        r.shouldBeInstanceOf<SizeRecommendation.Unavailable>()
        r.reason shouldBe UnavailableReason.EMPTY_SIZE_CHART
    }

    @Test
    fun `RN_001 sin ninguna medida relevante no recomienda ninguna`() {
        val soloAltura = BodyProfile.ofManual(MeasurementKey.HEIGHT to mm(1750))

        val r = SizeRecommendationPolicy.recommend(soloAltura, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Unavailable>()
        r.reason shouldBe UnavailableReason.NO_RELEVANT_MEASUREMENTS
    }

    // ---------------------------------------------------------------- RN-002
    @Test
    fun `RN_002 la preferencia de ajuste no modifica las medidas del perfil`() {
        val cenido = perfilM(FitPreference.SNUG)
        val holgado = perfilM(FitPreference.LOOSE)

        SizeRecommendationPolicy.recommend(cenido, camisas)
        SizeRecommendationPolicy.recommend(holgado, camisas)

        // El perfil es inmutable y sus medidas son identicas tras recomendar.
        cenido.measurements shouldBe holgado.measurements
        cenido[MeasurementKey.CHEST] shouldBe mm(950)
    }

    @Test
    fun `RN_002 entre dos tallas contiguas el ajuste cenido elige la menor`() {
        val r = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.SNUG), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "M"
        r.explanation.fitPreferenceApplied shouldBe FitPreference.SNUG
    }

    @Test
    fun `RN_002 entre dos tallas contiguas el ajuste holgado elige la mayor`() {
        val r = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.LOOSE), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "L"
        r.explanation.fitPreferenceApplied shouldBe FitPreference.LOOSE
    }

    @Test
    fun `RN_002 el desempate por ajuste se declara en la explicacion`() {
        val entre = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.REGULAR), camisas)
        val limpio = SizeRecommendationPolicy.recommend(perfilM(), camisas)

        (entre as SizeRecommendation.Recommended).explanation.fitPreferenceApplied.shouldNotBeNull()
        (limpio as SizeRecommendation.Recommended).explanation.fitPreferenceApplied.shouldBeNull()
    }

    // ---------------------------------------------------------------- RN-005
    @Test
    fun `RN_005 una medida estimada no reemplaza a una manual`() {
        val perfil = BodyProfile.ofManual(MeasurementKey.CHEST to mm(950))

        val tras = perfil.mergeWith(
            mapOf(MeasurementKey.CHEST to Measurement(mm(1010), MeasurementSource.ESTIMATED)),
        )

        tras[MeasurementKey.CHEST] shouldBe mm(950)
        tras.measurements[MeasurementKey.CHEST]!!.source shouldBe MeasurementSource.MANUAL
    }

    @Test
    fun `RN_005 una medida manual si reemplaza a una estimada`() {
        val perfil = BodyProfile(
            mapOf(MeasurementKey.CHEST to Measurement(mm(1010), MeasurementSource.ESTIMATED)),
        )

        val tras = perfil.mergeWith(
            mapOf(MeasurementKey.CHEST to Measurement(mm(950), MeasurementSource.MANUAL)),
        )

        tras[MeasurementKey.CHEST] shouldBe mm(950)
        tras.measurements[MeasurementKey.CHEST]!!.source shouldBe MeasurementSource.MANUAL
    }

    // ---------------------------------------------------------------- RN-015
    @Test
    fun `RN_015 la recomendacion guarda version de tabla y de algoritmo`() {
        val r = SizeRecommendationPolicy.recommend(perfilM(), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.chartReference shouldBe "acme-camisa-hombre-co@v3"
        r.algorithmVersion shouldBe SizeRecommendationPolicy.ALGORITHM_VERSION
    }

    @Test
    fun `RN_015 dos versiones de la misma tabla producen referencias distintas`() {
        val v4 = camisas.copy(version = 4)

        val a = SizeRecommendationPolicy.recommend(perfilM(), camisas) as SizeRecommendation.Recommended
        val b = SizeRecommendationPolicy.recommend(perfilM(), v4) as SizeRecommendation.Recommended

        a.chartReference shouldBe "acme-camisa-hombre-co@v3"
        b.chartReference shouldBe "acme-camisa-hombre-co@v4"
    }

    // ---------------------------------------------------------------- RN-016
    @Test
    fun `RN_016 toda recomendacion emitida lleva nivel de confianza distinto de NONE`() {
        val casos = listOf(
            perfilM(),
            perfilEntreMyL(FitPreference.REGULAR),
            BodyProfile.ofManual(MeasurementKey.CHEST to mm(950)),
        )

        casos.forEach { perfil ->
            val r = SizeRecommendationPolicy.recommend(perfil, camisas)
            r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
            (r.confidence != Confidence.NONE) shouldBe true
        }
    }

    @Test
    fun `RN_016 medidas todas dentro de una misma talla dan confianza alta`() {
        val r = SizeRecommendationPolicy.recommend(perfilM(), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.confidence shouldBe Confidence.HIGH
        r.explanation.isClean shouldBe true
    }

    @Test
    fun `RN_016 falta una medida clave y baja la confianza`() {
        val sinHombro = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(950),
            MeasurementKey.WAIST to mm(800),
        )

        val r = SizeRecommendationPolicy.recommend(sinHombro, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.confidence shouldBe Confidence.MEDIUM
        r.explanation.missing shouldContain MeasurementKey.SHOULDER
    }

    // ---------------------------------------------------------------- RN-017
    @Test
    fun `RN_017 si la confianza no es alta se ofrece la talla contigua`() {
        val r = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.SNUG), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.confidence shouldBe Confidence.MEDIUM
        r.alternative shouldBe "L"
    }

    @Test
    fun `RN_017 con confianza alta no se ofrece alternativa porque no hace falta`() {
        val r = SizeRecommendationPolicy.recommend(perfilM(), camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.alternative.shouldBeNull()
    }

    // ---------------------------------------------------------------- casos limite
    @Test
    fun `una persona por encima de la tabla recibe la talla mayor, no un error`() {
        val muyGrande = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(1300),
            MeasurementKey.WAIST to mm(1200),
            MeasurementKey.SHOULDER to mm(600),
        )

        val r = SizeRecommendationPolicy.recommend(muyGrande, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "XL"
    }

    @Test
    fun `una persona por debajo de la tabla recibe la talla menor, no un error`() {
        val muyPequena = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(700),
            MeasurementKey.WAIST to mm(600),
            MeasurementKey.SHOULDER to mm(350),
        )

        val r = SizeRecommendationPolicy.recommend(muyPequena, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "S"
    }

    @Test
    fun `la explicacion nombra que medidas apuntaban a otra talla`() {
        val r = SizeRecommendationPolicy.recommend(
            perfilEntreMyL(FitPreference.SNUG), camisas,
        ) as SizeRecommendation.Recommended

        // Eligio M por ajuste cenido; cintura y hombro apuntaban a L y debe decirlo.
        r.explanation.conflicting.keys shouldContain MeasurementKey.WAIST
        r.explanation.conflicting.keys shouldContain MeasurementKey.SHOULDER
        r.explanation.matched.keys shouldContain MeasurementKey.CHEST
    }

    @Test
    fun `el borde exacto de un rango es determinista`() {
        val enElBorde = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(920), // primer mm de M
            MeasurementKey.WAIST to mm(780),
            MeasurementKey.SHOULDER to mm(440),
        )

        val r = SizeRecommendationPolicy.recommend(enElBorde, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "M"
        r.confidence shouldBe Confidence.HIGH
    }

    @Test
    fun `el mismo perfil y la misma tabla producen siempre el mismo resultado`() {
        val a = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.REGULAR), camisas)
        val b = SizeRecommendationPolicy.recommend(perfilEntreMyL(FitPreference.REGULAR), camisas)

        a shouldBe b
    }

    @Test
    fun `una talla a la que le falta el rango de una medida no descarta la tabla entera`() {
        // Caso real: una marca carga pecho y cintura en todas las tallas pero olvida el hombro
        // en la XL. Debe seguir recomendando con el resto, no caerse ni ignorar la tabla.
        val incompleta = camisas.copy(
            sizesInOrder = camisas.sizesInOrder.mapIndexed { i, entry ->
                if (i == 3) entry.copy(ranges = entry.ranges - MeasurementKey.SHOULDER) else entry
            },
        )

        val r = SizeRecommendationPolicy.recommend(perfilM(), incompleta)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "M"
    }

    @Test
    fun `medidas que apuntan a tallas no contiguas dan confianza baja`() {
        // Pecho de S y cintura de XL: no es un cuerpo que la tabla represente bien.
        // Lo honesto es bajar la confianza, no fingir precision.
        val dispar = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(900),
            MeasurementKey.WAIST to mm(930),
            MeasurementKey.SHOULDER to mm(470),
        )

        val r = SizeRecommendationPolicy.recommend(dispar, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.confidence shouldBe Confidence.LOW
        r.alternative.shouldNotBeNull()
    }

    @Test
    fun `con tallas no contiguas gana la que reune mas medidas`() {
        // Dos medidas apuntan a L y una a S: gana L.
        val dosContraUna = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(1000),
            MeasurementKey.WAIST to mm(870),
            MeasurementKey.SHOULDER to mm(425),
        )

        val r = SizeRecommendationPolicy.recommend(dosContraUna, camisas)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.size shouldBe "L"
    }

    @Test
    fun `RN_016 construir una recomendacion con confianza NONE es imposible`() {
        // La regla deja de depender de que alguien se acuerde: el tipo la impone.
        assertFailsWith<IllegalArgumentException> {
            SizeRecommendation.Recommended(
                size = "M",
                confidence = Confidence.NONE,
                alternative = null,
                explanation = Explanation(emptyMap(), emptyMap(), emptySet(), null),
                chartReference = "x@v1",
            )
        }
    }

    @Test
    fun `RN_017 con una sola talla en la tabla y confianza no alta no se inventa alternativa`() {
        val tallaUnica = camisas.copy(sizesInOrder = listOf(camisas.sizesInOrder[1]))
        val sinHombro = BodyProfile.ofManual(
            MeasurementKey.CHEST to mm(950),
            MeasurementKey.WAIST to mm(800),
        )

        val r = SizeRecommendationPolicy.recommend(sinHombro, tallaUnica)

        r.shouldBeInstanceOf<SizeRecommendation.Recommended>()
        r.confidence shouldBe Confidence.MEDIUM
        r.alternative.shouldBeNull() // no hay contigua que ofrecer, y no se fabrica una
    }

    @Test
    fun `un rango invertido no se puede construir`() {
        assertFailsWith<IllegalArgumentException> { SizeRange(mm(1000), mm(900)) }
    }

    @Test
    fun `una tabla sin medidas clave no se puede construir`() {
        assertFailsWith<IllegalArgumentException> { camisas.copy(keyMeasurements = emptySet()) }
    }

    @Test
    fun `la version de una tabla empieza en uno`() {
        assertFailsWith<IllegalArgumentException> { camisas.copy(version = 0) }
    }
}
