package com.synaptia.smartmoda.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Los dispatchers se inyectan, no se referencian directamente.
 *
 * Un `Dispatchers.IO` escrito dentro de un caso de uso obliga a que su prueba dependa del
 * planificador real, que es la causa habitual de las pruebas intermitentes. Pasandolos como
 * dependencia, la prueba mete un dispatcher de prueba y el resultado deja de depender del reloj.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}
