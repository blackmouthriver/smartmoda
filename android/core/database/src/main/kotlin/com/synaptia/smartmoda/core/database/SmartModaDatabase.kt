package com.synaptia.smartmoda.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base local de la aplicacion.
 *
 * Offline-first (RNF-07): la interfaz observa esta base, y la red es un mecanismo de
 * actualizacion, no la fuente de verdad. Sin eso, la app no funciona en el probador de una
 * tienda con mala cobertura, que es justo donde se usa.
 */
@Database(
    entities = [BodyProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SmartModaDatabase : RoomDatabase() {
    abstract fun bodyProfileDao(): BodyProfileDao

    companion object {
        const val NAME = "smartmoda.db"
    }
}
