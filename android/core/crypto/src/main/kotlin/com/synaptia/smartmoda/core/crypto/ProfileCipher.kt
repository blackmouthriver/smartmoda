package com.synaptia.smartmoda.core.crypto

/**
 * Cifrado del perfil corporal.
 *
 * ADR-0003 y EN-0207: medidas, silueta, avatar y fotos viven CIFRADAS en el dispositivo y no
 * se envian al servidor. Si el usuario activa la sincronizacion, lo que sube es un blob que el
 * servidor almacena pero no puede descifrar.
 *
 * Este contrato se define en el Sprint 0 aunque su implementacion sea del Sprint 2, por una
 * razon concreta: el riesgo R-17. Si el perfil se persiste en claro aunque sea una vez, despues
 * hay que migrar el dato mas sensible del producto con una ventana de exposicion por medio.
 * Teniendo la interfaz desde el principio, no existe la tentacion de guardar sin cifrar.
 */
interface ProfileCipher {

    /** Cifra con una clave protegida por el almacen de claves del sistema operativo. */
    suspend fun encrypt(plaintext: ByteArray): EncryptedBlob

    /** Descifra. Falla si la clave no esta disponible, por ejemplo con la pantalla bloqueada. */
    suspend fun decrypt(blob: EncryptedBlob): ByteArray

    /** Destruye el material de clave. Se invoca al eliminar la cuenta (US-0106). */
    suspend fun wipe()
}

/**
 * @param schemaVersion version del formato del contenido cifrado. Permite evolucionar el
 *   perfil sin dejar ilegible lo ya guardado, que con datos que el servidor no puede migrar
 *   es la unica via de actualizacion posible.
 */
data class EncryptedBlob(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val schemaVersion: Int,
) {
    // equals y hashCode a mano porque ByteArray compara por referencia.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlob) return false
        return schemaVersion == other.schemaVersion &&
            ciphertext.contentEquals(other.ciphertext) &&
            nonce.contentEquals(other.nonce)
    }

    override fun hashCode(): Int =
        31 * (31 * ciphertext.contentHashCode() + nonce.contentHashCode()) + schemaVersion

    /** Nunca imprimir el contenido: acabaria en un log. */
    override fun toString(): String =
        "EncryptedBlob(bytes=${ciphertext.size}, schemaVersion=$schemaVersion)"
}
