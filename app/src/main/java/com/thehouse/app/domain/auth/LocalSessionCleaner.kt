package com.thehouse.app.domain.auth

/**
 * Limpieza local de la sesión sin llamada al Backend.
 *
 * Se usa tras una eliminación de cuenta confirmada y como parte del logout
 * existente. No borra preferencias globales del dispositivo ni datos de otros
 * usuarios.
 */
interface LocalSessionCleaner {

    /** Borra access token en memoria, refresh token persistido y estado de sesión. */
    suspend fun clearLocalSession()
}
