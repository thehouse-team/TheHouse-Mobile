package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.CurrentUserDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RefreshRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Ninguna llamada HTTP es válida durante la limpieza local de NV-15. */
private class UnusedAuthApi : AuthApi {
    override suspend fun register(body: RegisterRequestDto): Response<AuthResponseDto> =
        error("register must not be called")

    override suspend fun login(body: LoginRequestDto): Response<AuthResponseDto> =
        error("login must not be called")

    override suspend fun refresh(body: RefreshRequestDto): Response<AuthResponseDto> =
        error("refresh must not be called")

    override suspend fun getCurrentUser(authHeader: String): Response<CurrentUserDto> =
        error("users/me must not be called")

    override suspend fun logout(authHeader: String?): Response<Unit> =
        error("logout must not be called")
}

private val sessionUser = UserSession(
    userId = "u-15",
    displayName = "Estudiante",
    email = "estudiante@correo.mx",
)

/**
 * NV-15: tras eliminar la cuenta, un reinicio no debe restaurar la sesión.
 * La limpieza local no depende de red y borra access token, refresh token y estado.
 */
class LocalSessionClearTest {

    @Test
    fun `clear local session removes tokens and prevents restore after restart`() = runTest {
        val store = InMemorySessionStore()
        val storage = InMemorySessionTokenStorage()
        val repository = AuthRepositoryImpl(UnusedAuthApi(), store, storage)
        store.activate(sessionUser, accessToken = "access-15")
        storage.writeRefreshToken("refresh-15".toByteArray(Charsets.UTF_8))

        repository.clearLocalSession()

        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(store.accessToken)
        assertNull(storage.lastRaw)

        // Simula el arranque: sin refresh token no hay request y la sesión sigue anónima.
        val restored = repository.restoreSession()

        assertTrue(restored.isSuccess)
        assertNull(restored.getOrNull())
        assertEquals(SessionState.Anonymous, repository.session.value)
    }

    @Test
    fun `clear local session is safe when already anonymous`() = runTest {
        val store = InMemorySessionStore()
        val storage = InMemorySessionTokenStorage()
        val repository = AuthRepositoryImpl(UnusedAuthApi(), store, storage)

        repository.clearLocalSession()

        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(store.accessToken)
        assertNull(storage.lastRaw)
    }
}
