package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.usecase.CancelRequestUseCase
import com.thehouse.app.domain.usecase.GetRequestUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class RequestDetailUiState(
    val isLoading: Boolean = false,
    val request: RoomRequest? = null,
    val errorCode: RequestErrorCode? = null,
    val errorMessage: String? = null,
    val isCancelling: Boolean = false,
)

sealed interface RequestDetailEvent : UiEvent {
    data object Load : RequestDetailEvent
    data object Cancel : RequestDetailEvent
}

sealed interface RequestDetailEffect : UiEffect {
    data object OpenConversation : RequestDetailEffect
}

class RequestDetailViewModel(
    private val requestId: String,
    private val getRequest: GetRequestUseCase,
    private val cancelRequest: CancelRequestUseCase,
) : BaseViewModel<RequestDetailUiState, RequestDetailEvent, RequestDetailEffect>(
    RequestDetailUiState(),
) {

    init {
        load()
    }

    override fun onEvent(event: RequestDetailEvent) {
        when (event) {
            RequestDetailEvent.Load -> load()
            RequestDetailEvent.Cancel -> cancel()
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

    private fun cancel() {
        if (uiState.value.isCancelling) return
        updateState { it.copy(isCancelling = true, errorCode = null, errorMessage = null) }
        launch {
            cancelRequest(requestId).fold(
                onSuccess = { updated ->
                    updateState { it.copy(isCancelling = false, request = updated) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            isCancelling = false,
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
