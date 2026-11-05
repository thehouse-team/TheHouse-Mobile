package com.thehouse.app.domain.repository

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación definido en Domain e implementado en Data.
 *
 * Endpoints utilizados (contrato confirmado del backend):
 * - POST /api/v1/auth/register
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/logout
 *
 * NOTA U2 (constants documentadas en la spec NV-04):
 * - La renovación automática requiere POST /api/v1/auth/refresh (NO confirmado).
 * - La identificación remota del usuario requiere GET /api/v1/users/me (NO confirmado).
 *   Ambas capacidades permanecen BLOCKED-BACKEND: no se simulan contratos.
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
}
