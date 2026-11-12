package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Preference
import kotlinx.coroutines.flow.Flow

/** Contrato de Domain para las preferencias del usuario (D11). Data lo implementa. */
interface PreferenceRepository {
    val preferenceState: Flow<Preference?>

    suspend fun loadPreferences()
    suspend fun savePreferences(preference: Preference)
}
