package com.synaptia.smartmoda.feature.sizing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synaptia.smartmoda.core.common.DispatcherProvider
import com.synaptia.smartmoda.core.common.UiState
import com.synaptia.smartmoda.domain.sizing.BodyProfile
import com.synaptia.smartmoda.domain.sizing.SizeRecommendation
import com.synaptia.smartmoda.domain.sizing.SizeRecommendationPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * De donde salen las piezas del calculo.
 *
 * El perfil corporal se lee del almacenamiento LOCAL CIFRADO y nunca viaja (ADR-0003).
 * La tabla de tallas si viene de la red, porque es dato publico del catalogo.
 * Esa asimetria es la decision de privacidad del producto, y se ve en la forma del contrato.
 */
interface SizeChartRepository {
    suspend fun chartFor(variantId: String): com.synaptia.smartmoda.domain.sizing.SizeChartVersion?
}

interface BodyProfileRepository {
    /** Lee el perfil descifrado en memoria. Nunca lo expone fuera del proceso. */
    suspend fun current(): BodyProfile?
}

/**
 * ViewModel de la recomendacion de talla.
 *
 * El calculo ocurre AQUI, en el dispositivo, con `SizeRecommendationPolicy` de `shared-domain`.
 * No hay endpoint de "dame mi talla": si lo hubiera, las medidas tendrian que viajar y ADR-0003
 * se caeria. Que el motor sea Kotlin puro es lo que hace posible esa decision.
 */
class SizingViewModel @Inject constructor(
    private val charts: SizeChartRepository,
    private val profiles: BodyProfileRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SizeRecommendation>>(UiState.Loading)
    val state: StateFlow<UiState<SizeRecommendation>> = _state.asStateFlow()

    fun recommendFor(variantId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val result = runCatching {
                withContext(dispatchers.default) {
                    val profile = profiles.current()
                        ?: return@withContext null
                    SizeRecommendationPolicy.recommend(profile, charts.chartFor(variantId))
                }
            }

            _state.value = result.fold(
                onSuccess = { recommendation ->
                    // Sin perfil corporal no hay nada que calcular todavia: es un estado vacio
                    // legitimo que lleva al usuario a tomar sus medidas, no un error.
                    if (recommendation == null) UiState.Empty else UiState.Content(recommendation)
                },
                onFailure = { UiState.Error(message = "No pudimos calcular tu talla ahora mismo") },
            )
        }
    }
}
