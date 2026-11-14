package com.thehouse.app.presentation.smartmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.thehouse.app.domain.common.DispatcherProvider

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
    private val getProperties: GetPropertiesUseCase,
    private val runSmartMatch: RunSmartMatchUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmartMatchUiState>(SmartMatchUiState.Loading)
    val uiState: StateFlow<SmartMatchUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = SmartMatchUiState.Loading
            val session = sessionState.first()
            if (session !is SessionState.Active) {
                _uiState.value = SmartMatchUiState.RequiresAuth
                return@launch
            }
            try {
                preferenceRepository.loadPreferences()
                val preference = preferenceRepository.preferenceState.first()
                if (!hasMeaningfulPreferences(preference)) {
                    _uiState.value = SmartMatchUiState.NoPreferences
                    return@launch
                }
                val candidates = fetchCandidates()
                if (candidates.isEmpty()) {
                    _uiState.value = SmartMatchUiState.Empty
                    return@launch
                }
                val ranked = withContext(dispatchers.default) {
                    runSmartMatch.execute(preference!!, candidates)
                }
                _uiState.value = if (ranked.isEmpty()) SmartMatchUiState.Empty
                else SmartMatchUiState.Content(ranked)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                val domain = t as? DomainError ?: DomainError.Unknown
                _uiState.value = SmartMatchUiState.Error(domain.toUiError())
            }
        }
    }

    private fun hasMeaningfulPreferences(preference: com.thehouse.app.domain.model.Preference?): Boolean {
        if (preference == null) return false
        val b = preference.budget
        val hasBudget = b != null && (b.min != null || b.max != null)
        val hasServices = !preference.essentialServices.isNullOrEmpty()
        val hasDistance = preference.maxDistanceKm != null
        val hasLiving = !preference.livingWith.isNullOrBlank()
        val hasPlace = !preference.placeType.isNullOrBlank()
        val hasPets = !preference.pets.isNullOrBlank()
        val hasTransport = !preference.transport.isNullOrBlank()
        val hasTypes = !preference.propertyTypes.isNullOrEmpty()
        return hasBudget || hasServices || hasDistance || hasLiving || hasPlace || hasPets || hasTransport || hasTypes
    }

    private suspend fun fetchCandidates(): List<Property> {
        // Política documentada: obtener hasta 3 páginas (máx 150) respetando contrato (pageSize<=50), sin descarga infinita.
        val all = mutableListOf<Property>()
        val pageSize = 50
        val maxPages = 3
        for (page in 1..maxPages) {
            val result = getProperties(page = page, pageSize = pageSize)
            val paged = result.getOrElse { throw it }
            all.addAll(paged.items)
            if (page >= paged.totalPages || paged.items.isEmpty()) break
        }
        return all.distinctBy { it.id }
    }
}
