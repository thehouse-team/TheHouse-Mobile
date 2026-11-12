package com.thehouse.app.presentation.requests

import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.usecase.CancelRequestUseCase
import com.thehouse.app.domain.usecase.GetUserRequestsUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class RequestsUiState(
    val isLoading: Boolean = false,
    val items: List<RoomRequest> = emptyList(),
    val statusFilter: RequestStatus? = null,
    val errorCode: RequestErrorCode? = null,
    val errorMessage: String? = null,
    val cancellingId: String? = null,
)

sealed interface RequestsEvent : UiEvent {
    data object Load : RequestsEvent
    data class FilterChanged(val status: RequestStatus?) : RequestsEvent
    data class Cancel(val requestId: String) : RequestsEvent
}

sealed interface RequestsEffect : UiEffect {
    data class ShowMessage(val text: String) : RequestsEffect
}

class RequestsViewModel(
    private val getRequests: GetUserRequestsUseCase,
    private val cancelRequest: CancelRequestUseCase,
) : BaseViewModel<RequestsUiState, RequestsEvent, RequestsEffect>(RequestsUiState()) {

    init {
        load()
    }

    override fun onEvent(event: RequestsEvent) {
        when (event) {
            RequestsEvent.Load -> load()
            is RequestsEvent.FilterChanged -> {
                updateState { it.copy(statusFilter = event.status) }
                load()
            }
            is RequestsEvent.Cancel -> cancel(event.requestId)
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, errorCode = null, errorMessage = null) }
        launch {
            getRequests(uiState.value.statusFilter).fold(
                onSuccess = { page ->
                    updateState { it.copy(isLoading = false, items = page.items) }
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

    private fun cancel(requestId: String) {
        if (uiState.value.cancellingId != null) return
        updateState { it.copy(cancellingId = requestId, errorCode = null, errorMessage = null) }
        launch {
            cancelRequest(requestId).fold(
                onSuccess = { updated ->
                    updateState {
                        it.copy(
                            cancellingId = null,
                            items = it.items.map { item ->
                                if (item.id == updated.id) updated else item
                            },
                        )
                    }
                    sendEffect(RequestsEffect.ShowMessage("Solicitud cancelada."))
                },
                onFailure = { throwable ->
                    val exception = throwable as? RequestException
                    updateState {
                        it.copy(
                            cancellingId = null,
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
