package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.validation.ValidationResult
import com.thehouse.app.domain.validation.AuthValidators

/**
 * Caso de uso de registro (RF-01).
 *
 * Valida la entrada de forma independiente de Android y delega en el repositorio.
 * El backend real es POST /api/v1/auth/register. No se asume login automático:
 * la sesión se crea solo si la respuesta del backend la permite.
 */
class RegisterUseCase(private val repository: AuthRepository) {

    fun validate(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): ValidationResult {
        return AuthValidators.validateRegistration(
            name = name,
            email = email,
            password = password,
            acceptedTerms = acceptedTerms,
            acceptedPrivacy = acceptedPrivacy,
        )
    }

    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> = repository.register(
        name = name,
        email = email,
        password = password,
        acceptedTerms = acceptedTerms,
        acceptedPrivacy = acceptedPrivacy,
    )
}
