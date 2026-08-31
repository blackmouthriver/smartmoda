package com.synaptia.smartmoda.domain.common

/**
 * Identificador de empresa (tenant).
 *
 * Existe desde el Sprint 0 aunque en la Fase 1 solo haya un tenant real: anadirlo despues
 * obliga a migrar todas las tablas y reescribir los repositorios (ADR-0002).
 *
 * RN-009: el valor efectivo SIEMPRE se deriva del contexto autenticado en el servidor.
 * Nunca se construye a partir de algo que envie el cliente.
 */
@JvmInline
value class TenantId(val value: String) {
    init {
        require(value.isNotBlank()) { "TenantId no puede estar vacio" }
    }

    override fun toString(): String = value
}
