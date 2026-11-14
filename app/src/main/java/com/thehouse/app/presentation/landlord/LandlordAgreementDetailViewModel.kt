package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.usecase.CancelAgreementUseCase
import com.thehouse.app.domain.usecase.EndAgreementUseCase
import com.thehouse.app.domain.usecase.GetLandlordAgreementUseCase
import com.thehouse.app.domain.usecase.IssueAgreementCodeUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class LandlordAgreementDetailUiState(
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val agreement: LandlordAgreement? = null,
    val errorMessage: String? = null,
)

sealed interface LandlordAgreementDetailEvent : UiEvent {
    data object Load : LandlordAgreementDetailEvent
    data object IssueCode : LandlordAgreementDetailEvent
    data object Cancel : LandlordAgreementDetailEvent
    data class End(val reason: String?, val note: String?) : LandlordAgreementDetailEvent
}

sealed interface LandlordAgreementDetailEffect : UiEffect

class LandlordAgreementDetailViewModel(
    private val agreementId: String,
    private val getAgreement: GetLandlordAgreementUseCase,
    private val issueCode: IssueAgreementCodeUseCase,
    private val cancelAgreement: CancelAgreementUseCase,
    private val endAgreement: EndAgreementUseCase,
) : BaseViewModel<LandlordAgreementDetailUiState, LandlordAgreementDetailEvent, LandlordAgreementDetailEffect>(
    LandlordAgreementDetailUiState(),
) {

    init { load() }

    override fun onEvent(event: LandlordAgreementDetailEvent) {
        when (event) {
            LandlordAgreementDetailEvent.Load -> load()
            LandlordAgreementDetailEvent.IssueCode -> issue()
            LandlordAgreementDetailEvent.Cancel -> cancel()
            is LandlordAgreementDetailEvent.End -> end(event.reason, event.note)
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null) }
        launch {
            getAgreement(agreementId).fold(
                onSuccess = { a -> updateState { it.copy(isLoading = false, agreement = a) } },
                onFailure = { t -> updateState { it.copy(isLoading = false, errorMessage = AgreementErrorMessages.map(t as? AgreementException)) } },
            )
        }
    }

    private fun issue() {
        if (uiState.value.isBusy) return
        updateState { it.copy(isBusy = true, errorMessage = null) }
        launch {
            issueCode(agreementId).fold(
                onSuccess = { a -> updateState { it.copy(isBusy = false, agreement = a) } },
                onFailure = { t -> updateState { it.copy(isBusy = false, errorMessage = AgreementErrorMessages.map(t as? AgreementException)) } },
            )
        }
    }

    private fun cancel() {
        if (uiState.value.isBusy) return
        updateState { it.copy(isBusy = true, errorMessage = null) }
        launch {
            cancelAgreement(agreementId).fold(
                onSuccess = { a -> updateState { it.copy(isBusy = false, agreement = a) } },
                onFailure = { t -> updateState { it.copy(isBusy = false, errorMessage = AgreementErrorMessages.map(t as? AgreementException)) } },
            )
        }
    }

    private fun end(reason: String?, note: String?) {
        if (uiState.value.isBusy) return
        if ((note?.length ?: 0) > 300) {
            updateState { it.copy(errorMessage = "Revisa los datos ingresados.") }
            return
        }
        updateState { it.copy(isBusy = true, errorMessage = null) }
        launch {
            endAgreement(agreementId, reason?.takeIf { it.isNotBlank() }, note?.takeIf { it.isNotBlank() }).fold(
                onSuccess = { a -> updateState { it.copy(isBusy = false, agreement = a) } },
                onFailure = { t -> updateState { it.copy(isBusy = false, errorMessage = AgreementErrorMessages.map(t as? AgreementException)) } },
            )
        }
    }
}
