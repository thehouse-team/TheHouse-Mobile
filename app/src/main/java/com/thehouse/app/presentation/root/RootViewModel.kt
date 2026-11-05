package com.thehouse.app.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehouse.app.di.AppContainer
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.presentation.navigation.AppRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVVM + UDF: decisión de la pantalla inicial según sesión y onboarding.
 *
 * Reglas U2:
 * - Anonymous → Login.
 * - Active + onboarding pendiente → Onboarding (3 pantallas → bienvenida → inicio).
 * - Active + onboarding completado → Home (inicio provisional).
 * - NO declara sesión "siempre viva": sin `/auth/refresh` confirmado (BLOCKED-
 *   BACKEND), un token vencido limpia la sesión y regresa a Login.
 */
data class RootUiState(
    val isResolving: Boolean = true,
    val targetRoute: String? = null,
)

class RootViewModel(
    container: AppContainer,
) : ViewModel() {

    private val session = container.sessionStore.state
    private val onboardingRepository = container.onboardingRepository

    private val mutableState = MutableStateFlow(RootUiState())
    val state: StateFlow<RootUiState> = mutableState.asStateFlow()

    fun collect(sessionFlow: Flow<SessionState>) {
        viewModelScope.launch {
            sessionFlow.collect { value -> resolve(value) }
        }
    }

    private suspend fun resolve(value: SessionState) {
        val userKeyDerived = onboardingRepository.keyFor(sessionUserId(value))
        when (value) {
            SessionState.Anonymous -> updateTarget(AppRoutes.login.route)
            is SessionState.Active -> {
                val completed = runCatching {
                    onboardingRepository.isCompleted(userKeyDerived)
                }.getOrDefault(false)
                val target = if (completed) AppRoutes.home.route else AppRoutes.onboarding.route
                updateTarget(target)
            }
        }
    }

    fun sessionUserId(state: SessionState): String? =
        (state as? SessionState.Active)?.session?.userId

    fun userId(): String? = sessionUserId(session.value)

    private fun updateTarget(route: String) {
        mutableState.update { it.copy(isResolving = false, targetRoute = route) }
    }
}
