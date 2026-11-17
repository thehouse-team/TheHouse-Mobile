package com.thehouse.app.presentation.privacy

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.privacy.PrivacyPolicyUrl
import com.thehouse.app.domain.usecase.AccountDeletionResult
import com.thehouse.app.domain.usecase.DeleteAccountUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF de Privacidad + Eliminar cuenta (NV-15).
 *
 * Reglas:
 * - Sin URL HTTPS válida no se abre nada: estado informativo, sin crash.
 * - La eliminación solo se ofrece si existe contrato Backend ejecutable.
 * - La confirmación es obligatoria; cancelar no ejecuta nada.
 * - Loading + guard bloquean doble submit.
 * - Solo el éxito real emite [PrivacyEffect.AccountDeleted]; un fallo conserva
 *   la sesión y los datos locales.
 */
data class PrivacyUiState(
    val privacyPolicyUrl: String? = null,
    val isAuthenticated: Boolean = false,
    val isDeletionAvailable: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

sealed interface PrivacyEvent : UiEvent {
    data object OpenPrivacyPolicyClicked : PrivacyEvent
    data object DeleteAccountClicked : PrivacyEvent
    data object ConfirmDeleteClicked : PrivacyEvent
    data object CancelDeleteClicked : PrivacyEvent

    /** El navegador no pudo abrir la URL; la UI informa sin crash. */
    data object PrivacyPolicyLaunchFailed : PrivacyEvent
}

sealed interface PrivacyEffect : UiEffect {
    data class OpenPrivacyPolicy(val url: String) : PrivacyEffect
    data object AccountDeleted : PrivacyEffect
    data object RequireLogin : PrivacyEffect
}

class PrivacyViewModel(
    privacyPolicyUrl: String?,
    private val sessionState: StateFlow<SessionState>,
    private val deleteAccount: DeleteAccountUseCase,
) : BaseViewModel<PrivacyUiState, PrivacyEvent, PrivacyEffect>(
    PrivacyUiState(
        privacyPolicyUrl = PrivacyPolicyUrl.normalize(privacyPolicyUrl),
        isAuthenticated = sessionState.value is SessionState.Active,
        isDeletionAvailable = deleteAccount.isAvailable,
    ),
) {

    init {
        launch {
            sessionState.collect { state ->
                val isActive = state is SessionState.Active
                updateState {
                    it.copy(
                        isAuthenticated = isActive,
                        showDeleteConfirmation = if (isActive) {
                            it.showDeleteConfirmation
                        } else {
                            false
                        },
                    )
                }
            }
        }
    }

    override fun onEvent(event: PrivacyEvent) {
        when (event) {
            PrivacyEvent.OpenPrivacyPolicyClicked -> openPrivacyPolicy()
            PrivacyEvent.DeleteAccountClicked -> requestDelete()
            PrivacyEvent.ConfirmDeleteClicked -> confirmDelete()
            PrivacyEvent.CancelDeleteClicked -> cancelDelete()
            PrivacyEvent.PrivacyPolicyLaunchFailed -> updateState {
                it.copy(info = MESSAGE_PRIVACY_LAUNCH_FAILED)
            }
        }
    }

    private fun openPrivacyPolicy() {
        val url = PrivacyPolicyUrl.normalize(uiState.value.privacyPolicyUrl)
        if (url == null) {
            updateState { it.copy(info = MESSAGE_PRIVACY_BLOCKED) }
            return
        }
        sendEffect(PrivacyEffect.OpenPrivacyPolicy(url))
    }

    private fun requestDelete() {
        val current = uiState.value
        if (current.isDeleting) return
        if (!current.isAuthenticated) {
            sendEffect(PrivacyEffect.RequireLogin)
            return
        }
        if (!current.isDeletionAvailable) {
            updateState { it.copy(info = MESSAGE_DELETE_BLOCKED, error = null) }
            return
        }
        updateState {
            it.copy(
                showDeleteConfirmation = true,
                error = null,
                info = null,
            )
        }
    }

    private fun cancelDelete() {
        if (uiState.value.isDeleting) return
        updateState { it.copy(showDeleteConfirmation = false) }
    }

    private fun confirmDelete() {
        val current = uiState.value
        if (current.isDeleting) return
        if (!current.showDeleteConfirmation) return
        if (!current.isDeletionAvailable) {
            updateState {
                it.copy(showDeleteConfirmation = false, info = MESSAGE_DELETE_BLOCKED)
            }
            return
        }
        launch {
            updateState { it.copy(isDeleting = true, error = null, info = null) }
            when (val result = deleteAccount()) {
                AccountDeletionResult.Deleted -> {
                    updateState {
                        it.copy(isDeleting = false, showDeleteConfirmation = false)
                    }
                    sendEffect(PrivacyEffect.AccountDeleted)
                }
                AccountDeletionResult.BackendUnavailable -> {
                    updateState {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            info = MESSAGE_DELETE_BLOCKED,
                        )
                    }
                }
                is AccountDeletionResult.Failed -> {
                    updateState {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            error = result.error.toSafeMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun DomainError.toSafeMessage(): String = when (this) {
        DomainError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
        DomainError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
        DomainError.Unauthorized -> "Tu sesión expiró. Inicia sesión de nuevo."
        DomainError.AccountSuspended -> "Tu cuenta está suspendida."
        DomainError.NotFound -> "La cuenta ya no está disponible."
        DomainError.Conflict -> "No se pudo completar la eliminación. Intenta de nuevo."
        DomainError.Gone -> "La cuenta ya no está disponible."
        DomainError.RateLimited -> "Demasiados intentos. Espera un momento e intenta de nuevo."
        DomainError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
        DomainError.Unknown -> "No se pudo eliminar la cuenta. Intenta de nuevo."
    }

    companion object {
        const val MESSAGE_PRIVACY_BLOCKED =
            "El aviso de privacidad aún no está publicado. No se abrirá una URL no válida."

        const val MESSAGE_DELETE_BLOCKED =
            "La eliminación de cuenta todavía no está disponible. No se ha eliminado nada."

        const val MESSAGE_PRIVACY_LAUNCH_FAILED =
            "No se pudo abrir el navegador. Verifica que tu dispositivo tenga uno disponible."
    }
}
