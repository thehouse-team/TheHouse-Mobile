package com.thehouse.app.domain.repository

/**
 * Repositorio de primer ingreso (onboarding) definido en Domain.
 *
 * `onboardingCompleted` se guarda por usuario mediante DataStore y NO es un secreto.
 * U2: la clave se deriva del identificador local conocido por la sesión activa
 * ("local") o del id entregado por el backend en login/register. La consulta
 * remota del usuario (GET /api/v1/users/me) NO está confirmada: BLOCKED-BACKEND.
 */
interface OnboardingRepository {

    /** Clave estable por usuario para la marca de onboarding completado. */
    fun keyFor(userId: String?): String

    suspend fun isCompleted(key: String): Boolean

    suspend fun setCompleted(key: String, completed: Boolean)
}
