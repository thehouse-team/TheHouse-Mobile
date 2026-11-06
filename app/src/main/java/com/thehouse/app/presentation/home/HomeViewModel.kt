package com.thehouse.app.presentation.home

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF del Home (D4, NV-05, prototipo 07-inicio).
 *
 * - Guest (Anonymous) abre Home sin sesión falsa ni tokens fake.
 * - Acciones protegidas (favorito, contactar, publicar) como Guest
 *   emiten [HomeEffect.RequireLogin] (UX clara → Login).
 * - Autenticado (Active) nunca recibe bloqueo Guest.
 * - Logout limpia siempre el estado local (D3 intacto).
 * - Los datos visibles son fixture de prototipo
 *   ([com.thehouse.app.presentation.demo.PrototypeProperties]),
 *   no respuesta Backend.
 */
data class HomeUiState(
    val displayName: String = "",
    val email: String = "",
    val isLoggingOut: Boolean = false,
    val isGuest: Boolean = false,
    val isAuthenticated: Boolean = false,
)

sealed interface HomeEvent : UiEvent {
    data object LogoutClicked : HomeEvent
    /** Guardar favorito (acción protegida como Guest). */
    data object FavoriteClicked : HomeEvent
    /** Contactar propietario (acción protegida como Guest). */
    data object ContactClicked : HomeEvent
    /** Publicar alojamiento (acción protegida como Guest). */
    data object PublishClicked : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data object LoggedOut : HomeEffect
    data object RequireLogin : HomeEffect
}

class HomeViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>(HomeUiState()) {

    init {
        updateState {
            val isGuest = sessionState.value is SessionState.Anonymous
            it.copy(
                isGuest = isGuest,
                isAuthenticated = !isGuest,
            )
        }
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LogoutClicked -> logout()
            HomeEvent.FavoriteClicked -> guard(ProtectedAction.SaveFavorite)
            HomeEvent.ContactClicked -> guard(ProtectedAction.Contact)
            HomeEvent.PublishClicked -> guard(ProtectedAction.Publish)
        }
    }

    /** Protege acciones que requieren autenticación. */
    fun guard(action: ProtectedAction) {
        val isAuthenticated = sessionState.value is SessionState.Active
        updateState { it.copy(isGuest = !isAuthenticated, isAuthenticated = isAuthenticated) }
        if (AccessPolicy.shouldRedirectToLogin(action, isAuthenticated)) {
            sendEffect(HomeEffect.RequireLogin)
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
