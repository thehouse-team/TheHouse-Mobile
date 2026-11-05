package com.thehouse.app.presentation.auth

import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.usecase.RegisterUseCase
import com.thehouse.app.presentation.auth.toErrorText
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.launch

/**
 * MVVM + UDF para la pantalla de Crear cuenta (RF-01).
 *
 * Consentimientos obligatorios: términos y aviso de privacidad. El enlace a la
 * URL del aviso se mantiene pendiente (`BLOCKED-PRIVACY-URL`) y la UI no
 * inventa enlace: muestra el texto y queda deshabilitada la navegación externa.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface RegisterEvent : UiEvent {
    data class NameChanged(val value: String) : RegisterEvent
    data class EmailChanged(val value: String) : RegisterEvent
    data class PasswordChanged(val value: String) : RegisterEvent
    data object PasswordVisibilityToggled : RegisterEvent
    data class TermsToggled(val accepted: Boolean) : RegisterEvent
    data class PrivacyToggled(val accepted: Boolean) : RegisterEvent
    data object RegisterClicked : RegisterEvent
}

sealed interface RegisterEffect : UiEffect {
    /** El backend permitió iniciar sesión tras registrar → primer ingreso. */
    data object NavigateToOnboarding : RegisterEffect

    /** Registro creado sin sesión automática → volver a login. */
    data object NavigateToLogin : RegisterEffect
}

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
) : BaseViewModel<RegisterUiState, RegisterEvent, RegisterEffect>(RegisterUiState()) {

    override fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> updateState { it.copy(name = event.value, error = null) }
            is RegisterEvent.EmailChanged -> updateState { it.copy(email = event.value, error = null) }
            is RegisterEvent.PasswordChanged -> updateState { it.copy(password = event.value, error = null) }
            RegisterEvent.PasswordVisibilityToggled -> updateState { it.copy(passwordVisible = !it.passwordVisible) }
            is RegisterEvent.TermsToggled -> updateState { it.copy(acceptedTerms = event.accepted) }
            is RegisterEvent.PrivacyToggled -> updateState { it.copy(acceptedPrivacy = event.accepted) }
            RegisterEvent.RegisterClicked -> submit()
        }
    }

    private fun submit() {
        val current = uiState.value
        if (current.isLoading) return
        val validation = registerUseCase.validate(
            name = current.name,
            email = current.email,
            password = current.password,
            acceptedTerms = current.acceptedTerms,
            acceptedPrivacy = current.acceptedPrivacy,
        )
        if (!validation.isValid) {
            updateState { it.copy(error = validation.asError()) }
            return
        }
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            registerUseCase(
                name = current.name,
                email = current.email,
                password = current.password,
                acceptedTerms = current.acceptedTerms,
                acceptedPrivacy = current.acceptedPrivacy,
            ).fold(
                onSuccess = { session ->
                    updateState { it.copy(isLoading = false) }
                    // No se asume login automático: si la respuesta permitió
                    // sesión se va a onboarding; de lo contrario a login.
                    if (session != null) {
                        sendEffect(RegisterEffect.NavigateToOnboarding)
                    } else {
                        sendEffect(RegisterEffect.NavigateToLogin)
                    }
                },
                onFailure = { failure ->
                    updateState { it.copy(isLoading = false, error = failure.toErrorText()) }
                },
            )
        }
    }
}
