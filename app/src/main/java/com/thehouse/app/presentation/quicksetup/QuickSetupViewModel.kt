package com.thehouse.app.presentation.quicksetup

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

/**
 * Quick Setup breve con los campos realmente soportados (nombre y universidad).
 *
 * El flag de completitud es de solo lectura desde el perfil; la UI no lo inventa.
 */
data class QuickSetupUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val profile: UserProfile? = null,
    val isCompleted: Boolean = false,
    val nameDraft: String = "",
    val selectedUniversityId: String? = null,
    val universities: List<University> = emptyList(),
    val universitiesLoading: Boolean = false,
    val universitiesError: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val actionError: String? = null,
)

sealed interface QuickSetupEvent : UiEvent {
    data object Load : QuickSetupEvent
    data object Retry : QuickSetupEvent
    data class NameChanged(val value: String) : QuickSetupEvent
    data class UniversitySelected(val universityId: String) : QuickSetupEvent
    data object RetryUniversities : QuickSetupEvent
    data object Save : QuickSetupEvent
    data object ClearMessages : QuickSetupEvent
}

sealed interface QuickSetupEffect : UiEffect

class QuickSetupViewModel(
    private val profileRepository: ProfileRepository,
    private val getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val selectUserUniversity: SelectUserUniversityUseCase,
    private val getUniversities: GetUniversitiesUseCase,
) : BaseViewModel<QuickSetupUiState, QuickSetupEvent, QuickSetupEffect>(QuickSetupUiState()) {

    override fun onEvent(event: QuickSetupEvent) {
        when (event) {
            QuickSetupEvent.Load,
            QuickSetupEvent.Retry -> load()

            is QuickSetupEvent.NameChanged -> updateState {
                it.copy(nameDraft = event.value, actionError = null, message = null)
            }

            is QuickSetupEvent.UniversitySelected -> updateState {
                it.copy(selectedUniversityId = event.universityId, actionError = null, message = null)
            }

            QuickSetupEvent.RetryUniversities -> loadUniversities()

            QuickSetupEvent.Save -> save()

            QuickSetupEvent.ClearMessages -> updateState {
                it.copy(message = null, actionError = null)
            }
        }
    }

    private fun load() {
        updateState { it.copy(isLoading = true, loadError = null, actionError = null, message = null) }
        launch {
            try {
                profileRepository.loadProfile()
                val profile = getProfile()
                updateState {
                    it.copy(
                        isLoading = false,
                        loadError = null,
                        profile = profile,
                        isCompleted = profile?.quickSetupCompleted == true,
                        nameDraft = profile?.name.orEmpty(),
                        selectedUniversityId = profile?.university?.id ?: it.selectedUniversityId,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState { it.copy(isLoading = false, loadError = throwable.toSafeUiMessage()) }
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

    private fun save() {
        val current = uiState.value
        if (current.isSaving || current.isLoading) return
        updateState { it.copy(isSaving = true, actionError = null, message = null) }
        launch {
            try {
                val trimmedName = current.nameDraft.trim()
                val currentName = current.profile?.name.orEmpty()
                if (trimmedName != currentName) {
                    updateProfile(trimmedName.ifBlank { null })
                }
                val currentUniversityId = current.profile?.university?.id
                val selectedId = current.selectedUniversityId
                if (!selectedId.isNullOrBlank() && selectedId != currentUniversityId) {
                    selectUserUniversity(selectedId)
                }
                // Asegura estado fresco (los use cases ya recargan, pero es seguro).
                profileRepository.loadProfile()
                val profile = getProfile() ?: current.profile
                val completed = profile?.quickSetupCompleted == true
                updateState {
                    it.copy(
                        isSaving = false,
                        profile = profile,
                        isCompleted = completed,
                        nameDraft = profile?.name.orEmpty(),
                        selectedUniversityId = profile?.university?.id ?: it.selectedUniversityId,
                        message = if (completed) {
                            "Configuración inicial completa."
                        } else {
                            "Cambios guardados. Puedes completar tus preferencias cuando quieras."
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                updateState {
                    it.copy(
                        isSaving = false,
                        actionError = throwable.toSafeUiMessage(),
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_UNIVERSITIES = 50
    }
}
