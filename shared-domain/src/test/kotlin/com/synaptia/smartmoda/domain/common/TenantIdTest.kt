package com.synaptia.smartmoda.domain.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * RN-009 se verifica de verdad en la suite de aislamiento multi-tenant del servidor (EN-1206).
 * Lo que se protege aqui es lo unico que el dominio puede garantizar por si solo: que no exista
 * un TenantId vacio circulando, porque un identificador en blanco es justo el que se cuela en un
 * filtro y devuelve las filas de todo el mundo.
 */
class TenantIdTest {

    @Test
    fun `un tenant sin identificador no se puede construir`() {
        assertFailsWith<IllegalArgumentException> { TenantId("") }
        assertFailsWith<IllegalArgumentException> { TenantId("   ") }
    }

    @Test
    fun `dos referencias al mismo tenant son iguales`() {
        TenantId("acme") shouldBe TenantId("acme")
    }

    @Test
    fun `el identificador se imprime tal cual, sin envoltorio`() {
        // Aparece en logs y trazas: si se imprimiera como TenantId(value=acme) seria ruido.
        TenantId("acme").toString() shouldBe "acme"
    }
}
