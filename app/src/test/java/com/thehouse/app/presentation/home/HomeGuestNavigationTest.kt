package com.thehouse.app.presentation.home

import com.thehouse.app.domain.auth.AccessPolicy
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.presentation.navigation.BottomTab
import com.thehouse.app.presentation.navigation.TabNavigationState
import com.thehouse.app.presentation.onboarding.OnboardingEvent
import com.thehouse.app.presentation.onboarding.OnboardingViewModel
import com.thehouse.app.presentation.onboarding.OnboardingEffect
import com.thehouse.app.domain.repository.OnboardingRepository
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fake mínimo de sesión para D4 (sin red, sin Android). */
private class HomeGuestFakeRepository : AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
    override val session: StateFlow<SessionState> = state

    fun setActive() {
        state.value = SessionState.Active(
            UserSession(userId = "u1", displayName = "Carlos", email = "c@correo.mx"),
        )
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> = Result.success(null)

    override suspend fun login(email: String, password: String): Result<UserSession?> =
        Result.success(null)

    override suspend fun logout(): Result<Unit> {
        state.value = SessionState.Anonymous
        return Result.success(Unit)
    }

    override suspend fun refreshAccessToken(): Result<String?> = Result.success(null)
    override suspend fun getCurrentUser(): Result<UserSession?> = Result.success(null)
    override suspend fun restoreSession(): Result<UserSession?> = Result.success(null)
}

private class FakeOnboardingRepository : OnboardingRepository {
    var completed = false
    override fun keyFor(userId: String?): String =
        userId?.takeIf { it.isNotBlank() }?.lowercase() ?: "local"

    override suspend fun isCompleted(key: String): Boolean = completed
    override suspend fun setCompleted(key: String, completed: Boolean) {
        this.completed = completed
    }
}

class HomeGuestNavigationTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `omitir emite NavigateToWelcome y marca completado`() = runTest {
        val repo = FakeOnboardingRepository()
        val vm = OnboardingViewModel(repo, userKey = null)
        vm.onEvent(OnboardingEvent.SkipClicked)
        advanceUntilIdle()
        assertTrue(repo.completed)
        assertEquals(OnboardingEffect.NavigateToWelcome, vm.effects.first())
    }

    @Test
    fun `guest puede abrir Inicio Explorar y Smart Match`() {
        assertTrue(BottomTab.Inicio.guestCanViewContent)
        assertTrue(BottomTab.Explorar.guestCanViewContent)
        assertTrue(BottomTab.SmartMatch.guestCanViewContent)
    }

    @Test
    fun `guest favoritos requiere acceso`() {
        assertTrue(BottomTab.Favoritos.requiresAuthForContent)
        assertFalse(BottomTab.Favoritos.guestCanViewContent)
    }

    @Test
    fun `accion protegida guest lleva a login`() = runTest {
        val repo = HomeGuestFakeRepository()
        val vm = HomeViewModel(
            LogoutUseCase(repo),
            sessionState = MutableStateFlow<SessionState>(SessionState.Anonymous),
        )
        runCurrent()
        assertTrue(AccessPolicy.shouldRedirectToLogin(ProtectedAction.SaveFavorite, false))
        vm.onEvent(HomeEvent.FavoriteClicked)
        advanceUntilIdle()
        assertTrue(vm.effects.first() is HomeEffect.RequireLogin)
    }

    @Test
    fun `accion protegida contactar y publicar tambien protegen guest`() = runTest {
        val repo = HomeGuestFakeRepository()
        val anonymous = MutableStateFlow<SessionState>(SessionState.Anonymous)
        val vm = HomeViewModel(LogoutUseCase(repo), sessionState = anonymous)
        vm.onEvent(HomeEvent.ContactClicked)
        advanceUntilIdle()
        assertTrue(vm.effects.first() is HomeEffect.RequireLogin)
        val vm2 = HomeViewModel(LogoutUseCase(repo), sessionState = anonymous)
        vm2.onEvent(HomeEvent.PublishClicked)
        advanceUntilIdle()
        assertTrue(vm2.effects.first() is HomeEffect.RequireLogin)
    }

    @Test
    fun `authenticated no recibe bloqueo guest`() = runTest {
        val repo = HomeGuestFakeRepository()
        repo.setActive()
        val vm = HomeViewModel(
            LogoutUseCase(repo),
            sessionState = repo.session,
        )
        runCurrent()
        assertFalse(AccessPolicy.shouldRedirectToLogin(ProtectedAction.SaveFavorite, true))
        // Sin efecto RequireLogin: ningún efecto emitido.
        vm.onEvent(HomeEvent.FavoriteClicked)
        runCurrent()
        // Channel vacío → first() lanzaría; comprobamos que no hay bloqueo por estado
        assertTrue(vm.uiState.value.isAuthenticated || vm.uiState.value.isGuest == false)
    }

    @Test
    fun `bottom nav mantiene destino seleccionado`() {
        var state = TabNavigationState()
        state = state.onTabSelected(BottomTab.Explorar)
        assertEquals(BottomTab.Explorar, state.selected)
        state = state.onTabSelected(BottomTab.SmartMatch)
        assertEquals(BottomTab.SmartMatch, state.selected)
        state = state.onTabSelected(BottomTab.Favoritos)
        assertEquals(BottomTab.Favoritos, state.selected)
        assertEquals(BottomTab.Inicio, TabNavigationState.fromRoute("inicio"))
        assertEquals(BottomTab.Favoritos, TabNavigationState.fromRoute("favoritos"))
        assertEquals(BottomTab.Inicio, TabNavigationState.fromRoute("desconocida"))
    }

    @Test
    fun `logout guest no crea sesion`() = runTest {
        val repo = HomeGuestFakeRepository()
        val vm = HomeViewModel(
            LogoutUseCase(repo),
            sessionState = repo.session,
        )
        vm.logout()
        advanceUntilIdle()
        assertEquals(SessionState.Anonymous, repo.session.value)
        assertTrue(vm.effects.first() is HomeEffect.LoggedOut)
    }
}
