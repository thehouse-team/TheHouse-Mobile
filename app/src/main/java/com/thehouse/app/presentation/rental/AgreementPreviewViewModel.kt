package com.thehouse.app.presentation.rental

import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.usecase.AcceptAgreementUseCase
import com.thehouse.app.domain.usecase.PreviewAgreementUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.landlord.AgreementErrorMessages

data class AgreementPreviewUiState(
    val code: String = "",
    val isLoading: Boolean = true,
    val isAccepting: Boolean = false,
    val preview: AgreementPreview? = null,
    val acceptedTerms: Boolean = false,
    val errorMessage: String? = null,
    val retryAfterSeconds: Long? = null,
)

sealed interface AgreementPreviewEvent : UiEvent {
    data object Load : AgreementPreviewEvent
    data class AcceptedTermsChanged(val value: Boolean) : AgreementPreviewEvent
    data object Accept : AgreementPreviewEvent
}

sealed interface AgreementPreviewEffect : UiEffect {
    data object NavigateToMyRental : AgreementPreviewEffect
}

class AgreementPreviewViewModel(
    private val code: String,
    private val previewAgreement: PreviewAgreementUseCase,
    private val acceptAgreement: AcceptAgreementUseCase,
) : BaseViewModel<AgreementPreviewUiState, AgreementPreviewEvent, AgreementPreviewEffect>(
    AgreementPreviewUiState(code = code),
) {

    init { load() }

    override fun onEvent(event: AgreementPreviewEvent) {
        when (event) {
            AgreementPreviewEvent.Load -> load()
            is AgreementPreviewEvent.AcceptedTermsChanged -> updateState { it.copy(acceptedTerms = event.value) }
            AgreementPreviewEvent.Accept -> accept()
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null, retryAfterSeconds = null) }
        launch {
            previewAgreement(code = code, qrToken = null).fold(
                onSuccess = { preview -> updateState { it.copy(isLoading = false, preview = preview) } },
                onFailure = { throwable ->
                    val ex = throwable as? AgreementException
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = AgreementErrorMessages.map(ex),
                            retryAfterSeconds = ex?.retryAfterSeconds,
                        )
                    }
                },
            )
        }
    }

    private fun accept() {
        val state = uiState.value
        if (state.isAccepting || !state.acceptedTerms || state.preview == null) return
        updateState { it.copy(isAccepting = true, errorMessage = null, retryAfterSeconds = null) }
        launch {
            acceptAgreement(code = code, qrToken = null, acceptedTerms = true).fold(
                onSuccess = {
                    updateState { it.copy(isAccepting = false) }
                    sendEffect(AgreementPreviewEffect.NavigateToMyRental)
                },
                onFailure = { throwable ->
                    val ex = throwable as? AgreementException
                    updateState {
                        it.copy(
                            isAccepting = false,
                            errorMessage = AgreementErrorMessages.map(ex),
                            retryAfterSeconds = ex?.retryAfterSeconds,
                        )
                    }
                },
            )
        }
    }
}
