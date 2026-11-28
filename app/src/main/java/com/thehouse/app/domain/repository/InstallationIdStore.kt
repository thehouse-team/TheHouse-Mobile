package com.thehouse.app.domain.repository

import com.thehouse.app.domain.device.InstallationId

/**
 * Persistencia del identificador de instalación.
 *
 * El valor se conserva durante reinicios de la app, force-stop, actualización
 * del APK y renovación del token FCM. Solo se pierde tras reinstalación o
 * limpieza de datos (documentado en la spec D21).
 */
interface InstallationIdStore {
    suspend fun getOrCreate(): InstallationId
}
