package com.thehouse.app.presentation.roomdetail

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.RoomDetail
import com.thehouse.app.domain.usecase.GetRoomDetailUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.CancellationException

sealed interface RoomDetailUiState {
    data object Loading : RoomDetailUiState
    data class Content(val room: RoomDetail, val isFavorite: Boolean = false) : RoomDetailUiState
    data object NotFound : RoomDetailUiState
    data object RequiresAuth : RoomDetailUiState
    data class Error(val error: UiError) : RoomDetailUiState
}

sealed interface RoomDetailEvent : UiEvent {
    data object Retry : RoomDetailEvent
    data object FavoriteClicked : RoomDetailEvent
}

sealed interface RoomDetailEffect : UiEffect {
    data object Back : RoomDetailEffect
    data object RequireLogin : RoomDetailEffect
    data class Message(val text: String) : RoomDetailEffect
}

/**
 * D18 + D20: detalle real de cuarto (`GET /rooms/{roomId}`), sin fallback local.
 * El estado de favorito se deriva del store compartido por roomId.
 */
class RoomDetailViewModel(
    private val roomId: String,
    private val getRoomDetail: GetRoomDetailUseCase,
    private val favoriteStore: com.thehouse.app.domain.usecase.FavoriteStateStore? = null,
    private val toggleFavorite: com.thehouse.app.domain.usecase.ToggleFavoriteUseCase? = null,
) : BaseViewModel<RoomDetailUiState, RoomDetailEvent, RoomDetailEffect>(
    RoomDetailUiState.Loading,
) {

    init {
        favoriteStore?.roomIds?.let { flow ->
            launch {
                flow.collect { ids ->
                    val current = uiState.value
                    if (current is RoomDetailUiState.Content && current.isFavorite != (roomId in ids)) {
                        setState(current.copy(isFavorite = roomId in ids))
                    }
                }
            }
        }
        load()
    }

    override fun onEvent(event: RoomDetailEvent) {
        when (event) {
            RoomDetailEvent.Retry -> if (uiState.value !is RoomDetailUiState.Content) load()
            RoomDetailEvent.FavoriteClicked -> toggleFavorite()
        }
    }

    private fun toggleFavorite() {
        val useCase = toggleFavorite
        if (useCase == null) {
            sendEffect(RoomDetailEffect.RequireLogin)
            return
        }
        launch {
            when (val result = useCase(roomId)) {
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Added ->
                    sendEffect(RoomDetailEffect.Message("Guardado en favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Removed ->
                    sendEffect(RoomDetailEffect.Message("Quitado de favoritos"))
                is com.thehouse.app.domain.usecase.FavoriteToggleResult.Failed ->
                    if (result.error == DomainError.Unauthorized) {
                        sendEffect(RoomDetailEffect.RequireLogin)
                    } else {
                        sendEffect(RoomDetailEffect.Message("No se pudo actualizar el favorito"))
                    }
            }
        }
    }

    private fun load() {
        if (roomId.isBlank()) {
            setState(RoomDetailUiState.NotFound)
            return
        }
        setState(RoomDetailUiState.Loading)
        launch {
            try {
                val result = getRoomDetail(roomId)
                setState(
                    result.fold(
                        onSuccess = { room ->
                            RoomDetailUiState.Content(
                                room = room,
                                isFavorite = favoriteStore?.contains(roomId) == true,
                            )
                        },
                        onFailure = { t ->
                            when ((t as? DomainException)?.error ?: DomainError.Unknown) {
                                DomainError.NotFound -> RoomDetailUiState.NotFound
                                DomainError.Unauthorized, DomainError.AccountSuspended -> RoomDetailUiState.RequiresAuth
                                else -> RoomDetailUiState.Error(t.toSafeRoomError())
                            }
                        },
                    ),
                )
            } catch (ce: CancellationException) {
                throw ce
            }
        }
    }

    private fun Throwable.toSafeRoomError(): UiError =
        ((this as? DomainException)?.error ?: DomainError.Unknown).toUiError()
}
