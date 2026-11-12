package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.usecase.AcceptRequestUseCase
import com.thehouse.app.domain.usecase.GetRequestUseCase
import com.thehouse.app.domain.usecase.RejectRequestUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.requests.RequestErrorMessages

data class LandlordRequestDetailUiState(
    val isLoading: Boolean = false,
    val request: RoomRequest? = null,
    val errorCode: RequestErrorCode? = null,
    val errorMessage: String? = null,
    val busyId: String? = null,
)

sealed interface LandlordRequestDetailEvent : UiEvent {
    data object Load : LandlordRequestDetailEvent
    data object Accept : LandlordRequestDetailEvent
    data class Reject(val reason: String, val note: String?) : LandlordRequestDetailEvent
}

sealed interface LandlordRequestDetailEffect : UiEffect {
    data class OpenConversation(val conversationId: String) : LandlordRequestDetailEffect
}

class LandlordRequestDetailViewModel(
    private val requestId: String,
    private val getRequest: GetRequestUseCase,
    private val acceptRequest: AcceptRequestUseCase,
    private val rejectRequest: RejectRequestUseCase,
) : BaseViewModel<LandlordRequestDetailUiState, LandlordRequestDetailEvent,
    LandlordRequestDetailEffect>(LandlordRequestDetailUiState()) {

    init {
        load()
    }

    override fun onEvent(event: LandlordRequestDetailEvent) {
        when (event) {
            LandlordRequestDetailEvent.Load -> load()
            LandlordRequestDetailEvent.Accept -> accept()
            is LandlordRequestDetailEvent.Reject -> reject(event.reason, event.note)
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, errorCode = null, errorMessage = null) }
        launch {
            getRequest(requestId).fold(
                onSuccess = { request ->
                    updateState { it.copy(isLoading = false, request = request) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorCode = exception?.code ?: RequestErrorCode.UNKNOWN,
                            errorMessage = RequestErrorMessages.map(
                                exception?.code,
                                exception?.domainError,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun accept() {
        if (uiState.value.busyId != null) return
        updateState { it.copy(busyId = requestId, errorCode = null, errorMessage = null) }
        launch {
            acceptRequest(requestId).fold(
                onSuccess = { updated ->
                    updateState { it.copy(busyId = null, request = updated) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            busyId = null,
                            errorCode = exception?.code ?: RequestErrorCode.UNKNOWN,
                            errorMessage = RequestErrorMessages.map(
                                exception?.code,
                                exception?.domainError,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun reject(reason: String, note: String?) {
        if (uiState.value.busyId != null) return
        updateState { it.copy(busyId = requestId, errorCode = null, errorMessage = null) }
        launch {
            rejectRequest(requestId, reason, note).fold(
                onSuccess = { updated ->
                    updateState { it.copy(busyId = null, request = updated) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            busyId = null,
                            errorCode = exception?.code ?: RequestErrorCode.UNKNOWN,
                            errorMessage = RequestErrorMessages.map(
                                exception?.code,
                                exception?.domainError,
                            ),
                        )
                    }
                },
            )
        }
    }
}
