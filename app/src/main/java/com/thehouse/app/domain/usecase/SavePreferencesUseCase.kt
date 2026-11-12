package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository

/** D11: guarda las preferencias vía PUT /users/me/preferences. */
class SavePreferencesUseCase(
    private val repository: PreferenceRepository
) {
    suspend operator fun invoke(preference: Preference) = repository.savePreferences(preference)
}
