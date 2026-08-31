package com.synaptia.smartmoda.domain.common

/**
 * Resultado explicito del dominio. Se prefiere a lanzar excepciones porque un fallo de negocio
 * (no hay tabla de tallas, el stock cambio) no es excepcional: es un camino previsto que la
 * interfaz debe saber mostrar.
 */
sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>
    data class Failure(val reason: String, val code: String) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(f: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Ok -> DomainResult.Ok(f(value))
    is DomainResult.Failure -> this
}
