package com.thehouse.app.presentation.favorites

import com.thehouse.app.data.favorites.state.FavoriteIdsStore
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.usecase.GetFavoritesUseCase
import com.thehouse.app.domain.usecase.RemoveFavoriteUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val items: List<Favorite> = emptyList(),
    val error: UiError? = null,
    val removingId: String? = null,
)

sealed interface FavoritesEvent : UiEvent {
    data object Load : FavoritesEvent
    data class Remove(val propertyId: String) : FavoritesEvent
}

sealed interface FavoritesEffect : UiEffect {
    data class ShowMessage(val message: String) : FavoritesEffect
}

class FavoritesViewModel(
    private val getFavorites: GetFavoritesUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val favoriteIdsStore: FavoriteIdsStore,
    private val sessionState: StateFlow<SessionState>,
) : BaseViewModel<FavoritesUiState, FavoritesEvent, FavoritesEffect>(FavoritesUiState()) {

    init {
        load()
    }

    override fun onEvent(event: FavoritesEvent) {
        when (event) {
            FavoritesEvent.Load -> load()
            is FavoritesEvent.Remove -> remove(event.propertyId)
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, error = null) }
        launch {
            val result = getFavorites()
            result.fold(
                onSuccess = { page ->
                    updateState { it.copy(isLoading = false, items = page.items, error = null) }
                    favoriteIdsStore.setAll(page.items.map { it.propertyId }.toSet())
                },
                onFailure = { throwable ->
                    val error = (throwable as? DomainException)?.error ?: DomainError.Unknown
                    updateState { it.copy(isLoading = false, error = error.toUiError()) }
                },
            )
        }
    }

    private fun remove(propertyId: String) {
        if (uiState.value.removingId != null) return
        updateState { it.copy(removingId = propertyId) }
        launch {
            val result = removeFavorite(propertyId)
            result.fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            items = it.items.filterNot { f -> f.propertyId == propertyId },
                            removingId = null,
                        )
                    }
                    favoriteIdsStore.remove(propertyId)
                },
                onFailure = { throwable ->
                    val error = (throwable as? DomainException)?.error ?: DomainError.Unknown
                    val message = when (error) {
                        DomainError.RateLimited -> "Demasiadas solicitudes. Intenta de nuevo."
                        DomainError.Server -> "Servicio no disponible temporalmente."
                        else -> "No se pudo quitar el favorito."
                    }
                    updateState { it.copy(removingId = null) }
                    sendEffect(FavoritesEffect.ShowMessage(message))
                },
            )
        }
    }
}