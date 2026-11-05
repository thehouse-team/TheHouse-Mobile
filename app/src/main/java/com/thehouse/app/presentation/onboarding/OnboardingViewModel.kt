package com.thehouse.app.presentation.onboarding

import com.thehouse.app.domain.repository.OnboardingRepository
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent

/**
 * MVVM + UDF para el onboarding de primer ingreso.
 *
 * 3 pantallas de introducción con botón `Omitir`. La marca
 * `onboardingCompleted` se guarda por usuario con DataStore (dato no sensible).
 * La personalización por usuario remota (GET /api/v1/users/me) NO está
 * confirmada y permanece BLOCKED-BACKEND: la bienvenida se llena con el nombre
 * que el backend entregó en login/register.
 */
data class OnboardingUiState(
    val page: Int = 0,
    val totalPages: Int = 3,
    val key: String = "local",
)

sealed interface OnboardingEvent : UiEvent {
    data class PageChanged(val index: Int) : OnboardingEvent
    data object SkipClicked : OnboardingEvent
    data object NextClicked : OnboardingEvent
}

sealed interface OnboardingEffect : UiEffect {
    data object NavigateToWelcome : OnboardingEffect
}

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val userKey: String?,
) : BaseViewModel<OnboardingUiState, OnboardingEvent, OnboardingEffect>(
    OnboardingUiState(key = onboardingRepository.keyFor(userKey)),
) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.PageChanged -> updateState { it.copy(page = event.index.coerceIn(0, it.totalPages - 1)) }
            OnboardingEvent.SkipClicked -> complete()
            OnboardingEvent.NextClicked -> {
                val next = uiState.value.page + 1
                if (next >= uiState.value.totalPages) {
                    complete()
                } else {
                    updateState { it.copy(page = next) }
                }
            }
        }
    }

    fun complete() {
        launch {
            runCatching { onboardingRepository.setCompleted(uiState.value.key, true) }
            sendEffect(OnboardingEffect.NavigateToWelcome)
        }
    }
}
