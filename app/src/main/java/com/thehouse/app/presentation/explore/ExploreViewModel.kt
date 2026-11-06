package com.thehouse.app.presentation.explore

import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.SearchPropertiesUseCase
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.toCatalogState
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

/**
 * MVVM + UDF de Explorar (D5, NV-07 parcial).
 * - Carga propiedades reales (API → DTO → mapper → repository → UI state).
 * - Búsqueda solo local sobre título/ciudad (campos reales).
 * - Sin mapa (placeholder en UI), sin Smart Match ML, sin favoritos backend.
 * - Sin use case real (tests/previa): Empty (sin red inventada).
 */
data class ExploreUiState(
    val query: String = "",
    val properties: CatalogLoadState<Property> = CatalogLoadState.Loading,
    val allProperties: List<Property> = emptyList(),
    val isSearching: Boolean = false,
)

sealed interface ExploreEvent : UiEvent {
    data object RetryClicked : ExploreEvent
    data class QueryChanged(val query: String) : ExploreEvent
}

sealed interface ExploreEffect : UiEffect

class ExploreViewModel(
    private val getProperties: GetPropertiesUseCase? = null,
    private val searchProperties: SearchPropertiesUseCase = SearchPropertiesUseCase(),
) : BaseViewModel<ExploreUiState, ExploreEvent, ExploreEffect>(ExploreUiState()) {

    init {
        load()
    }

    override fun onEvent(event: ExploreEvent) {
        when (event) {
            ExploreEvent.RetryClicked -> load()
            is ExploreEvent.QueryChanged -> applyQuery(event.query)
        }
    }

    fun load() {
        val useCase = getProperties
        if (useCase == null) {
            updateState { it.copy(properties = CatalogLoadState.Empty) }
            return
        }
        updateState { it.copy(properties = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            val state = result.toCatalogState()
            val all = if (state is CatalogLoadState.Success) state.items else emptyList()
            updateState {
                it.copy(
                    properties = state,
                    allProperties = all,
                )
            }
        }
    }

    private fun applyQuery(query: String) {
        val all = uiState.value.allProperties
        updateState {
            val filtered = searchProperties(all, query)
            val state: CatalogLoadState<Property> = when {
                filtered.isNotEmpty() -> CatalogLoadState.Success(filtered)
                query.isBlank() -> it.properties
                else -> CatalogLoadState.Empty
            }
            it.copy(query = query, properties = state)
        }
    }
}
