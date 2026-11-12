package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.firstOrNull

/** D11: obtiene el perfil actual (estado ya cargado). */
class GetProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): UserProfile? = repository.profileState.firstOrNull()
}
