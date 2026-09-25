package com.synaptia.smartmoda.core.common

/**
 * Estado de una pantalla.
 *
 * Es un tipo sellado y no un puñado de booleanos (`isLoading`, `hasError`, `isEmpty`) porque
 * esos permiten combinaciones imposibles: cargando y con error a la vez, vacio y con contenido.
 * Aqui esos estados no se pueden representar, asi que no hay que probarlos.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>

    /**
     * @param message texto ya apto para mostrar. Nunca lleva detalle interno ni stack trace:
     *   eso va a la traza con su `traceId`, no a la cara del usuario.
     */
    data class Error(val message: String, val traceId: String? = null) : UiState<Nothing>
}
