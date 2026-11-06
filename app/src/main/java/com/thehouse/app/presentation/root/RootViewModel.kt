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
 * Reglas U2 + D3 (sesión persistente) + D4 (Guest):
 * - Active + onboarding pendiente → Onboarding (3 pantallas → bienvenida → inicio).
 * - Active + onboarding completado → Home (shell con bottom nav).
 * - Anonymous + onboarding completado (clave local, p. ej. tras Omitir)
 *   → Home como Guest (inicio/explorar/smart_match; favoritos con muro).
 * - Anonymous + onboarding pendiente → Onboarding (primer uso; Inicia sesión
 *   sigue disponible dentro del onboarding).
 * - D3: al iniciar, `RootViewModel` dispara UNA restauración de sesión
 *   (refresh 200 + /me 200 → Active; refresh 401 / sin refresh → Anonymous).
 *   No se inventa sesión offline. Logout → caída a Guest/Onboarding
 *   según la marca local (sin forzar Login); las acciones protegidas
 *   llevan a Login explícitamente.
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
    private val restoreSession = container.restoreSession

    private val mutableState = MutableStateFlow(RootUiState())
    val state: StateFlow<RootUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            // Restauración silenciosa al arranque; el estado resultante
            // (Active/Anonymous) se observa vía [collect] para decidir la ruta.
            runCatching { restoreSession() }
        }
    }

    fun collect(sessionFlow: Flow<SessionState>) {
        viewModelScope.launch {
            sessionFlow.collect { value -> resolve(value) }
        }
    }

    private suspend fun resolve(value: SessionState) {
        val userKeyDerived = onboardingRepository.keyFor(sessionUserId(value))
        when (value) {
            SessionState.Anonymous -> {
                // Guest: si ya se completó/omitió el onboarding local, el
                // shell de Home abre sin exigir cuenta. Si es primer uso,
                // el onboarding ofrece Omitir (Guest) o Inicia sesión.
                val guestCompleted = runCatching {
                    onboardingRepository.isCompleted(onboardingRepository.keyFor(null))
                }.getOrDefault(false)
                updateTarget(
                    if (guestCompleted) AppRoutes.home.route else AppRoutes.onboarding.route,
                )
            }
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
