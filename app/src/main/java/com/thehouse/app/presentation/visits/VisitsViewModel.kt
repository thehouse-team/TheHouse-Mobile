package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.usecase.CancelVisitUseCase
import com.thehouse.app.domain.usecase.GetVisitsUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class VisitsUiState(
    val isLoading: Boolean = false,
    val items: List<Visit> = emptyList(),
    val statusFilter: VisitStatus? = VisitStatus.REQUESTED,
    val errorCode: VisitErrorCode? = null,
    val domainError: DomainError? = null,
    val cancellingId: String? = null,
)

sealed interface VisitsEvent : UiEvent {
    data object Load : VisitsEvent
    data class FilterChanged(val status: VisitStatus?) : VisitsEvent
    data class Cancel(val visitId: String) : VisitsEvent
}

sealed interface VisitsEffect : UiEffect {
    data class ShowMessage(val text: String) : VisitsEffect
}

class VisitsViewModel(
    private val getVisits: GetVisitsUseCase,
    private val cancelVisit: CancelVisitUseCase,
) : BaseViewModel<VisitsUiState, VisitsEvent, VisitsEffect>(VisitsUiState()) {

    init {
        load()
    }

    override fun onEvent(event: VisitsEvent) {
        when (event) {
            VisitsEvent.Load -> load()
            is VisitsEvent.FilterChanged -> {
                updateState { it.copy(statusFilter = event.status) }
                load()
            }
            is VisitsEvent.Cancel -> cancel(event.visitId)
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, errorCode = null, domainError = null) }
        launch {
            val result = getVisits(uiState.value.statusFilter)
            result.fold(
                onSuccess = { page ->
                    updateState { it.copy(isLoading = false, items = page.items) }
                },
                onFailure = { throwable ->
                    val exception = throwable as? VisitException
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorCode = exception?.code ?: VisitErrorCode.UNKNOWN,
                            domainError = exception?.domainError ?: DomainError.Unknown,
                        )
                    }
                },
            )
        }
    }

    private fun cancel(visitId: String) {
        if (uiState.value.cancellingId != null) return
        updateState { it.copy(cancellingId = visitId) }
        launch {
            val result = cancelVisit(visitId)
            result.fold(
                onSuccess = { updated ->
                    updateState {
                        it.copy(
                            cancellingId = null,
                            items = it.items.map { item ->
                                if (item.id == updated.id) updated else item
                            },
                        )
                    }
                    sendEffect(VisitsEffect.ShowMessage("Visita cancelada."))
                },
                onFailure = { throwable ->
                    val exception = throwable as? VisitException
                    updateState {
                        it.copy(
                            cancellingId = null,
                            errorCode = exception?.code ?: VisitErrorCode.UNKNOWN,
                            domainError = exception?.domainError ?: DomainError.Unknown,
                        )
                    }
                },
            )
        }
    }
}