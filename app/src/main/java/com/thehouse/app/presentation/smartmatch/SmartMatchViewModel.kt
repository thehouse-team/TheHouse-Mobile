package com.thehouse.app.presentation.smartmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchPreferencesDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchBudgetDto
import com.thehouse.app.data.smartmatch.SmartMatchRepository
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.SmartMatchProfileIncompleteException
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.smartmatch.SmartMatchFactor
import com.thehouse.app.domain.smartmatch.SmartMatchModelVersions
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import com.thehouse.app.perf.Perf
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.toSafeUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SmartMatchUiState {
    data object Loading : SmartMatchUiState
    data object RequiresAuth : SmartMatchUiState
    /** Perfil incompleto según el Backend (422 `smart_match_profile_incomplete`). */
    data object NoPreferences : SmartMatchUiState
    data object Empty : SmartMatchUiState
    data class Error(val error: UiError) : SmartMatchUiState
    data class Content(val results: List<SmartMatchResult>) : SmartMatchUiState
}

class SmartMatchViewModel(
    private val sessionState: StateFlow<SessionState>,
    private val preferenceRepository: PreferenceRepository,
    private val smartMatchRepository: SmartMatchRepository,
    private val getProperties: GetPropertiesUseCase,
    private val runSmartMatch: RunSmartMatchUseCase,
    private val dispatchers: DispatcherProvider,
    private val connectivityObserver: ConnectivityObserver? = null,
) : ViewModel() {

    private var localEstimateActive = false

    private val _uiState = MutableStateFlow<SmartMatchUiState>(SmartMatchUiState.Loading)
    val uiState: StateFlow<SmartMatchUiState> = _uiState.asStateFlow()

    /** Campos que el Backend reporta como incompletos en el 422 NV-23. */
    private val _profileMissing = MutableStateFlow<List<String>>(emptyList())
    val profileMissing: StateFlow<List<String>> = _profileMissing.asStateFlow()

    /**
     * Carga única por entrada lógica a la pantalla. El Composable usa
     * `LaunchedEffect(Unit)`; retry invoca explícitamente este mismo método.
     * Sin key inestable, sin init {}, sin loops.
     */
    /**
     * D16 FASE C: al recuperar conectividad, si había una estimación local
     * activa se recarga desde el Backend. Un solo score: la estimación se
     * sustituye por el resultado oficial (o por un error tipado). Sin polling.
     */
    fun observeConnectivity() {
        val observer = connectivityObserver ?: return
        viewModelScope.launch {
            observer.isOnline
                .collect { online ->
                    if (online && localEstimateActive) {
                        SmartMatchFlowLogger.emit("reconnect_remote_replaces_local")
                        load()
                    }
                }
        }
    }

    fun load() {
        SmartMatchFlowLogger.emit("load_start")
        viewModelScope.launch {
            _profileMissing.value = emptyList()
            localEstimateActive = false
            _uiState.value = SmartMatchUiState.Loading
            val session = sessionState.first()
            SmartMatchFlowLogger.emit(
                "session=" + when (session) {
                    is SessionState.Active -> "Active"
                    SessionState.Anonymous -> "Anonymous"
                    is SessionState.Suspended -> "Suspended"
                },
            )
            when (session) {
                is SessionState.Anonymous -> loadGuest()
                is SessionState.Active -> loadAuthenticated()
                is SessionState.Suspended -> {
                    updateState(SmartMatchUiState.RequiresAuth)
                }
            }
        }
    }

    private suspend fun loadGuest() {
        try {
            SmartMatchFlowLogger.emit("preferences_start")
            // D16 fix: un usuario Anonymous NUNCA debe invocar
            // GET /api/v1/users/me/preferences (endpoint protegido → 401).
            // Las preferencias guest se leen del estado local existente
            // (PreferenceRepository.preferenceState, alimentado por DataStore
            // y por la sesión local); 0 llamadas HTTP de preferencias.
            val preference = loadGuestPreferencesOrNull()
            val ready = preference != null && hasMeaningfulPreferences(preference)
            SmartMatchFlowLogger.preferences(
                when {
                    preference == null -> "missing"
                    !ready -> "incomplete"
                    else -> "present"
                },
            )
            if (!ready) {
                // Sin preferencias: estado tipado, NO error genérico y sin query inválida.
                updateState(SmartMatchUiState.NoPreferences)
                return
            }
            val queryRequest = SmartMatchQueryRequest(
                universityId = null,
                preferences = preference.toPreferencesDto(),
            )
            SmartMatchFlowLogger.emit("guest_query_start")
            val response = smartMatchRepository.queryRecommendations(queryRequest)
            if (response.items.isEmpty()) {
                updateState(SmartMatchUiState.Empty)
                return
            }
            // D16: el Backend es autoridad también en GUEST 200. NO re-score,
            // NO re-order. La corrección de la auditoría elimina el ranking local.
            SmartMatchFlowLogger.emit("guest_backend_authority")
            updateState(SmartMatchUiState.Content(response.items.map { it.toDomain() }))
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: SmartMatchProfileIncompleteException) {
            SmartMatchFlowLogger.emit("guest_query_result=profile_incomplete")
            _profileMissing.value = e.missing
            updateState(SmartMatchUiState.NoPreferences)
        } catch (t: Throwable) {
            // UI segura: error tipado; DEBUG: tipo + operación, sin PII ni mensaje.
            SmartMatchFlowLogger.exception("guest", t)
            when (val error = t.toSafeUiError()) {
                UiError.Network, UiError.Timeout -> {
                    SmartMatchFlowLogger.fallbackStart("network")
                    localMlFallback()
                }
                UiError.ServiceUnavailable -> {
                    SmartMatchFlowLogger.fallbackStart("503")
                    localMlFallback()
                }
                else -> updateState(SmartMatchUiState.Error(error))
            }
        }
    }

    /**
     * AUTH (NV-23 v1.2.0): el Backend es autoridad de perfil, preferencias,
     * universidad, scoring, ranking, breakdown y dismissed. Mobile NO vuelve
     * a ordenar ni a re-scorear una respuesta 200.
     *
     * Solo ante indisponibilidad remota (network/503) se usa el fallback ML
     * local de NV-17/NV-23; ahí sí se cargan preferencias y candidatos.
     */
    private suspend fun loadAuthenticated() {
        try {
            SmartMatchFlowLogger.emit("auth_query_start")
            val result = smartMatchRepository.getRecommendations(page = 1, pageSize = DEFAULT_PAGE_SIZE)
            val response = result.getOrElse { throw it }
            SmartMatchFlowLogger.authQueryResult(response.items.size)
            if (response.items.isEmpty()) {
                updateState(SmartMatchUiState.Empty)
                return
            }
            // Autoridad del Backend: mapping 1:1, sin sort ni re-score.
            updateState(SmartMatchUiState.Content(response.items.map { it.toDomain() }))
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: SmartMatchProfileIncompleteException) {
            SmartMatchFlowLogger.emit("auth_query_result=profile_incomplete")
            _profileMissing.value = e.missing
            updateState(SmartMatchUiState.NoPreferences)
        } catch (t: Throwable) {
            SmartMatchFlowLogger.exception("auth", t)
            when (val error = t.toSafeUiError()) {
                UiError.Unauthorized -> updateState(SmartMatchUiState.RequiresAuth)
                UiError.Network, UiError.Timeout -> {
                    SmartMatchFlowLogger.fallbackStart("network")
                    localMlFallback()
                }
                // D16: solo 503 habilita fallback. El resto de 5xx NO.
                UiError.ServiceUnavailable -> {
                    SmartMatchFlowLogger.fallbackStart("503")
                    localMlFallback()
                }
                else -> updateState(SmartMatchUiState.Error(error))
            }
        }
    }

    /** Fallback ML on-device: solo offline/network/503. Si vuelve el remoto, vuelve a ser autoridad. */
    private suspend fun localMlFallback() {
        localEstimateActive = true
        try {
            SmartMatchFlowLogger.emit("preferences_start")
            preferenceRepository.loadPreferences()
            val preference = preferenceRepository.preferenceState.first()
            val ready = preference != null && hasMeaningfulPreferences(preference)
            SmartMatchFlowLogger.preferences(
                when {
                    preference == null -> "missing"
                    !ready -> "incomplete"
                    else -> "present"
                },
            )
            if (!ready) {
                updateState(SmartMatchUiState.NoPreferences)
                return
            }
            SmartMatchFlowLogger.emit("candidates_start")
            val candidates = fetchCandidates()
            if (candidates.isEmpty()) {
                updateState(SmartMatchUiState.Empty)
                return
            }
            rank(preference, candidates)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            SmartMatchFlowLogger.exception("fallback", t)
            updateState(SmartMatchUiState.Error(t.toSafeUiError()))
        }
    }

    private suspend fun rank(preference: Preference, candidates: List<Property>) {
        SmartMatchFlowLogger.emit("ranking_start")
        val ranked = withContext(dispatchers.default) {
            Perf.trace("smartmatch.ranking") {
                runSmartMatch.execute(preference, candidates)
            }
        }
        updateState(
            if (ranked.isEmpty()) SmartMatchUiState.Empty
            else SmartMatchUiState.Content(ranked),
        )
    }

    private fun updateState(state: SmartMatchUiState) {
        _uiState.value = state
        SmartMatchFlowLogger.state(state)
    }

    private fun Preference?.toPreferencesDto(): SmartMatchPreferencesDto? {
        if (this == null) return null
        return SmartMatchPreferencesDto(
            budget = budget?.let { SmartMatchBudgetDto(min = it.min, max = it.max, currency = it.currency) },
            essentialServices = essentialServices,
            livingWith = livingWith,
            placeType = placeType,
            pets = pets,
            transport = transport,
            propertyTypes = propertyTypes,
            currency = budget?.currency,
        )
    }

    /**
     * D16 fix — Guest: SOLO estado local, SIN `loadPreferences()`.
     *
     * `PreferenceRepository.preferenceState` ya contiene las preferencias
     * persistidas localmente (DataStore/OfflineCacheStore para la sesión)
     * cargadas por el flujo de Preferences/Quick Setup. Nunca se llama al
     * endpoint autenticado cuando `sessionState` es Anonymous.
     */
    private suspend fun loadGuestPreferencesOrNull(): Preference? {
        return try {
            // restoreLocalOrNull usa SOLO DataStore/estado en memoria (0 HTTP).
            preferenceRepository.restoreLocalOrNull() ?: preferenceRepository.preferenceState.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            SmartMatchFlowLogger.exception("guestLocalPreferences", t)
            null
        }
    }

    /** Carga preferencias del usuario autenticado (endpoint protegido). */
    private suspend fun loadPreferenceOrNull(): Preference? {
        return try {
            preferenceRepository.loadPreferences()
            preferenceRepository.preferenceState.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            SmartMatchFlowLogger.exception("loadPreferences", t)
            try {
                preferenceRepository.preferenceState.first()
            } catch (fallback: Throwable) {
                SmartMatchFlowLogger.exception("preferenceState", fallback)
                null
            }
        }
    }

    private fun hasMeaningfulPreferences(preference: Preference?): Boolean {
        if (preference == null) return false
        val b = preference.budget
        return (b != null && (b.min != null || b.max != null)) ||
            !preference.essentialServices.isNullOrEmpty() ||
            preference.maxDistanceKm != null ||
            !preference.livingWith.isNullOrBlank() ||
            !preference.placeType.isNullOrBlank() ||
            !preference.pets.isNullOrBlank() ||
            !preference.transport.isNullOrBlank() ||
            !preference.propertyTypes.isNullOrEmpty()
    }

    /** Hasta 3 páginas de 50 (máx. 150), distinct por id. Sin datos fake. */
    private suspend fun fetchCandidates(): List<Property> {
        SmartMatchFlowLogger.emit("candidates_start")
        val all = mutableListOf<Property>()
        val pageSize = 50
        val maxPages = 3
        for (page in 1..maxPages) {
            val result = getProperties(page = page, pageSize = pageSize)
            val paged = result.getOrElse { throw it }
            all.addAll(paged.items)
            if (page >= paged.totalPages || paged.items.isEmpty()) break
        }
        SmartMatchFlowLogger.candidates(all.distinctBy { it.id }.size)
        return all.distinctBy { it.id }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20

        /**
         * Mapping del item NV-23 v1.2.0 + D16 al modelo de dominio.
         * Conserva el orden del Backend, el `matchScore` decimal y el breakdown
         * estructurado (factor/label/points/maxPoints). Sin re-score ni redondeo.
         */
        /**
         * GUEST 200: mapping 1:1 del item público. El Backend es autoridad de
         * orden, score y breakdown. `matchScore` nullable se conserva como 0.0
         * solo cuando el Backend marca el room como no elegible.
         */
        private fun com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationDto.toDomain(): SmartMatchResult {
            val score = (matchScore ?: 0.0).coerceIn(0.0, 100.0)
            val property = Property(
                id = roomId,
                title = title ?: "",
                price = price ?: 0.0,
                currency = currency ?: "",
                city = "",
                universityId = null,
                bedrooms = 0,
                bathrooms = 0,
                areaM2 = 0.0,
                createdAt = "",
                description = null,
                amenities = emptyList(),
                thumbnailUrl = null,
                photos = emptyList(),
            )
            return SmartMatchResult(
                property = property,
                probability = (score / 100.0).coerceIn(0.0, 1.0),
                matchScore = score,
                factors = breakdown.map {
                    SmartMatchFactor(
                        factor = it.factor,
                        label = it.label,
                        points = it.points,
                        maxPoints = it.maxPoints,
                    )
                },
                source = SmartMatchSource.BACKEND,
                modelVersion = SmartMatchModelVersions.SERVER_MODEL_VERSION,
            )
        }

        private fun com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationItemDto.toDomain(): SmartMatchResult {
            val room = this.room
            val property = Property(
                id = room.id,
                title = room.title,
                price = room.price,
                currency = room.currency,
                city = "",
                universityId = null,
                bedrooms = 0,
                bathrooms = 0,
                areaM2 = 0.0,
                createdAt = "",
                description = null,
                amenities = room.services,
                thumbnailUrl = room.thumbnailUrl,
                photos = emptyList(),
            )
            return SmartMatchResult(
                property = property,
                probability = (matchScore / 100.0).coerceIn(0.0, 1.0),
                matchScore = matchScore,
                factors = breakdown.map {
                    SmartMatchFactor(
                        factor = it.factor,
                        label = it.label,
                        points = it.points,
                        maxPoints = it.maxPoints,
                    )
                },
                source = SmartMatchSource.BACKEND,
                modelVersion = SmartMatchModelVersions.SERVER_MODEL_VERSION,
            )
        }
    }
}
