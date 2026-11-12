package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.repository.ProfileRepository

/** D11: selecciona la universidad del usuario vía PUT /users/me/university. */
class SelectUserUniversityUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(universityId: String?) =
        repository.selectUniversity(universityId)
}
