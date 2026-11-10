package com.thehouse.app.presentation.roomdetail

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.RoomDetail
import com.thehouse.app.domain.usecase.GetRoomByIdUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError

sealed interface RoomDetailUiState {
    data object Loading : RoomDetailUiState
    data class Content(val room: RoomDetail) : RoomDetailUiState
    data object NotFound : RoomDetailUiState
    data class Error(val error: UiError) : RoomDetailUiState
}

sealed interface RoomDetailEvent : UiEvent {
    data object Retry : RoomDetailEvent
}

sealed interface RoomDetailEffect : UiEffect {
    data object RequireLogin : RoomDetailEffect
}

/**
 * Detalle real de cuarto (NV-07 D10).
 *
 * Solo usa `GET /rooms/{id}` (Bearer); no inventa dirección, dueño,
 * amenities ni contacto. 401 se informa y redirige a Login.
 */
class RoomDetailViewModel(
    private val roomId: String,
    private val getRoomById: GetRoomByIdUseCase,
) : BaseViewModel<RoomDetailUiState, RoomDetailEvent, RoomDetailEffect>(
    RoomDetailUiState.Loading,
) {
    private var requestInFlight = false

    init {
        load()
    }

    override fun onEvent(event: RoomDetailEvent) {
        when (event) {
            RoomDetailEvent.Retry -> {
                if (uiState.value is RoomDetailUiState.Error ||
                    uiState.value is RoomDetailUiState.NotFound
                ) {
                    load()
                }
            }
        }
    }

    private fun load() {
        if (requestInFlight) return
        requestInFlight = true
        setState(RoomDetailUiState.Loading)
        launch {
            try {
                val result = getRoomById(roomId)
                setState(
                    result.fold(
                        onSuccess = { RoomDetailUiState.Content(it) },
                        onFailure = { failure ->
                            val error = (failure as? DomainException)?.error
                                ?: DomainError.Unknown
                            if (error == DomainError.NotFound) {
                                RoomDetailUiState.NotFound
                            } else {
                                if (error == DomainError.Unauthorized) {
                                    sendEffect(RoomDetailEffect.RequireLogin)
                                }
                                RoomDetailUiState.Error(error.toUiError())
                            }
                        },
                    ),
                )
            } finally {
                requestInFlight = false
            }
        }
    }
}
