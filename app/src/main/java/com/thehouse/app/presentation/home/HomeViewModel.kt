package com.thehouse.app.presentation.home

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.toCatalogState
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM + UDF del Home (D4 Guest + D5 catálogo real, NV-05/NV-06/NV-07).
 *
 * - Guest (Anonymous) abre Home sin sesión falsa ni tokens fake.
 * - Acciones protegidas (favorito, contactar, publicar) como Guest
 *   emiten [HomeEffect.RequireLogin] (UX clara → Login).
 * - Autenticado (Active) nunca recibe bloqueo Guest.
 * - Logout limpia siempre el estado local (D3 intacto).
 * - D5: universidades y propiedades REALES del backend (sin UTTT hardcodeada
 *   productiva, sin Match/verificación/distancia inventados).
 * - Destacada = primera propiedad disponible (regla UI local, no del backend).
 * - Fixtures de prototipo solo Preview/tests; producción usa API real.
 */
data class HomeUiState(
    val displayName: String = "",
    val email: String = "",
    val isLoggingOut: Boolean = false,
    val isGuest: Boolean = false,
    val isAuthenticated: Boolean = false,
    val universities: CatalogLoadState<University> = CatalogLoadState.Loading,
    val properties: CatalogLoadState<Property> = CatalogLoadState.Loading,
    val selectedUniversityId: String? = null,
    val featuredProperty: Property? = null,
    val propertiesVersion: Int = 0,
    val universitiesVersion: Int = 0,
)

sealed interface HomeEvent : UiEvent {
    data object LogoutClicked : HomeEvent
    /** Guardar favorito (acción protegida como Guest). */
    data object FavoriteClicked : HomeEvent
    /** Contactar propietario (acción protegida como Guest). */
    data object ContactClicked : HomeEvent
    /** Publicar alojamiento (acción protegida como Guest). */
    data object PublishClicked : HomeEvent
    data object RetryUniversitiesClicked : HomeEvent
    data object RetryPropertiesClicked : HomeEvent
    data class UniversitySelected(val universityId: String) : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data object LoggedOut : HomeEffect
    data object RequireLogin : HomeEffect
}

class HomeViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Anonymous),
    private val getUniversities: GetUniversitiesUseCase? = null,
    private val getProperties: GetPropertiesUseCase? = null,
    private val selectedRepository: SelectedUniversityRepository? = null,
    private val selectUniversity: SelectUniversityUseCase? = null,
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>(HomeUiState()) {

    init {
        updateState {
            val isGuest = sessionState.value is SessionState.Anonymous
            it.copy(
                isGuest = isGuest,
                isAuthenticated = !isGuest,
            )
        }
        if (selectedRepository != null) {
            launch {
                selectedRepository.loadSelected()
                updateState { it.copy(selectedUniversityId = selectedRepository.selectedUniversityId.value) }
            }
        }
        loadUniversities()
        loadProperties()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LogoutClicked -> logout()
            HomeEvent.FavoriteClicked -> guard(ProtectedAction.SaveFavorite)
            HomeEvent.ContactClicked -> guard(ProtectedAction.Contact)
            HomeEvent.PublishClicked -> guard(ProtectedAction.Publish)
            HomeEvent.RetryUniversitiesClicked -> loadUniversities()
            HomeEvent.RetryPropertiesClicked -> loadProperties()
            is HomeEvent.UniversitySelected -> select(event.universityId)
        }
    }

    fun loadUniversities() {
        val useCase = getUniversities
        if (useCase == null) {
            updateState { it.copy(universities = CatalogLoadState.Empty) }
            return
        }
        updateState { it.copy(universities = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            updateState {
                it.copy(
                    universities = result.toCatalogState(),
                    universitiesVersion = it.universitiesVersion + 1,
                )
            }
        }
    }

    fun loadProperties() {
        val useCase = getProperties
        if (useCase == null) {
            updateState { it.copy(properties = CatalogLoadState.Empty) }
            return
        }
        updateState { it.copy(properties = CatalogLoadState.Loading) }
        launch {
            val result = useCase().map { page -> page.items }
            val state = result.toCatalogState()
            val featured = (state as? CatalogLoadState.Success)?.items?.firstOrNull()
            updateState {
                it.copy(
                    properties = state,
                    featuredProperty = featured,
                    propertiesVersion = it.propertiesVersion + 1,
                )
            }
        }
    }

    private fun select(universityId: String) {
        launch {
            selectUniversity?.invoke(universityId)
            selectedRepository?.selectedUniversityId?.value?.let { id ->
                updateState { it.copy(selectedUniversityId = id) }
            } ?: updateState { it.copy(selectedUniversityId = universityId) }
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
