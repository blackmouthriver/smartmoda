package com.synaptia.smartmoda.domain.sizing

/**
 * Motor de recomendacion de talla.
 *
 * Determinista a proposito (ADR-0007): explicable, reproducible, gratuito y funciona sin conexion.
 * Un modelo estadistico necesita historico de devoluciones que todavia no existe; cuando exista se
 * anade como capa de ajuste ENCIMA de esto, nunca en su reemplazo.
 *
 * Reglas que implementa:
 *  - RN-001  sin tabla aplicable no se inventa una talla
 *  - RN-002  la preferencia de ajuste desempata y se explica; no altera las medidas
 *  - RN-015  toda salida guarda la version de tabla y de algoritmo
 *  - RN-016  toda recomendacion lleva nivel de confianza
 *  - RN-017  con confianza no alta se ofrece la talla contigua
 *
 * No depende de Android, ni de Spring, ni de red. Corre igual en el movil, en el servidor y
 * compilado a WebAssembly en el navegador. Esa es la razon de que viva en shared-domain.
 */
object SizeRecommendationPolicy {

    const val ALGORITHM_VERSION = "sizing-1.0.0"

    fun recommend(profile: BodyProfile, chart: SizeChartVersion?): SizeRecommendation {
        // RN-001: la ausencia de tabla es un resultado legitimo, no un error a maquillar.
        if (chart == null) {
            return SizeRecommendation.Unavailable(UnavailableReason.NO_SIZE_CHART)
        }
        if (!chart.isUsable) {
            return SizeRecommendation.Unavailable(UnavailableReason.EMPTY_SIZE_CHART, chart.reference)
        }

        val available = chart.keyMeasurements.filter { profile.has(it) }
        if (available.isEmpty()) {
            return SizeRecommendation.Unavailable(
                UnavailableReason.NO_RELEVANT_MEASUREMENTS, chart.reference,
            )
        }
        val missing = chart.keyMeasurements - available.toSet()

        // Para cada medida disponible, en que talla cae (o a cual se acerca mas).
        val hitByMeasurement: Map<MeasurementKey, Int> = available.associateWith { key ->
            bestSizeIndexFor(profile[key]!!, key, chart)
        }

        val indices = hitByMeasurement.values.toSortedSet()
        val spread = indices.last() - indices.first()

        val confidence = confidenceOf(spread, missing.size)
        val chosen = chooseIndex(hitByMeasurement, indices.toList(), profile.fitPreference, confidence)
        val alternative = alternativeFor(chosen, indices.toList(), chart, confidence)

        val fitApplied = if (confidence == Confidence.MEDIUM && spread == 1) profile.fitPreference else null

        val explanation = Explanation(
            matched = hitByMeasurement.filterValues { it == chosen }
                .mapValues { chart.sizesInOrder[it.value].label },
            conflicting = hitByMeasurement.filterValues { it != chosen }
                .mapValues { chart.sizesInOrder[it.value].label },
            missing = missing,
            fitPreferenceApplied = fitApplied,
        )

        return SizeRecommendation.Recommended(
            size = chart.sizesInOrder[chosen].label,
            confidence = confidence,
            alternative = alternative,
            explanation = explanation,
            chartReference = chart.reference,
        )
    }

    /**
     * Talla a la que apunta una medida. Si cae dentro de un rango, esa. Si no cae en ninguno
     * (persona por encima o por debajo de la tabla), la mas cercana: fallar con la talla contigua
     * es mejor que no decir nada, y la confianza ya reflejara la duda.
     */
    private fun bestSizeIndexFor(
        value: com.synaptia.smartmoda.domain.common.Millimeters,
        key: MeasurementKey,
        chart: SizeChartVersion,
    ): Int {
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        chart.sizesInOrder.forEachIndexed { index, entry ->
            val range = entry.ranges[key] ?: return@forEachIndexed
            val d = range.distanceFrom(value)
            if (d < bestDistance) {
                bestDistance = d
                bestIndex = index
            }
        }
        return bestIndex
    }

    /**
     * La confianza sale de dos senales: cuanto se dispersan las medidas entre tallas, y cuantas
     * medidas clave faltan. Los umbrales estan en ADR-0011 para el espejo y aqui para el movil.
     */
    private fun confidenceOf(spread: Int, missingCount: Int): Confidence = when {
        spread == 0 && missingCount == 0 -> Confidence.HIGH
        spread <= 1 && missingCount <= 1 -> Confidence.MEDIUM
        else -> Confidence.LOW
    }

    /**
     * Que talla se elige.
     *
     * RN-002: con dos tallas contiguas la preferencia de ajuste desempata. SNUG tira a la menor,
     * LOOSE a la mayor, REGULAR a la mayor tambien: entre apretar y sobrar, sobrar se devuelve
     * menos. Con dispersion mayor gana la talla que mas medidas reune, y si empatan, la mayor.
     */
    private fun chooseIndex(
        hits: Map<MeasurementKey, Int>,
        indices: List<Int>,
        fit: FitPreference,
        confidence: Confidence,
    ): Int {
        if (indices.size == 1) return indices.first()

        if (confidence == Confidence.MEDIUM && indices.size == 2 && indices[1] - indices[0] == 1) {
            return when (fit) {
                FitPreference.SNUG -> indices[0]
                FitPreference.REGULAR, FitPreference.LOOSE -> indices[1]
            }
        }

        // Dispersion mayor que una talla: gana la que reune mas medidas; si empatan, la mayor,
        // porque entre apretar y sobrar, sobrar se devuelve menos.
        val byFrequency = hits.values.groupingBy { it }.eachCount()
        val maxCount = byFrequency.values.max()
        return byFrequency.filterValues { it == maxCount }.keys.max()
    }

    /** RN-017: si la confianza no es alta, siempre se ofrece una salida contigua. */
    private fun alternativeFor(
        chosen: Int,
        indices: List<Int>,
        chart: SizeChartVersion,
        confidence: Confidence,
    ): String? {
        if (confidence == Confidence.HIGH) return null
        val other = indices.firstOrNull { it != chosen }
            ?: (chosen + 1).takeIf { it in chart.sizesInOrder.indices }
            ?: (chosen - 1).takeIf { it in chart.sizesInOrder.indices }
            ?: return null
        return chart.sizesInOrder[other].label
    }
}
