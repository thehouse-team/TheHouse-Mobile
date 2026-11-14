package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.usecase.CreateAgreementUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class CreateAgreementUiState(
    val requestId: String = "",
    val monthlyRent: String = "",
    val deposit: String = "",
    val startDate: String = "",
    val durationMonths: String = "6",
    val paymentDay: String = "5",
    val isSubmitting: Boolean = false,
    val created: LandlordAgreement? = null,
    val errorMessage: String? = null,
)

sealed interface CreateAgreementEvent : UiEvent {
    data class MonthlyRentChanged(val value: String) : CreateAgreementEvent
    data class DepositChanged(val value: String) : CreateAgreementEvent
    data class StartDateChanged(val value: String) : CreateAgreementEvent
    data class DurationMonthsChanged(val value: String) : CreateAgreementEvent
    data class PaymentDayChanged(val value: String) : CreateAgreementEvent
    data object Submit : CreateAgreementEvent
}

sealed interface CreateAgreementEffect : UiEffect

class CreateAgreementViewModel(
    requestId: String,
    private val createAgreement: CreateAgreementUseCase,
) : BaseViewModel<CreateAgreementUiState, CreateAgreementEvent, CreateAgreementEffect>(
    CreateAgreementUiState(requestId = requestId),
) {

    override fun onEvent(event: CreateAgreementEvent) {
        when (event) {
            is CreateAgreementEvent.MonthlyRentChanged -> updateState { it.copy(monthlyRent = event.value, errorMessage = null) }
            is CreateAgreementEvent.DepositChanged -> updateState { it.copy(deposit = event.value, errorMessage = null) }
            is CreateAgreementEvent.StartDateChanged -> updateState { it.copy(startDate = event.value, errorMessage = null) }
            is CreateAgreementEvent.DurationMonthsChanged -> updateState { it.copy(durationMonths = event.value, errorMessage = null) }
            is CreateAgreementEvent.PaymentDayChanged -> updateState { it.copy(paymentDay = event.value, errorMessage = null) }
            CreateAgreementEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val state = uiState.value
        if (state.isSubmitting || state.created != null) return
        val monthlyRent = state.monthlyRent.toIntOrNull()
        val deposit = state.deposit.toIntOrNull()
        val duration = state.durationMonths.toIntOrNull()
        val paymentDay = state.paymentDay.toIntOrNull()
        val startDate = state.startDate.trim()

        val validation = when {
            monthlyRent == null || monthlyRent !in 500..100000 -> "Revisa los datos ingresados."
            deposit == null || deposit < 0 || deposit > monthlyRent * 2 -> "Revisa los datos ingresados."
            !Regex("\\d{4}-\\d{2}-\\d{2}").matches(startDate) -> "Revisa los datos ingresados."
            duration == null || duration !in 1..24 -> "Revisa los datos ingresados."
            paymentDay == null || paymentDay !in 1..28 -> "Revisa los datos ingresados."
            else -> null
        }
        if (validation != null) {
            updateState { it.copy(errorMessage = validation) }
            return
        }

        updateState { it.copy(isSubmitting = true, errorMessage = null) }
        launch {
            createAgreement(
                requestId = state.requestId,
                monthlyRent = monthlyRent!!,
                deposit = deposit!!,
                startDate = startDate,
                durationMonths = duration!!,
                paymentDay = paymentDay!!,
            ).fold(
                onSuccess = { agreement ->
                    updateState { it.copy(isSubmitting = false, created = agreement) }
                },
                onFailure = { throwable ->
                    val ex = throwable as? AgreementException
                    updateState {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = AgreementErrorMessages.map(ex),
                        )
                    }
                },
            )
        }
    }
}
