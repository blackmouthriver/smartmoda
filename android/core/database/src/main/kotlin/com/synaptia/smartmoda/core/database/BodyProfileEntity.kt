package com.synaptia.smartmoda.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil corporal en la base local.
 *
 * Solo guarda el blob CIFRADO (EN-0207). No hay columnas `chest`, `waist` ni `hip`: si
 * existieran, alguien las llenaria en claro alguna vez. Lo que no se puede representar no se
 * puede filtrar.
 */
@Entity(tableName = "body_profile")
data class BodyProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val schemaVersion: Int,
    val updatedAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BodyProfileEntity) return false
        return id == other.id &&
            schemaVersion == other.schemaVersion &&
            updatedAt == other.updatedAt &&
            ciphertext.contentEquals(other.ciphertext) &&
            nonce.contentEquals(other.nonce)
    }

    override fun hashCode(): Int {
        var r = id
        r = 31 * r + ciphertext.contentHashCode()
        r = 31 * r + nonce.contentHashCode()
        r = 31 * r + schemaVersion
        r = 31 * r + updatedAt.hashCode()
        return r
    }

    companion object {
        /** Hay un solo perfil por dispositivo: el de quien lo usa. */
        const val SINGLETON_ID = 1
    }
}
