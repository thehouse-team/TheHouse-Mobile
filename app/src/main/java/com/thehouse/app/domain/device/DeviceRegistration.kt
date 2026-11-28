package com.thehouse.app.domain.device

/** Dominio puro del dispositivo registrado en el Backend. */
data class RegisteredDevice(
    val installationId: String,
    val platform: String,
    val appVersion: String?,
    val registeredAt: String,
    val updatedAt: String,
)

/** Errores semánticos del registro de dispositivos (nunca cruzan DTO crudos). */
sealed interface DeviceError {
    data object Network : DeviceError
    data object Timeout : DeviceError
    data object Validation : DeviceError
    data object Unauthorized : DeviceError
    data object NotFound : DeviceError
    data object Conflict : DeviceError
    data object RateLimited : DeviceError
    data object ServiceUnavailable : DeviceError
    data object Server : DeviceError
    data object Unknown : DeviceError
}

/** Resultado del registro: distingue creación (201) de actualización (200). */
sealed interface DeviceRegistrationResult {
    data object Created : DeviceRegistrationResult
    data object Updated : DeviceRegistrationResult
}

/** Persistencia acotada del resultado de desvinculación para no afirmar éxito falso. */
enum class UnregisterOutcome {
    RemoteDeleted, AlreadyAbsent, RemoteFailure, LocalOnly
}
