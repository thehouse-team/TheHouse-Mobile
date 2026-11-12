package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.DashboardSummary
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.usecase.ActivateLandlordUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingsUseCase
import com.thehouse.app.domain.usecase.GetLandlordProfileUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent

/** Estados UI del dashboard de arrendador: Loading/Empty/Content/Error/LandlordRequired. */
data class LandlordDashboardUiState(
    val isLoading: Boolean = false,
    val notLandlord: Boolean = false,
    val buildingCount: Int = 0,
    val buildings: List<Building> = emptyList(),
    val summary: DashboardSummary? = null,
    val errorCode: LandlordErrorCode? = null,
    val errorMessage: String? = null,
)

sealed interface LandlordDashboardEvent : UiEvent {
    data object Load : LandlordDashboardEvent
    data object Retry : LandlordDashboardEvent
}

sealed interface LandlordDashboardEffect : com.thehouse.app.presentation.common.UiEffect {
    data class ShowMessage(val text: String) : LandlordDashboardEffect
}

class LandlordDashboardViewModel(
    private val getProfile: GetLandlordProfileUseCase,
    private val getBuildings: GetLandlordBuildingsUseCase,
) : BaseViewModel<LandlordDashboardUiState, LandlordDashboardEvent, LandlordDashboardEffect>(
    LandlordDashboardUiState(),
) {

    init {
        load()
    }

    override fun onEvent(event: LandlordDashboardEvent) {
        when (event) {
            LandlordDashboardEvent.Load, LandlordDashboardEvent.Retry -> load()
        }
    }

    fun load() {
        updateState {
            it.copy(isLoading = true, notLandlord = false, errorCode = null, errorMessage = null)
        }
        launch {
            val profile = getProfile()
            profile.fold(
                onSuccess = { profileValue ->
                    if (profileValue == null) {
                        updateState { it.copy(isLoading = false, notLandlord = true) }
                        return@launch
                    }
                    loadBuildings()
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun loadBuildings() {
        launch {
            getBuildings().fold(
                onSuccess = { page ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            buildingCount = page.totalItems.toInt(),
                            buildings = page.items,
                            summary = page.summary,
                        )
                    }
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun handleFailure(throwable: Throwable) {
        val exception = throwable as? LandlordException
        updateState {
            it.copy(
                isLoading = false,
                notLandlord = exception?.code == LandlordErrorCode.LANDLORD_REQUIRED,
                errorCode = exception?.code,
                errorMessage = LandlordErrorMessages.map(exception),
            )
        }
    }
}

/** Alta del modo arrendador (POST /landlord/profile). */
data class LandlordProfileUiState(
    val fullName: String = "",
    val phone: String = "",
    val rentalMode: RentalMode = RentalMode.INDIVIDUAL,
    val rfc: String = "",
    val acceptedTerms: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val fullNameError: String? = if (fullName.length in 2..120) null else "Nombre de 2 a 120 caracteres."
    val phoneError: String? = if (PHONE_REGEX.matches(phone)) null else "Formato: +52 seguido de 10 dígitos."
    val rfcError: String? = when {
        rentalMode == RentalMode.INDIVIDUAL -> null
        rfc.length == 12 -> null
        else -> "RFC de persona moral: 12 caracteres."
    }
    val termsError: String? = if (acceptedTerms) null else "Debes aceptar los términos."
    val isValid: Boolean =
        fullNameError == null && phoneError == null && rfcError == null && termsError == null

    private companion object {
        val PHONE_REGEX = Regex("^\\+52\\d{10}$")
    }
}

sealed interface LandlordProfileEvent : UiEvent {
    data class FullNameChanged(val value: String) : LandlordProfileEvent
    data class PhoneChanged(val value: String) : LandlordProfileEvent
    data class RentalModeChanged(val mode: RentalMode) : LandlordProfileEvent
    data class RfcChanged(val value: String) : LandlordProfileEvent
    data class TermsToggled(val accepted: Boolean) : LandlordProfileEvent
    data object Submit : LandlordProfileEvent
}

/** Versión vigente de términos configurada en el Backend (LandlordTerms:Version). */
const val LANDLORD_TERMS_VERSION = "2026-11"

sealed interface LandlordProfileEffect : com.thehouse.app.presentation.common.UiEffect {
    data object Activated : LandlordProfileEffect
}

class LandlordProfileViewModel(
    private val activateLandlord: com.thehouse.app.domain.usecase.ActivateLandlordUseCase,
) : BaseViewModel<LandlordProfileUiState, LandlordProfileEvent, LandlordProfileEffect>(
    LandlordProfileUiState(),
) {

    override fun onEvent(event: LandlordProfileEvent) {
        when (event) {
            is LandlordProfileEvent.FullNameChanged ->
                updateState { it.copy(fullName = event.value) }
            is LandlordProfileEvent.PhoneChanged ->
                updateState { it.copy(phone = event.value) }
            is LandlordProfileEvent.RentalModeChanged ->
                updateState { it.copy(rentalMode = event.mode) }
            is LandlordProfileEvent.RfcChanged ->
                updateState { it.copy(rfc = event.value) }
            is LandlordProfileEvent.TermsToggled ->
                updateState { it.copy(acceptedTerms = event.accepted) }
            LandlordProfileEvent.Submit -> submit()
        }
    }

    /** Protección contra doble submit: un guard en isSaving. */
    private fun submit() {
        val current = uiState.value
        if (current.isSaving || !current.isValid) return
        updateState { it.copy(isSaving = true, errorMessage = null) }
        launch {
            activateLandlord(
                fullName = current.fullName.trim(),
                phone = current.phone.trim(),
                rentalMode = current.rentalMode,
                rfc = if (current.rentalMode == RentalMode.AGENCY) current.rfc.trim() else null,
                acceptedTermsVersion = LANDLORD_TERMS_VERSION,
            ).fold(
                onSuccess = { _ ->
                    updateState { it.copy(isSaving = false) }
                    sendEffect(LandlordProfileEffect.Activated)
                },
                onFailure = { throwable ->
                    val exception = throwable as? LandlordException
                    updateState {
                        it.copy(
                            isSaving = false,
                            errorMessage = LandlordErrorMessages.map(exception),
                        )
                    }
                },
            )
        }
    }
}
