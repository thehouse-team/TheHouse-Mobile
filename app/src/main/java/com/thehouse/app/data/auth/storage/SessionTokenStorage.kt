package com.thehouse.app.data.auth.storage

/**
 * Contrato de almacenamiento aislado de datos de sesión sensibles.
 *
 * La implementación cifra con AES-GCM y Android Keystore; nunca debe usar
 * `EncryptedSharedPreferences` (decisión de la spec NV-04) ni textos planos.
 * Los valores persistidos jamás se envían a backup (ver `data_extraction_rules`).
 */
interface SessionTokenStorage {
    suspend fun readRefreshToken(): ByteArray?
    suspend fun writeRefreshToken(value: ByteArray)
    suspend fun clear()
}
