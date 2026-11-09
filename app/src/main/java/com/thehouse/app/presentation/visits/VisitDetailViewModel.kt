package com.thehouse.app.presentation.visits

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.usecase.CancelVisitUseCase
import com.thehouse.app.domain.usecase.GetVisitUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class VisitDetailUiState(
    val isLoading: Boolean = true,
    val visit: Visit? = null,
    val errorCode: VisitErrorCode? = null,
    val domainError: DomainError? = null,
    val isCancelling: Boolean = false,
)

sealed interface VisitDetailEvent : UiEvent {
    data object Load : VisitDetailEvent
    data object Cancel : VisitDetailEvent
}

sealed interface VisitDetailEffect : UiEffect {
    data class ShowMessage(val text: String) : VisitDetailEffect
}

class VisitDetailViewModel(
    private val visitId: String,
    private val getVisit: GetVisitUseCase,
    private val cancelVisit: CancelVisitUseCase,
) : BaseViewModel<VisitDetailUiState, VisitDetailEvent, VisitDetailEffect>(VisitDetailUiState()) {

    init {
        load()
    }

    override fun onEvent(event: VisitDetailEvent) {
        when (event) {
            VisitDetailEvent.Load -> load()
            VisitDetailEvent.Cancel -> cancel()
        }
    }

    fun load() {
        updateState { it.copy(isLoading = true, errorCode = null, domainError = null) }
        launch {
            getVisit(visitId).fold(
                onSuccess = { visit -> updateState { state -> state.copy(isLoading = false, visit = visit) } },
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

    private fun cancel() {
        val current = uiState.value.visit ?: return
        if (uiState.value.isCancelling) return
        updateState { it.copy(isCancelling = true) }
        launch {
            cancelVisit(current.id).fold(
                onSuccess = { updated ->
                    updateState { it.copy(isCancelling = false, visit = updated) }
                    sendEffect(VisitDetailEffect.ShowMessage("Visita cancelada."))
                },
                onFailure = { throwable ->
                    val exception = throwable as? VisitException
                    updateState {
                        it.copy(
                            isCancelling = false,
                            errorCode = exception?.code ?: VisitErrorCode.UNKNOWN,
                            domainError = exception?.domainError ?: DomainError.Unknown,
                        )
                    }
                },
            )
        }
    }
}