package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.repository.AuthRepository

/**
 * Caso de uso de logout.
 *
 * Contrato de Domain: el repositorio intenta el logout en backend
 * (POST /api/v1/auth/logout), y SIEMPRE limpia el estado local (tokens, sesión).
 * Sin red, el logout local es inmediato y el resultado no interrumpe el flujo.
 */
class LogoutUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
