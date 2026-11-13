package com.thehouse.app.presentation.rental

import com.thehouse.app.domain.error.AgreementErrorCode
import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.usecase.GetMyRentalUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.landlord.AgreementErrorMessages

data class MyRentalUiState(
    val isLoading: Boolean = false,
    val rental: MyRental? = null,
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface MyRentalEvent : UiEvent {
    data object Load : MyRentalEvent
}

sealed interface MyRentalEffect : UiEffect

class MyRentalViewModel(
    private val getMyRental: GetMyRentalUseCase,
) : BaseViewModel<MyRentalUiState, MyRentalEvent, MyRentalEffect>(MyRentalUiState()) {

    init { load() }

    override fun onEvent(event: MyRentalEvent) {
        when (event) {
            MyRentalEvent.Load -> load()
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null, emptyMessage = null) }
        launch {
            getMyRental().fold(
                onSuccess = { rental -> updateState { it.copy(isLoading = false, rental = rental) } },
                onFailure = { throwable ->
                    val ex = throwable as? AgreementException
                    if (ex?.code == AgreementErrorCode.NO_ACTIVE_RENTAL) {
                        updateState { it.copy(isLoading = false, rental = null, emptyMessage = "No tienes una renta activa.") }
                    } else {
                        updateState { it.copy(isLoading = false, errorMessage = AgreementErrorMessages.map(ex)) }
                    }
                },
            )
        }
    }
}
