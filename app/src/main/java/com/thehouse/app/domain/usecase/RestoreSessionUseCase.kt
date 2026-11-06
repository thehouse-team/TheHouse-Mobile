package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository

/**
 * Caso de uso de restauración de sesión al iniciar la app (D3).
 *
 * - Sin refreshToken → Anonymous (sin sesión offline inventada).
 * - Con refresh 200 → guardar tokens nuevos + `GET /users/me` → Active.
 * - Con refresh 401 → limpieza total (Anonymous, vuelve a Login).
 */
class RestoreSessionUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(): Result<UserSession?> = repository.restoreSession()
}
