package com.thehouse.app.presentation.favorites

import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.GetFavoritesUseCase
import com.thehouse.app.domain.usecase.GetFavoriteIdsUseCase
import com.thehouse.app.domain.usecase.RemoveFavoriteUseCase
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
import com.thehouse.app.domain.model.Favorite
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val items: List<Favorite> = emptyList(),
    val error: UiError? = null,
    val removingId: String? = null,
)

sealed interface FavoritesEvent : UiEvent {
    data object Load : FavoritesEvent
    data class Remove(val roomId: String) : FavoritesEvent
}

sealed interface FavoritesEffect : UiEffect {
    data class ShowMessage(val message: String) : FavoritesEffect
}

/**
 * Favoritos reales D20: GET /favorites + /ids (roomIds) y DELETE por roomId.
 * El estado compartido se sincroniza con cada carga.
 */
class FavoritesViewModel(
    private val getFavorites: GetFavoritesUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val favoriteIdsStore: FavoriteStateStore,
    private val sessionState: StateFlow<SessionState>,
    connectivityObserver: ConnectivityObserver? = null,
    private val getFavoriteIds: GetFavoriteIdsUseCase? = null,
    private val toggleFavorite: ToggleFavoriteUseCase? = null,
) : BaseViewModel<FavoritesUiState, FavoritesEvent, FavoritesEffect>(FavoritesUiState()) {

    val isOnline: StateFlow<Boolean> = connectivityObserver?.isOnline ?: MutableStateFlow(true)

    init { load() }

    override fun onEvent(event: FavoritesEvent) {
        when (event) {
            FavoritesEvent.Load -> load()
            is FavoritesEvent.Remove -> remove(event.roomId)
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null
    private var loadedOwner: String? = null

    fun load(force: Boolean = false) {
        val session = sessionState.value
        if (session !is SessionState.Active) {
            updateState { it.copy(isLoading = false, items = emptyList(), error = null) }
            return
        }
        val owner = (session as? com.thehouse.app.domain.model.SessionState.Active)?.session?.userId
        // D20 hotfix: init + LaunchedEffect(isAuthenticated) llegaban juntos → dos GET.
        if (!force && loadJob?.isActive == true && loadedOwner == owner) return
        loadJob?.cancel()
        loadedOwner = owner
        updateState { it.copy(isLoading = true, error = null) }
        loadJob = launch {
            val result = getFavorites()
            result.fold(
                onSuccess = { page ->
                    updateState { it.copy(isLoading = false, items = page.items, error = null) }
                    // GET /favorites ya incluye roomIds; el reconcile con /ids solo
                    // se hace en la sincronía única de AppContainer (evita duplicado).
                    favoriteIdsStore.setAll(page.items.map { it.roomId }.toSet())
                },
                onFailure = { throwable ->
                    val error = (throwable as? DomainException)?.error ?: DomainError.Unknown
                    updateState { it.copy(isLoading = false, error = error.toUiError()) }
                },
            )
        }
    }

    private fun remove(roomId: String) {
        if (uiState.value.removingId != null) return
        updateState { it.copy(removingId = roomId) }
        launch {
            val result = removeFavorite(roomId)
            result.fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            items = it.items.filterNot { f -> f.roomId == roomId },
                            removingId = null,
                        )
                    }
                    favoriteIdsStore.remove(roomId)
                },
                onFailure = { throwable ->
                    val error = (throwable as? DomainException)?.error ?: DomainError.Unknown
                    val message = when (error) {
                        DomainError.RateLimited -> "Demasiadas solicitudes. Intenta de nuevo."
                        DomainError.Server, DomainError.ServiceUnavailable -> "Servicio no disponible temporalmente."
                        else -> "No se pudo quitar el favorito."
                    }
                    updateState { it.copy(removingId = null) }
                    sendEffect(FavoritesEffect.ShowMessage(message))
                },
            )
        }
    }
}
