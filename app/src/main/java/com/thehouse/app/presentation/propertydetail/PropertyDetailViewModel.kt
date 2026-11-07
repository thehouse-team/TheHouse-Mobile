package com.thehouse.app.presentation.propertydetail

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.usecase.GetPropertyByIdUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.flow.StateFlow

sealed interface PropertyDetailUiState {
    data object Loading : PropertyDetailUiState
    data class Content(val property: Property) : PropertyDetailUiState
    data object NotFound : PropertyDetailUiState
    data class Error(val error: UiError) : PropertyDetailUiState
}

sealed interface PropertyDetailEvent : UiEvent {
    data object Retry : PropertyDetailEvent
    data object FavoriteClicked : PropertyDetailEvent
    data object ContactClicked : PropertyDetailEvent
}

sealed interface PropertyDetailEffect : UiEffect {
    data object RequireLogin : PropertyDetailEffect
    data class ActionUnavailable(val action: ProtectedAction) : PropertyDetailEffect
}

class PropertyDetailViewModel(
    private val propertyId: String,
    private val getPropertyById: GetPropertyByIdUseCase,
    private val sessionState: StateFlow<SessionState>,
) : BaseViewModel<PropertyDetailUiState, PropertyDetailEvent, PropertyDetailEffect>(
    PropertyDetailUiState.Loading,
) {
    private var requestInFlight = false

    init {
        load()
    }

    override fun onEvent(event: PropertyDetailEvent) {
        when (event) {
            PropertyDetailEvent.Retry -> {
                if (uiState.value is PropertyDetailUiState.Error ||
                    uiState.value is PropertyDetailUiState.NotFound
                ) {
                    load()
                }
            }
            PropertyDetailEvent.FavoriteClicked -> guard(ProtectedAction.SaveFavorite)
            PropertyDetailEvent.ContactClicked -> guard(ProtectedAction.Contact)
        }
    }

    private fun load() {
        if (requestInFlight) return
        requestInFlight = true
        setState(PropertyDetailUiState.Loading)
        launch {
            try {
                val result = getPropertyById(propertyId)
                setState(
                    result.fold(
                        onSuccess = { PropertyDetailUiState.Content(it) },
                        onFailure = {
                            val error = (it as? DomainException)?.error ?: DomainError.Unknown
                            if (error == DomainError.NotFound) {
                                PropertyDetailUiState.NotFound
                            } else {
                                PropertyDetailUiState.Error(error.toUiError())
                            }
                        },
                    ),
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private fun guard(action: ProtectedAction) {
        if (uiState.value !is PropertyDetailUiState.Content) return
        val isAuthenticated = sessionState.value is SessionState.Active
        if (AccessPolicy.shouldRedirectToLogin(action, isAuthenticated)) {
            sendEffect(PropertyDetailEffect.RequireLogin)
        } else {
            // Como D4/D5: sin persistencia de favoritos ni llamadas de contacto.
            // Se explica la indisponibilidad; nunca se simula un guardado/envío.
            sendEffect(PropertyDetailEffect.ActionUnavailable(action))
        }
    }
}
