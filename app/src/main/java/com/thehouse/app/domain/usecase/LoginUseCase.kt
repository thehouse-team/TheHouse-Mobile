package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.validation.AuthValidators
import com.thehouse.app.domain.validation.ValidationResult

/**
 * Caso de uso de login (RF-02).
 *
 * Backend real: POST /api/v1/auth/login. Google Sign-In queda Posterior (fuera de U2).
 */
class LoginUseCase(private val repository: AuthRepository) {

    fun validate(email: String, password: String): ValidationResult =
        AuthValidators.validateLogin(email, password)

    suspend operator fun invoke(email: String, password: String): Result<UserSession?> =
        repository.login(email, password)
}
