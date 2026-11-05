package com.thehouse.app.presentation.auth

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.usecase.LoginUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.RegisterUseCase
import com.thehouse.app.presentation.home.HomeEvent
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel fake de AuthRepository para pruebas unitarias (sin red).
 */
class FakeAuthRepository(
    private val loginBehavior: suspend (String, String) -> Result<UserSession?>,
    private val registerBehavior: suspend (
        String, String, String, Boolean, Boolean,
    ) -> Result<UserSession?>,
    private val logoutBehavior: suspend () -> Result<Unit> = { Result.success(Unit) },
) : AuthRepository {

    private val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
    override val session: StateFlow<SessionState> = state

    var loginCalls: Int = 0
    var logoutCalls: Int = 0
    var lastConfirmed: UserSession? = null

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> {
        assertEquals(true, acceptedTerms)
        assertEquals(true, acceptedPrivacy)
        val result = registerBehavior(name, email, password, acceptedTerms, acceptedPrivacy)
        result.getOrNull()?.let { user ->
            state.value = SessionState.Active(user)
        }
        return result
    }

    override suspend fun login(email: String, password: String): Result<UserSession?> {
        loginCalls++
        val result = loginBehavior(email, password)
        result.getOrNull()?.let { user ->
            state.value = SessionState.Active(user)
        }
        return result
    }

    override suspend fun logout(): Result<Unit> {
        logoutCalls++
        state.value = SessionState.Anonymous
        return logoutBehavior()
    }
}

/** Pruebas unitarias de LoginViewModel (dispatchers controlados, sin red). */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `login exitoso emite efecto de navegación`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { email, password ->
                assertEquals("usuario@correo.mx", email)
                assertEquals("password123", password)
                Result.success(UserSession(userId = "u1", displayName = "Victor", email = email))
            },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
        )
        val viewModel = LoginViewModel(LoginUseCase(repo))

        viewModel.onEvent(LoginEvent.EmailChanged("usuario@correo.mx"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        viewModel.onEvent(LoginEvent.LoginClicked)
        runCurrent()

        assertEquals(LoginEffect.NavigateAfterLogin, viewModel.effects.first())
        assertTrue(repo.loginCalls == 1)
    }

    @Test
    fun `login con validación inválida muestra error en español sin llamar red`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.success(null) },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
        )
        val viewModel = LoginViewModel(LoginUseCase(repo))
        viewModel.onEvent(LoginEvent.EmailChanged("no-es-correo"))
        viewModel.onEvent(LoginEvent.PasswordChanged("123"))
        viewModel.onEvent(LoginEvent.LoginClicked)
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("correo", ignoreCase = true))
        assertEquals(0, repo.loginCalls)
    }

    @Test
    fun `login con 401 muestra credenciales inválidas`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.failure(DomainException(DomainError.Unauthorized)) },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
        )
        val viewModel = LoginViewModel(LoginUseCase(repo))
        viewModel.onEvent(LoginEvent.EmailChanged("usuario@correo.mx"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        viewModel.onEvent(LoginEvent.LoginClicked)
        runCurrent()

        assertEquals("Credenciales inválidas. Revisa tu correo y contraseña.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

/** Pruebas unitarias de RegisterViewModel (consentimientos obligatorios). */
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `registro sin aceptar aviso de privacidad muestra error y no consume red`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.success(null) },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
        )
        val viewModel = RegisterViewModel(RegisterUseCase(repo))
        viewModel.onEvent(RegisterEvent.NameChanged("Victor"))
        viewModel.onEvent(RegisterEvent.EmailChanged("v@correo.mx"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("password123"))
        viewModel.onEvent(RegisterEvent.TermsToggled(true))
        viewModel.onEvent(RegisterEvent.RegisterClicked)
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("aviso", ignoreCase = true))
    }

    @Test
    fun `registro sin sesión en respuesta NO asume login automático`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.success(null) },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
        )
        val viewModel = RegisterViewModel(RegisterUseCase(repo))
        viewModel.onEvent(RegisterEvent.NameChanged("Victor"))
        viewModel.onEvent(RegisterEvent.EmailChanged("v@correo.mx"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("password123"))
        viewModel.onEvent(RegisterEvent.TermsToggled(true))
        viewModel.onEvent(RegisterEvent.PrivacyToggled(true))
        viewModel.onEvent(RegisterEvent.RegisterClicked)
        runCurrent()

        assertEquals(RegisterEffect.NavigateToLogin, viewModel.effects.first())
    }

    @Test
    fun `registro con sesión emite navegación a onboarding`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.success(null) },
            registerBehavior = { _, _, _, _, _ ->
                Result.success(UserSession(userId = "u2", displayName = "Victor", email = "v@correo.mx"))
            },
        )
        val viewModel = RegisterViewModel(RegisterUseCase(repo))
        viewModel.onEvent(RegisterEvent.NameChanged("Victor"))
        viewModel.onEvent(RegisterEvent.EmailChanged("v@correo.mx"))
        viewModel.onEvent(RegisterEvent.PasswordChanged("password123"))
        viewModel.onEvent(RegisterEvent.TermsToggled(true))
        viewModel.onEvent(RegisterEvent.PrivacyToggled(true))
        viewModel.onEvent(RegisterEvent.RegisterClicked)
        runCurrent()

        assertEquals(RegisterEffect.NavigateToOnboarding, viewModel.effects.first())
    }
}

/** Logout limpia SIEMPRE el estado local aunque el backend falle. */
class LogoutStateTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `logout fallido en backend limpia sesión local`() = runTest {
        val repo = FakeAuthRepository(
            loginBehavior = { _, _ -> Result.success(null) },
            registerBehavior = { _, _, _, _, _ -> Result.success(null) },
            logoutBehavior = { Result.failure(DomainException(DomainError.Server)) },
        )
        val viewModel = HomeViewModel(LogoutUseCase(repo))
        viewModel.onEvent(HomeEvent.LogoutClicked)
        runCurrent()

        assertEquals(SessionState.Anonymous, repo.session.value)
        assertEquals(1, repo.logoutCalls)
    }
}
