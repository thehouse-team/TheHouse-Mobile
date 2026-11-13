package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.usecase.GetLandlordAgreementsUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

data class LandlordAgreementsUiState(
    val isLoading: Boolean = false,
    val items: List<LandlordAgreement> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 0,
    val selectedStatus: AgreementStatus? = null,
    val errorMessage: String? = null,
)

sealed interface LandlordAgreementsEvent : UiEvent {
    data object Load : LandlordAgreementsEvent
    data object Retry : LandlordAgreementsEvent
    data class FilterChanged(val status: AgreementStatus?) : LandlordAgreementsEvent
    data object NextPage : LandlordAgreementsEvent
    data object PrevPage : LandlordAgreementsEvent
}

sealed interface LandlordAgreementsEffect : UiEffect

class LandlordAgreementsViewModel(
    private val getLandlordAgreements: GetLandlordAgreementsUseCase,
) : BaseViewModel<LandlordAgreementsUiState, LandlordAgreementsEvent, LandlordAgreementsEffect>(
    LandlordAgreementsUiState(),
) {

    init {
        load()
    }

    override fun onEvent(event: LandlordAgreementsEvent) {
        when (event) {
            LandlordAgreementsEvent.Load, LandlordAgreementsEvent.Retry -> load(page = uiState.value.page)
            is LandlordAgreementsEvent.FilterChanged -> {
                updateState { it.copy(selectedStatus = event.status, page = 1) }
                load(page = 1)
            }
            LandlordAgreementsEvent.NextPage -> {
                val next = uiState.value.page + 1
                if (next <= uiState.value.totalPages.coerceAtLeast(1)) load(page = next)
            }
            LandlordAgreementsEvent.PrevPage -> {
                val prev = uiState.value.page - 1
                if (prev >= 1) load(page = prev)
            }
        }
    }

    private fun load(page: Int = 1) {
        updateState { it.copy(isLoading = true, errorMessage = null) }
        launch {
            val status = uiState.value.selectedStatus
            getLandlordAgreements(status = status, page = page, pageSize = 20).fold(
                onSuccess = { result ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            page = result.page,
                            totalPages = result.totalPages,
                        )
                    }
                },
                onFailure = { throwable ->
                    val ex = throwable as? AgreementException
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = AgreementErrorMessages.map(ex),
                        )
                    }
                },
            )
        }
    }
}
