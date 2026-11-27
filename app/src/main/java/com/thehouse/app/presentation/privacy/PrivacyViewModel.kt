package com.thehouse.app.presentation.privacy

import com.thehouse.app.domain.error.AccountDeletionError
import com.thehouse.app.domain.error.AccountDeletionException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.DeletionPreview
import com.thehouse.app.domain.model.LegalInfo
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.privacy.PrivacyPolicyUrl
import com.thehouse.app.domain.repository.LegalRepository
import com.thehouse.app.domain.usecase.AccountDeletionResult
import com.thehouse.app.domain.usecase.DeleteAccountUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF de Privacidad, Aceptación legal y Eliminar cuenta (D19).
 *
 * - Carga `GET /legal` (URLs null seguras) y el estado `legalUpToDate`.
 * - Aceptación `PUT /users/me/legal` con 409 → recarga sin loop.
 * - Eliminación con password + confirmación explícita "ELIMINAR" y preview.
 */
data class PrivacyUiState(
    val privacyPolicyUrl: String? = null,
    val legalInfo: LegalInfo? = null,
    val legalUpToDate: Boolean = true,
    val privacyStatus: String? = null,
    val isAcceptingLegal: Boolean = false,
    val deletionPreview: DeletionPreview? = null,
    val isAuthenticated: Boolean = false,
    val isDeletionAvailable: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deletePassword: String = "",
    val deleteConfirmation: String = "",
    val deleteConfirmationError: String? = null,
    val error: String? = null,
    val info: String? = null,
)

sealed interface PrivacyEvent : UiEvent {
    data object LoadLegal : PrivacyEvent
    data object OpenPrivacyPolicyClicked : PrivacyEvent
    data object AcceptLegalClicked : PrivacyEvent
    data object DeleteAccountClicked : PrivacyEvent
    data class DeletePasswordChanged(val value: String) : PrivacyEvent
    data class DeleteConfirmationChanged(val value: String) : PrivacyEvent
    data object ConfirmDeleteClicked : PrivacyEvent
    data object CancelDeleteClicked : PrivacyEvent
    data object PrivacyPolicyLaunchFailed : PrivacyEvent
}

sealed interface PrivacyEffect : UiEffect {
    data class OpenPrivacyPolicy(val url: String) : PrivacyEffect
    data object AccountDeleted : PrivacyEffect
    data object RequireLogin : PrivacyEffect
    data object LegalAccepted : PrivacyEffect
}

class PrivacyViewModel(
    privacyPolicyUrl: String?,
    private val sessionState: StateFlow<SessionState>,
    private val deleteAccount: DeleteAccountUseCase,
    private val legalRepository: LegalRepository? = null,
    private val legalUpToDateProvider: () -> Boolean = { true },
) : BaseViewModel<PrivacyUiState, PrivacyEvent, PrivacyEffect>(
    PrivacyUiState(
        privacyPolicyUrl = PrivacyPolicyUrl.normalize(privacyPolicyUrl),
        isAuthenticated = sessionState.value is SessionState.Active,
        isDeletionAvailable = deleteAccount.isAvailable,
        legalUpToDate = legalUpToDateProvider(),
    ),
) {

    private var legalReloadAttempted = false

    init {
        launch { loadLegal() }
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
            PrivacyEvent.LoadLegal -> launch { loadLegal() }
            PrivacyEvent.OpenPrivacyPolicyClicked -> openPrivacyPolicy()
            PrivacyEvent.AcceptLegalClicked -> acceptLegal()
            PrivacyEvent.DeleteAccountClicked -> requestDelete()
            is PrivacyEvent.DeletePasswordChanged -> updateState {
                it.copy(deletePassword = event.value, deleteConfirmationError = null, error = null)
            }
            is PrivacyEvent.DeleteConfirmationChanged -> updateState {
                it.copy(deleteConfirmation = event.value, deleteConfirmationError = null, error = null)
            }
            PrivacyEvent.ConfirmDeleteClicked -> confirmDelete()
            PrivacyEvent.CancelDeleteClicked -> cancelDelete()
            PrivacyEvent.PrivacyPolicyLaunchFailed -> updateState {
                it.copy(info = MESSAGE_PRIVACY_LAUNCH_FAILED)
            }
        }
    }

    private suspend fun loadLegal() {
        val repo = legalRepository ?: run {
            updateState { it.copy(privacyStatus = MESSAGE_PRIVACY_BLOCKED) }
            return
        }
        repo.getLegal().fold(
            onSuccess = { info ->
                updateState {
                    it.copy(
                        legalInfo = info,
                        privacyPolicyUrl = PrivacyPolicyUrl.normalize(info.privacyUrl)
                            ?: it.privacyPolicyUrl,
                        privacyStatus = info.privacyStatus,
                        info = null,
                    )
                }
            },
            onFailure = {
                updateState { it.copy(privacyStatus = MESSAGE_PRIVACY_BLOCKED) }
            },
        )
    }

    private fun openPrivacyPolicy() {
        val url = PrivacyPolicyUrl.normalize(uiState.value.privacyPolicyUrl)
        if (url == null) {
            updateState {
                it.copy(info = uiState.value.privacyStatus ?: MESSAGE_PRIVACY_BLOCKED)
            }
            return
        }
        sendEffect(PrivacyEffect.OpenPrivacyPolicy(url))
    }

    private fun acceptLegal() {
        val current = uiState.value
        val info = current.legalInfo
        if (info == null || current.isAcceptingLegal) return
        launch {
            updateState { it.copy(isAcceptingLegal = true, error = null, info = null) }
            val repo = legalRepository
            if (repo == null) {
                updateState { it.copy(isAcceptingLegal = false, info = MESSAGE_LEGAL_UNAVAILABLE) }
                return@launch
            }
            repo.acceptLegal(info.termsVersion, info.privacyVersion).fold(
                onSuccess = {
                    legalReloadAttempted = false
                    updateState {
                        it.copy(
                            isAcceptingLegal = false,
                            legalUpToDate = true,
                            info = "Avisos legales aceptados.",
                        )
                    }
                    sendEffect(PrivacyEffect.LegalAccepted)
                },
                onFailure = { throwable ->
                    val outdated = throwable is AccountDeletionException &&
                        throwable.reason == AccountDeletionError.LegalVersionOutdated
                    if (outdated && !legalReloadAttempted) {
                        // Un solo recargo para evitar loop infinito.
                        legalReloadAttempted = true
                        loadLegal()
                        updateState {
                            it.copy(
                                isAcceptingLegal = false,
                                info = "Las versiones legales cambiaron. Revisa el aviso actualizado.",
                            )
                        }
                    } else {
                        updateState {
                            it.copy(
                                isAcceptingLegal = false,
                                error = "No se pudo aceptar los avisos legales. Intenta de nuevo.",
                            )
                        }
                    }
                },
            )
        }
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
        launch {
            deleteAccount.preview().fold(
                onSuccess = { preview ->
                    updateState {
                        it.copy(
                            deletionPreview = preview,
                            showDeleteConfirmation = true,
                            error = null,
                            info = null,
                        )
                    }
                },
                onFailure = {
                    updateState {
                        it.copy(
                            showDeleteConfirmation = true,
                            info = "No se pudo cargar el resumen de eliminación.",
                        )
                    }
                },
            )
        }
    }

    private fun cancelDelete() {
        if (uiState.value.isDeleting) return
        updateState {
            it.copy(
                showDeleteConfirmation = false,
                deletePassword = "",
                deleteConfirmation = "",
                deleteConfirmationError = null,
            )
        }
    }

    private fun confirmDelete() {
        val current = uiState.value
        if (current.isDeleting || !current.showDeleteConfirmation) return
        if (!current.isDeletionAvailable || !deleteAccount.isAvailable) {
            updateState {
                it.copy(
                    isDeletionAvailable = false,
                    showDeleteConfirmation = false,
                    info = MESSAGE_DELETE_BLOCKED,
                )
            }
            return
        }
        if (current.deleteConfirmation != CONFIRMATION) {
            updateState {
                it.copy(deleteConfirmationError = "Escribe ELIMINAR para confirmar.")
            }
            return
        }
        launch {
            updateState { it.copy(isDeleting = true, error = null, info = null) }
            when (val result = deleteAccount(current.deletePassword)) {
                AccountDeletionResult.Deleted -> {
                    updateState { it.copy(isDeleting = false, showDeleteConfirmation = false) }
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
                        it.copy(isDeleting = false, error = result.error.toSafeMessage())
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
        DomainError.Conflict -> "La contraseña no es correcta o la cuenta no puede eliminarse."
        DomainError.Gone -> "La cuenta ya no está disponible."
        DomainError.RateLimited -> "Demasiados intentos. Espera un momento e intenta de nuevo."
        DomainError.ServiceUnavailable, DomainError.Server ->
            "El servicio no está disponible ahora. Intenta más tarde."
        DomainError.ValidationError -> "Revisa los datos ingresados."
        DomainError.Unknown -> "No se pudo eliminar la cuenta. Intenta de nuevo."
    }

    companion object {
        const val CONFIRMATION = "ELIMINAR"
        const val MESSAGE_PRIVACY_BLOCKED =
            "El aviso de privacidad aún no está publicado. No se abrirá una URL no válida."
        const val MESSAGE_LEGAL_UNAVAILABLE = "No se pudo cargar la información legal."
        const val MESSAGE_DELETE_BLOCKED =
            "La eliminación de cuenta todavía no está disponible. No se ha eliminado nada."
        const val MESSAGE_PRIVACY_LAUNCH_FAILED =
            "No se pudo abrir el navegador. Verifica que tu dispositivo tenga uno disponible."
    }
}
