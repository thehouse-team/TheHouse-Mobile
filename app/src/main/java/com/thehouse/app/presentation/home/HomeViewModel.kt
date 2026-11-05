package com.thehouse.app.presentation.home

import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

/**
 * MVVM + UDF de la pantalla de inicio provisional (U2).
 *
 * Muestra el saludo con el nombre disponible (del login/register real) y el
 * botón `Cerrar sesión`. NO declara sesión "siempre viva": sin `/auth/refresh`
 * confirmado, un token vencido limpia la sesión y regresa a Login.
 */
data class HomeUiState(
    val displayName: String = "",
    val email: String = "",
    val isLoggingOut: Boolean = false,
)

sealed interface HomeEvent : UiEvent {
    data object LogoutClicked : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data object LoggedOut : HomeEffect
}

class HomeViewModel(
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>(HomeUiState()) {

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LogoutClicked -> logout()
        }
    }

    fun logout() {
        if (uiState.value.isLoggingOut) return
        launch {
            updateState { it.copy(isLoggingOut = true) }
            // El logout SIEMPRE limpia el estado local; el backend es best-effort.
            logoutUseCase()
            updateState { it.copy(isLoggingOut = false) }
            sendEffect(HomeEffect.LoggedOut)
        }
    }
}
