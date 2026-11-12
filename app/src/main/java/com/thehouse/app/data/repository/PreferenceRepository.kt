package com.thehouse.app.data.repository

import com.thehouse.app.data.remote.api.BudgetPatchBody
import com.thehouse.app.data.remote.api.PreferencesApi
import com.thehouse.app.data.remote.api.PreferencesPatchBody
import com.thehouse.app.data.remote.dto.preferences.toDomain
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Implementación Data del contrato de Domain (D11). Sin lógica de negocio. */
class PreferenceRepositoryImpl(
    private val api: PreferencesApi
) : PreferenceRepository {

    private val _preferenceState = MutableStateFlow<Preference?>(null)
    override val preferenceState: Flow<Preference?> = _preferenceState.asStateFlow()

    override suspend fun loadPreferences() {
        val dto = api.getPreferences()
        _preferenceState.value = dto.toDomain()
    }

    override suspend fun savePreferences(preference: Preference) {
        api.putPreferences(
            PreferencesPatchBody(
                budget = preference.budget?.let {
                    BudgetPatchBody(
                        min = it.min,
                        max = it.max,
                        currency = it.currency,
                    )
                },
                maxDistanceKm = preference.maxDistanceKm,
                essentialServices = preference.essentialServices,
                livingWith = preference.livingWith,
                placeType = preference.placeType,
                pets = preference.pets,
                transport = preference.transport,
                propertyTypes = preference.propertyTypes,
            )
        )
        loadPreferences()
    }
}
