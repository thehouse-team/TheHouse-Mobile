package com.thehouse.app.presentation.home

import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** D21 hotfix: "Publicar mi renta" debe resolver navegación válida. */
class D21PublishNavigationTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(kotlinx.coroutines.test.UnconfinedTestDispatcher())

    private class FakeAuth : AuthRepository {
        val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
        override val session: StateFlow<SessionState> = state
        fun activate() {
            state.value = SessionState.Active(
                UserSession(userId = "u1", displayName = "Carlos", email = "c@correo.mx"),
            )
        }
        override suspend fun register(
            name: String, email: String, password: String,
            acceptedTerms: Boolean, acceptedPrivacy: Boolean,
        ): Result<UserSession?> = Result.success(null)
        override suspend fun login(email: String, password: String): Result<UserSession?> =
            Result.success(null)
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
        override suspend fun refreshAccessToken(): Result<String?> = Result.success(null)
        override suspend fun getCurrentUser(): Result<UserSession?> = Result.success(null)
        override suspend fun restoreSession(): Result<UserSession?> = Result.success(null)
    }

    @Test
    fun `guest publicar emite RequireLogin y no OpenLandlord`() = runTest {
        val auth = FakeAuth()
        val vm = HomeViewModel(LogoutUseCase(auth), sessionState = auth.state)
        vm.onEvent(HomeEvent.PublishClicked)
        advanceUntilIdle()
        val effect = vm.effects.first()
        assertTrue(effect is HomeEffect.RequireLogin)
    }

    @Test
    fun `usuario autenticado publicar emite OpenLandlord`() = runTest {
        val auth = FakeAuth().apply { activate() }
        val vm = HomeViewModel(LogoutUseCase(auth), sessionState = auth.state)
        vm.onEvent(HomeEvent.PublishClicked)
        advanceUntilIdle()
        val effect = vm.effects.first()
        assertTrue(effect is HomeEffect.OpenLandlord)
    }

    @Test
    fun `HomeScreen observa OpenLandlord y llama callback de arrendador`() {
        val source = java.io.File("src/main/java/com/thehouse/app/presentation/home/HomeScreen.kt").readText()
        assertTrue(source.contains("HomeEffect.OpenLandlord -> onOpenLandlord()"))
        assertTrue(source.contains("onClick = { viewModel.onEvent(HomeEvent.PublishClicked) }"))
    }

    @Test
    fun `boton publicar permanece antes de recomendados y privacidad no aparece al final`() {
        val source = java.io.File("src/main/java/com/thehouse/app/presentation/home/HomeScreen.kt").readText()
        val publish = source.indexOf("Publicar mi renta")
        val recommended = source.indexOf("Recomendados para ti")
        assertTrue(publish >= 0 && recommended >= 0 && publish < recommended)
        assertTrue(!source.contains("TheHouseCard(onClick = onOpenPrivacy)"))
    }
}
