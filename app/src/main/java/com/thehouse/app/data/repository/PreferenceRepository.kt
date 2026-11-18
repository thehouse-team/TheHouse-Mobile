package com.thehouse.app.data.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.cache.PreferencesSnapshot
import com.thehouse.app.data.remote.api.BudgetPatchBody
import com.thehouse.app.data.remote.api.PreferencesApi
import com.thehouse.app.data.remote.api.PreferencesPatchBody
import com.thehouse.app.data.remote.dto.preferences.toDomain
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

class PreferenceRepositoryImpl(
    private val api: PreferencesApi,
    private val cache: OfflineCacheStore? = null,
    private val currentUserId: () -> String? = { null },
) : PreferenceRepository {

    private val _preferenceState = MutableStateFlow<Preference?>(null)
    override val preferenceState: Flow<Preference?> = _preferenceState.asStateFlow()

    override suspend fun loadPreferences() {
        val userId = currentUserId()
        val cacheKey = userId?.let { OfflineCacheStore.Keys.preferences(it) }
        try {
            val dto = api.getPreferences()
            val pref = dto.toDomain()
            _preferenceState.value = pref
            if (cacheKey != null) {
                cache?.put(cacheKey, PreferencesSnapshot.from(pref), PreferencesSnapshot.serializer(), userScope = userId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            val cached = cacheKey?.let { cache?.get(it, PreferencesSnapshot.serializer()) }?.payload?.toDomain()
            if (cached != null && isCacheFallbackEligible(e)) {
                _preferenceState.value = cached
                return
            }
            throw e
        }
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

    private fun isCacheFallbackEligible(e: Exception): Boolean =
        e is IOException || e is SocketTimeoutException || e is HttpException
}