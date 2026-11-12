package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.repository.ProfileRepository

/** D11: indica si el Quick Setup ya fue completado (dato real del perfil). */
class GetQuickSetupUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Boolean = repository.quickSetupState.value
}
