package com.thehouse.app.presentation.profile

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.repository.ProfileRepository
import com.thehouse.app.domain.usecase.GetProfileUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SelectUserUniversityUseCase
import com.thehouse.app.domain.usecase.UpdateProfileUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import com.thehouse.app.presentation.common.toSafeUiMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de la pantalla de perfil.
 *
 * Solo expone datos reales del contrato D11: nombre, universidad y el flag de
 * Quick Setup. El correo se lee de la sesión activa (dato real de identidad),
 * nunca de tokens ni identificadores técnicos.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: UserProfile? = null,
    val email: String? = null,
    val nameDraft: String = "",
    val isEditingName: Boolean = false,
    val isSavingName: Boolean = false,
    val isSavingUniversity: Boolean = false,
    val universities: List<University> = emptyList(),
    val universitiesLoading: Boolean = false,
    val universitiesError: String? = null,
    val showUniversityPicker: Boolean = false,
    val message: String? = null,
    val actionError: String? = null,
) {
    val quickSetupCompleted: Boolean
        get() = profile?.quickSetupCompleted == true
}

sealed interface ProfileEvent : UiEvent {
    data object Load : ProfileEvent
    data object Retry : ProfileEvent
    data object EditName : ProfileEvent
    data object CancelEditName : ProfileEvent
    data class NameChanged(val value: String) : ProfileEvent
    data object SaveName : ProfileEvent
    data object OpenUniversityPicker : ProfileEvent
    data object DismissUniversityPicker : ProfileEvent
    data class SelectUniversity(val universityId: String) : ProfileEvent
    data object RetryUniversities : ProfileEvent
    data object ClearMessages : ProfileEvent
}

/** Efectos one-shot del perfil (D19: 401 definitivo → login). */
sealed interface ProfileEffect : UiEffect {
    data object RequireLogin : ProfileEffect
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val selectUserUniversity: SelectUserUniversityUseCase,
    private val getUniversities: GetUniversitiesUseCase,
    private val sessionState: StateFlow<SessionState>,
) : BaseViewModel<ProfileUiState, ProfileEvent, ProfileEffect>(ProfileUiState()) {

    init {
        launch {
            sessionState.collect { state ->
                val email = (state as? SessionState.Active)?.session?.email
                updateState { it.copy(email = email) }
            }
        }
    }

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Load,
            ProfileEvent.Retry -> load()

            ProfileEvent.EditName -> updateState {
                it.copy(
                    isEditingName = true,
                    nameDraft = it.profile?.name.orEmpty(),
                    message = null,
                    actionError = null,
                )
            }

            ProfileEvent.CancelEditName -> updateState {
                it.copy(
                    isEditingName = false,
                    nameDraft = it.profile?.name.orEmpty(),
                    actionError = null,
                )
            }

            is ProfileEvent.NameChanged -> updateState {
                it.copy(nameDraft = event.value, actionError = null, message = null)
            }

            ProfileEvent.SaveName -> saveName()

            ProfileEvent.OpenUniversityPicker -> {
                updateState {
                    it.copy(
                        showUniversityPicker = true,
                        message = null,
                        actionError = null,
                    )
                }
                if (uiState.value.universities.isEmpty() && !uiState.value.universitiesLoading) {
                    loadUniversities()
                }
            }

            ProfileEvent.DismissUniversityPicker -> updateState {
                it.copy(showUniversityPicker = false)
            }

            is ProfileEvent.SelectUniversity -> selectUniversity(event.universityId)

            ProfileEvent.RetryUniversities -> loadUniversities()

            ProfileEvent.ClearMessages -> updateState {
                it.copy(message = null, actionError = null)
            }
        }
    }

    private fun load() {
        updateState {
            it.copy(
                isLoading = true,
                error = null,
                message = null,
                actionError = null,
            )
        }
        launch {
            try {
                profileRepository.loadProfile()
                val profile = getProfile()
                updateState {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        nameDraft = profile?.name.orEmpty(),
                        isEditingName = false,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                val unauthorized = (throwable as? DomainException)?.error == DomainError.Unauthorized
                updateState {
                    it.copy(
                        isLoading = false,
                        // D19: un 401 definitivo NO es un error genérico; la UI
                        // navega a Login mediante el efecto RequireLogin.
                        error = if (unauthorized) null else throwable.toSafeUiMessage(),
                    )
                }
                if (unauthorized) sendEffect(ProfileEffect.RequireLogin)
            }
        }
        loadUniversities()
    }

    private fun loadUniversities() {
        if (uiState.value.universitiesLoading) return
        updateState { it.copy(universitiesLoading = true, universitiesError = null) }
        launch {
            getUniversities(page = 1, pageSize = MAX_UNIVERSITIES).fold(
                onSuccess = { paged ->
                    updateState {
                        it.copy(
                            universities = paged.items,
                            universitiesLoading = false,
                            universitiesError = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            universitiesLoading = false,
                            universitiesError = throwable.toSafeUiMessage(),
                        )
                    }
                },
            )
        }
    }

    private fun saveName() {
        val current = uiState.value
        if (current.isSavingName) return
        val trimmed = current.nameDraft.trim()
        if (trimmed == current.profile?.name.orEmpty()) {
            updateState {
                it.copy(
                    isEditingName = false,
                    actionError = null,
                    message = null,
                )
            }
            return
        }
        updateState {
            it.copy(
                isSavingName = true,
                message = null,
                actionError = null,
            )
        }
        launch {
            try {
                updateProfile(trimmed.ifBlank { null })
                val profile = getProfile() ?: current.profile
                updateState {
                    it.copy(
                        isSavingName = false,
                        isEditingName = false,
                        profile = profile,
                        nameDraft = profile?.name.orEmpty(),
                        message = "Perfil actualizado.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState {
                    it.copy(
                        isSavingName = false,
                        actionError = throwable.toSafeUiMessage(),
                    )
                }
            }
        }
    }

    private fun selectUniversity(universityId: String) {
        val current = uiState.value
        if (current.isSavingUniversity) return
        updateState {
            it.copy(
                isSavingUniversity = true,
                message = null,
                actionError = null,
            )
        }
        launch {
            try {
                selectUserUniversity(universityId)
                val profile = getProfile() ?: current.profile
                updateState {
                    it.copy(
                        isSavingUniversity = false,
                        showUniversityPicker = false,
                        profile = profile,
                        message = "Universidad actualizada.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState {
                    it.copy(
                        isSavingUniversity = false,
                        actionError = throwable.toSafeUiMessage(),
                    )
                }
            }
        }
    }

    private companion object {
        /** pageSize máximo confirmado por el contrato de universidades. */
        const val MAX_UNIVERSITIES = 50
    }
}
