package com.synaptia.smartmoda.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BodyProfileDao {

    @Query("SELECT * FROM body_profile WHERE id = :id")
    suspend fun get(id: Int = BodyProfileEntity.SINGLETON_ID): BodyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: BodyProfileEntity)

    /** US-0106: eliminar cuenta borra el perfil de verdad, no lo marca como borrado. */
    @Query("DELETE FROM body_profile")
    suspend fun deleteAll()
}
