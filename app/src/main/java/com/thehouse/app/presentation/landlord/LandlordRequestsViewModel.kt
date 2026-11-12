package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.usecase.AcceptRequestUseCase
import com.thehouse.app.domain.usecase.GetLandlordRequestsUseCase
import com.thehouse.app.domain.usecase.RejectRequestUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.requests.RequestErrorMessages

data class LandlordRequestsUiState(
    val isLoading: Boolean = false,
    val items: List<RoomRequest> = emptyList(),
    val statusFilter: RequestStatus? = null,
    val errorCode: RequestErrorCode? = null,
    val errorMessage: String? = null,
    val busyId: String? = null,
)

sealed interface LandlordRequestsEvent : UiEvent {
    data object Load : LandlordRequestsEvent
    data class FilterChanged(val status: RequestStatus?) : LandlordRequestsEvent
    data class Accept(val requestId: String) : LandlordRequestsEvent
    data class Reject(val requestId: String, val reason: String, val note: String?) :
        LandlordRequestsEvent
}

sealed interface LandlordRequestsEffect : UiEffect {
    data class ShowMessage(val text: String) : LandlordRequestsEffect
}

class LandlordRequestsViewModel(
    private val getLandlordRequests: GetLandlordRequestsUseCase,
    private val acceptRequest: AcceptRequestUseCase,
    private val rejectRequest: RejectRequestUseCase,
) : BaseViewModel<LandlordRequestsUiState, LandlordRequestsEvent, LandlordRequestsEffect>(
    LandlordRequestsUiState(),
) {

    init {
        load()
    }

    override fun onEvent(event: LandlordRequestsEvent) {
        when (event) {
            LandlordRequestsEvent.Load -> load()
            is LandlordRequestsEvent.FilterChanged -> {
                updateState { it.copy(statusFilter = event.status) }
                load()
            }
            is LandlordRequestsEvent.Accept -> accept(event.requestId)
            is LandlordRequestsEvent.Reject -> reject(event.requestId, event.reason, event.note)
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, errorCode = null, errorMessage = null) }
        launch {
            getLandlordRequests(uiState.value.statusFilter).fold(
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

    private fun accept(requestId: String) {
        if (uiState.value.busyId != null) return
        updateState { it.copy(busyId = requestId, errorCode = null, errorMessage = null) }
        launch {
            acceptRequest(requestId).fold(
                onSuccess = { updated ->
                    updateState { it.copy(busyId = null) }
                    sendEffect(LandlordRequestsEffect.ShowMessage("Solicitud aceptada."))
                    loadQuiet()
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

    private fun reject(requestId: String, reason: String, note: String?) {
        if (uiState.value.busyId != null) return
        updateState { it.copy(busyId = requestId, errorCode = null, errorMessage = null) }
        launch {
            rejectRequest(requestId, reason, note).fold(
                onSuccess = { _ ->
                    updateState { it.copy(busyId = null) }
                    sendEffect(LandlordRequestsEffect.ShowMessage("Solicitud rechazada."))
                    loadQuiet()
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

    private fun loadQuiet() {
        launch {
            getLandlordRequests(uiState.value.statusFilter).onSuccess { page ->
                updateState { it.copy(items = page.items) }
            }
        }
    }
}
