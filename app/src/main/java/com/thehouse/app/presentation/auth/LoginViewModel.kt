package com.thehouse.app.presentation.auth

import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.usecase.LoginUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.common.toUiError
import kotlinx.coroutines.launch

/**
 * MVVM + UDF para la pantalla de Login (RF-02).
 *
 * Solo se usan modelos de Domain; los errores llegan como [DomainError] dentro
 * de [DomainException] y se traducen a mensajes en español para la UI.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

sealed interface LoginEvent : com.thehouse.app.presentation.common.UiEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object PasswordVisibilityToggled : LoginEvent
    data object LoginClicked : LoginEvent
}

sealed interface LoginEffect : com.thehouse.app.presentation.common.UiEffect {
    data object NavigateAfterLogin : LoginEffect
    data class RegisterCompleted(val name: String?) : LoginEffect
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : BaseViewModel<LoginUiState, LoginEvent, LoginEffect>(LoginUiState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> updateState { it.copy(email = event.value, error = null) }
            is LoginEvent.PasswordChanged -> updateState { it.copy(password = event.value, error = null) }
            LoginEvent.PasswordVisibilityToggled -> updateState { it.copy(passwordVisible = !it.passwordVisible) }
            LoginEvent.LoginClicked -> submit()
        }
    }

    private fun submit() {
        val current = uiState.value
        if (current.isLoading) return
        val validation = loginUseCase.validate(current.email, current.password)
        if (!validation.isValid) {
            updateState { it.copy(error = validation.asError()) }
            return
        }
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            loginUseCase(current.email, current.password).fold(
                onSuccess = { _ ->
                    updateState { it.copy(isLoading = false) }
                    sendEffect(LoginEffect.NavigateAfterLogin)
                },
                onFailure = { failure ->
                    updateState { it.copy(isLoading = false, error = failure.toErrorText()) }
                },
            )
        }
    }
}

/** Traducción de presentación de cualquier error de dominio a mensaje en español. */
fun Throwable.toErrorText(): String {
    val domainError = (this as? DomainException)?.error
    return UiErrorMessages.map(
        domainError?.toUiError() ?: com.thehouse.app.presentation.common.UiError.Unknown,
    )
}
