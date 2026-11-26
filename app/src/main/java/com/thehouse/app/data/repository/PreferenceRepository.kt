package com.thehouse.app.data.repository

import com.thehouse.app.data.cache.OfflineCacheStore
import com.thehouse.app.data.cache.PreferencesSnapshot
import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.PreferencesApi
import com.thehouse.app.data.remote.api.PreferencesPatchBody
import com.thehouse.app.data.remote.dto.preferences.PreferencesContract.toRequest
import com.thehouse.app.data.remote.dto.preferences.toDomainDisplay
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.preferences.flow.PreferencesFlowLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Implementación real de [PreferenceRepository] contra
 * `GET|PUT /api/v1/users/me/preferences`.
 *
 * D14:
 * - Los errores HTTP se convierten a [DomainException] antes de cruzar a Presentation.
 * - La caché NUNCA esconde estados contractuales (401/403/400/404/422/429).
 * - El guardado usa la respuesta 200 del PUT y no depende de un GET posterior.
 */
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
        PreferencesFlowLogger.loadStart()
        try {
            PreferencesFlowLogger.requestStart()
            val dto = api.getPreferences()
            val pref = dto.toDomainDisplay()
            _preferenceState.value = pref
            writeCache(cacheKey, pref, userId)
            PreferencesFlowLogger.requestSuccess()
            PreferencesFlowLogger.state(if (isEmpty(pref)) "Empty" else "Content")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            reportException(throwable, "load")
            if (isAvailabilityFailure(throwable)) {
                val cached = cacheKey?.let { cache?.get(it, PreferencesSnapshot.serializer()) }?.payload?.toDomain()
                if (cached != null) {
                    _preferenceState.value = cached
                    PreferencesFlowLogger.state(if (isEmpty(cached)) "Empty" else "Content")
                    return
                }
            }
            throw toDomainException(throwable)
        }
    }

    /**
     * D16 guest: lee el snapshot local (DataStore existente) sin invocar
     * `GET /users/me/preferences`. Si hay estado en memoria lo reutiliza;
     * si no, restaura el snapshot guest del DataStore. 0 HTTP.
     */
    override suspend fun restoreLocalOrNull(): Preference? {
        _preferenceState.value?.let { return it }
        val cached = cache?.get(OfflineCacheStore.Keys.GUEST_PREFERENCES, PreferencesSnapshot.serializer())
            ?.payload?.toDomain()
        if (cached != null) {
            _preferenceState.value = cached
            PreferencesFlowLogger.state(if (isEmpty(cached)) "Empty" else "Content")
        }
        return cached
    }

    override suspend fun savePreferences(preference: Preference) {
        val userId = currentUserId()
        val cacheKey = userId?.let { OfflineCacheStore.Keys.preferences(it) }
        PreferencesFlowLogger.requestStart()
        try {
            val body: PreferencesPatchBody = with(com.thehouse.app.data.remote.dto.preferences.PreferencesContract) {
                preference.toRequest(
                    min = preference.budget?.min?.toInt(),
                    max = preference.budget?.max?.toInt(),
                )
            }
            val response = api.putPreferences(body)
            val saved = response.toDomainDisplay()
            _preferenceState.value = saved
            writeCache(cacheKey, saved, userId)
            PreferencesFlowLogger.requestSuccess()
            PreferencesFlowLogger.state(if (isEmpty(saved)) "Empty" else "Content")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            reportException(throwable, "save")
            throw toDomainException(throwable)
        }
    }

    private suspend fun writeCache(cacheKey: String?, pref: Preference, userId: String?) {
        val store = cache ?: return
        // D16 guest: toda preferencia guardada queda también en la clave guest
        // del MISMO DataStore, para que Anonymous pueda leerla sin endpoint 401.
        store.put(OfflineCacheStore.Keys.GUEST_PREFERENCES, PreferencesSnapshot.from(pref), PreferencesSnapshot.serializer())
        if (cacheKey != null && userId != null) {
            store.put(cacheKey, PreferencesSnapshot.from(pref), PreferencesSnapshot.serializer(), userScope = userId)
        }
    }

    private fun isEmpty(preference: Preference): Boolean =
        preference.budget?.max == null &&
            preference.livingWith == null &&
            preference.placeType == null &&
            preference.essentialServices.isNullOrEmpty() &&
            preference.propertyTypes.isNullOrEmpty()

    /** Solo fallos de disponibilidad: nunca 4xx. */
    private fun isAvailabilityFailure(throwable: Throwable): Boolean =
        throwable is IOException || throwable is SocketTimeoutException

    private fun toDomainException(throwable: Throwable): Throwable {
        if (throwable is DomainException) return throwable
        val dataError = when (throwable) {
            is SocketTimeoutException -> DataError.Timeout
            is IOException -> DataError.Network
            is HttpException -> {
                val status = throwable.code()
                // 400/422 son estados contractuales conocidos: se tipan como Conflict
                // (dominio) para evitar caer en Unknown. La UI de Preferences traduce
                // Conflict a un mensaje específico de validación de preferencias.
                if (status == 400 || status == 422) {
                    return DomainException(DomainError.Conflict)
                }
                DataError.Http(status)
            }
            else -> DataError.Serialization
        }
        return DomainException(dataError.toDomainError())
    }

    private fun reportException(throwable: Throwable, operation: String) {
        PreferencesFlowLogger.exception(throwable, operation)
        when (toDomainException(throwable)) {
            is DomainException -> {
                val error = (toDomainException(throwable) as DomainException).error
                when (error) {
                    DomainError.Unauthorized -> PreferencesFlowLogger.requestUnauthorized()
                    DomainError.NotFound -> PreferencesFlowLogger.requestNotFound()
                    DomainError.Network, DomainError.Timeout, DomainError.Server,
                    DomainError.RateLimited, DomainError.Unknown,
                    DomainError.AccountSuspended, DomainError.Conflict, DomainError.Gone,
                    DomainError.ServiceUnavailable, DomainError.ValidationError,
                    -> PreferencesFlowLogger.requestError()
                }
                PreferencesFlowLogger.state("Error")
            }
            else -> Unit
        }
    }
}
