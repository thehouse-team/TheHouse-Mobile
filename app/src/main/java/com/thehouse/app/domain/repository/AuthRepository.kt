package com.thehouse.app.domain.repository

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación definido en Domain e implementado en Data.
 *
 * Endpoints utilizados (contrato confirmado del backend, D3):
 * - POST /api/v1/auth/register
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/refresh (rotación: SIEMPRE reemplaza anterior)
 * - GET /api/v1/users/me (id, email, createdAt)
 * - POST /api/v1/auth/logout (204; revoca access + familia refresh)
 *
 * Reglas D3:
 * - Renovación automática single-flight ante 401 (un solo refresh a la vez).
 * - Restauración de sesión al reiniciar app solo si el refresh responde 200.
 * - Refresh 401 → limpieza local total (Anonymous, sin sesión offline inventada).
 */
interface AuthRepository {

    /** Estado de sesión observable. El access token vive solo en memoria (Data). */
    val session: StateFlow<SessionState>

    suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?>

    suspend fun login(email: String, password: String): Result<UserSession?>

    suspend fun logout(): Result<Unit>

    /** Ejecuta `POST /auth/refresh` con rotación local del refresh token. */
    suspend fun refreshAccessToken(): Result<String?>

    /** Ejecuta `GET /users/me` con retry único tras refresh ante 401. */
    suspend fun getCurrentUser(): Result<UserSession?>

    /**
     * Restauración de sesión al iniciar la app:
     * sin refreshToken → Anonymous; con refresh 200 → tokens nuevos + /me;
     * con refresh 401 → limpieza total (Anonymous).
     */
    suspend fun restoreSession(): Result<UserSession?>
}
