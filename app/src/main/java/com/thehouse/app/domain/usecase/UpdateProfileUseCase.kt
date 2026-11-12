package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.repository.ProfileRepository

/** D11: actualiza el nombre del perfil vía PATCH /users/me/profile. */
class UpdateProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(name: String?) = repository.updateProfile(name)
}
