package com.thehouse.app.presentation.smartmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchPreferencesDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchBudgetDto
import com.thehouse.app.data.smartmatch.SmartMatchRepository
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import com.thehouse.app.perf.Perf
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.toUiError
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmartMatchUiState>(SmartMatchUiState.Loading)
    val uiState: StateFlow<SmartMatchUiState> = _uiState.asStateFlow()

    /**
     * Carga única por entrada lógica a la pantalla. El Composable usa
     * `LaunchedEffect(Unit)`; retry invoca explícitamente este mismo método.
     * Sin key inestable, sin init {}, sin loops.
     */
    fun load() {
        SmartMatchFlowLogger.emit("load_start")
        viewModelScope.launch {
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
            val preference = loadPreferenceOrNull()
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
            val candidates = fetchCandidates()
            if (candidates.isEmpty()) {
                updateState(SmartMatchUiState.Empty)
                return
            }
            rank(preference, candidates)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            // UI segura: error tipado; DEBUG: tipo + operación, sin PII ni mensaje.
            SmartMatchFlowLogger.exception("guest", t)
            updateState(SmartMatchUiState.Error(toUiError(t)))
        }
    }

    private suspend fun loadAuthenticated() {
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
            SmartMatchFlowLogger.emit("auth_query_start")
            val candidates = fetchCandidates()
            if (candidates.isEmpty()) {
                updateState(SmartMatchUiState.Empty)
                return
            }
            rank(preference, candidates)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            SmartMatchFlowLogger.exception("auth", t)
            updateState(SmartMatchUiState.Error(toUiError(t)))
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

    private fun toUiError(t: Throwable): UiError =
        (t as? DomainError ?: DomainError.Unknown).toUiError()

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
     * Carga preferencias tolerando fallos de red sin sesión.
     *
     * En guest, `/users/me/preferences` responde 401: eso NO es un error de
     * Smart Match. Si hay caché local (`preferenceState`), se reutiliza; si no,
     * se devuelve null → NoPreferences. La excepción se registra en DEBUG con
     * tipo y operación, sin mensaje ni PII.
     */
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
}
