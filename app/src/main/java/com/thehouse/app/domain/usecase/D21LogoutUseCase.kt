package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.device.DeviceSyncCoordinator
import com.thehouse.app.domain.device.UnregisterOutcome
import com.thehouse.app.domain.repository.AuthRepository

/**
 * D21: extiende [LogoutUseCase] para desvincular el dispositivo MIENTRAS la
 * sesión todavía es válida, antes de revocar credenciales.
 *
 * Es un `LogoutUseCase` para preservar las firmas existentes de
 * `U2ViewModelFactory` y `HomeViewModel` sin modificarlas.
 *
 * Garantías:
 * - POST /auth/logout y la purga local existente se preservan (`super.invoke()`).
 * - Un fallo de red NO bloquea el logout: se registra el outcome real y la
 *   purga local siempre se ejecuta en el `finally` del repositorio de auth.
 * - `clearForAccountSwitch()` se ejecuta siempre para que una cuenta B no
 *   herede sincronizaciones ni notificaciones de la cuenta A.
 * - Nunca afirma que el dispositivo remoto fue eliminado si DELETE falló.
 */
class D21LogoutUseCase(
    repository: AuthRepository,
    private val deviceSync: DeviceSyncCoordinator,
) : LogoutUseCase(repository) {

    @Volatile
    var lastUnregisterOutcome: UnregisterOutcome? = null
        private set

    override suspend operator fun invoke(): Result<Unit> {
        lastUnregisterOutcome = try {
            deviceSync.unregisterOnLogout()
        } catch (_: Throwable) {
            UnregisterOutcome.RemoteFailure
        }
        return try {
            super.invoke()
        } finally {
            deviceSync.clearForAccountSwitch()
        }
    }
}
