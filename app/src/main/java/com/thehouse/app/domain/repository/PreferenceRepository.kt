package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Preference
import kotlinx.coroutines.flow.Flow

/** Contrato de Domain para las preferencias del usuario (D11). Data lo implementa. */
interface PreferenceRepository {
    val preferenceState: Flow<Preference?>

    suspend fun loadPreferences()
    suspend fun savePreferences(preference: Preference)

    /**
     * D16 guest: restaura preferencias desde almacenamiento LOCAL (sin red).
     * Implementación por defecto para fakes: sin estado local. La implementación
     * real usa el DataStore existente (OfflineCacheStore), sin endpoint 401.
     */
    suspend fun restoreLocalOrNull(): Preference? = null
}
