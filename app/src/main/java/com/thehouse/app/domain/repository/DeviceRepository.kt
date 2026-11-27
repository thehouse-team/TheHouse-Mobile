package com.thehouse.app.domain.repository

import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.DeviceRegistrationResult
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.device.RegisteredDevice

/** Excepción de Domain con el motivo semántico del fallo (sin cuerpo crudo). */
class DeviceException(val reason: DeviceError) : Exception("device_$reason")

/**
 * Contrato de Domain para el registro de dispositivos FCM.
 *
 * El Backend es la autoridad: el repositorio no inventa respuestas ni
 * devuelve éxito ante errores HTTP.
 */
interface DeviceRepository {
    suspend fun register(
        installationId: InstallationId,
        fcmToken: String,
        platform: String = "android",
        appVersion: String?,
        locale: String?,
    ): Result<DeviceRegistrationResult>

    /** 204 o 404 se consideran "no presente"; cualquier otro código es fallo. */
    suspend fun unregister(installationId: InstallationId): Result<Unit>

    suspend fun list(): Result<List<RegisteredDevice>>
}
