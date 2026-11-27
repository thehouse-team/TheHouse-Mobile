package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.DeviceRegistrationResult
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.device.RegisteredDevice
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.DeviceRepository
import com.thehouse.app.domain.repository.InstallationIdStore

class GetInstallationIdUseCase(
    private val store: InstallationIdStore,
) {
    suspend operator fun invoke(): InstallationId = store.getOrCreate()
}

/**
 * Registro de dispositivo. Rechaza explícitamente sesiones no activas (guest):
 * el Backend exigiría un token JWT y no debe haber registro para invitados.
 */
class RegisterDeviceUseCase(
    private val repository: DeviceRepository,
    private val installationIdStore: InstallationIdStore,
    private val sessionState: () -> SessionState,
    private val appVersion: () -> String?,
    private val locale: () -> String? = { "es-MX" },
) {
    suspend operator fun invoke(fcmToken: String): Result<DeviceRegistrationResult> {
        if (sessionState() !is SessionState.Active) {
            return Result.failure(Exception("guest_device_registration_forbidden"))
        }
        val installationId = installationIdStore.getOrCreate()
        return repository.register(
            installationId = installationId,
            fcmToken = fcmToken,
            appVersion = appVersion(),
            locale = locale(),
        )
    }
}

class UnregisterDeviceUseCase(
    private val repository: DeviceRepository,
    private val installationIdStore: InstallationIdStore,
) {
    suspend operator fun invoke(): Result<Unit> =
        repository.unregister(installationIdStore.getOrCreate())
}

class ListDevicesUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(): Result<List<RegisteredDevice>> = repository.list()
}

/** Persistencia acotada del resultado de desvinculación para no afirmar éxito falso. */
enum class UnregisterOutcome {
    RemoteDeleted, AlreadyAbsent, RemoteFailure, LocalOnly
}

fun DeviceError.toUnregisterOutcome(): UnregisterOutcome = when (this) {
    DeviceError.NotFound -> UnregisterOutcome.AlreadyAbsent
    else -> UnregisterOutcome.RemoteFailure
}
