package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.firstOrNull

/** D11: obtiene las preferencias actuales (estado ya cargado). */
class GetPreferencesUseCase(
    private val repository: PreferenceRepository
) {
    suspend operator fun invoke(): Preference? = repository.preferenceState.firstOrNull()
}
