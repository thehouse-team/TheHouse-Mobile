package com.thehouse.app.presentation.rental

import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.usecase.PreviewAgreementUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.landlord.AgreementErrorMessages

data class RentalCodeUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val retryAfterSeconds: Long? = null,
)

sealed interface RentalCodeEvent : UiEvent {
    data class CodeChanged(val value: String) : RentalCodeEvent
    data object Submit : RentalCodeEvent
}

sealed interface RentalCodeEffect : UiEffect {
    data class NavigateToPreview(val code: String) : RentalCodeEffect
}

class RentalCodeViewModel(
    private val previewAgreement: PreviewAgreementUseCase,
) : BaseViewModel<RentalCodeUiState, RentalCodeEvent, RentalCodeEffect>(RentalCodeUiState()) {

    override fun onEvent(event: RentalCodeEvent) {
        when (event) {
            is RentalCodeEvent.CodeChanged -> updateState { it.copy(code = event.value, errorMessage = null) }
            RentalCodeEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val raw = uiState.value.code.trim()
        if (uiState.value.isLoading) return
        if (raw.isBlank()) {
            updateState { it.copy(errorMessage = "Revisa los datos ingresados.") }
            return
        }
        updateState { it.copy(isLoading = true, errorMessage = null, retryAfterSeconds = null) }
        launch {
            previewAgreement(code = raw, qrToken = null).fold(
                onSuccess = { preview ->
                    updateState { it.copy(isLoading = false) }
                    sendEffect(RentalCodeEffect.NavigateToPreview(raw))
                },
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
}
