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
    data class Content(val room: RoomDetail) : RoomDetailUiState
    data object NotFound : RoomDetailUiState
    data object RequiresAuth : RoomDetailUiState
    data class Error(val error: UiError) : RoomDetailUiState
}

sealed interface RoomDetailEvent : UiEvent {
    data object Retry : RoomDetailEvent
}

sealed interface RoomDetailEffect : UiEffect {
    data object Back : RoomDetailEffect
    data object RequireLogin : RoomDetailEffect
}

/** D18: ViewModel del detalle real de cuarto. Sin datos inventados ni fallback local. */
class RoomDetailViewModel(
    private val roomId: String,
    private val getRoomDetail: GetRoomDetailUseCase,
) : BaseViewModel<RoomDetailUiState, RoomDetailEvent, RoomDetailEffect>(
    RoomDetailUiState.Loading,
) {

    init { load() }

    override fun onEvent(event: RoomDetailEvent) {
        when (event) {
            RoomDetailEvent.Retry -> if (uiState.value !is RoomDetailUiState.Content) load()
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
                        onSuccess = { RoomDetailUiState.Content(it) },
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
