package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.remote.api.AuthApi
import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.CurrentUserDto
import com.thehouse.app.data.remote.dto.auth.LoginRequestDto
import com.thehouse.app.data.remote.dto.auth.RefreshRequestDto
import com.thehouse.app.data.remote.dto.auth.RegisterRequestDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.Response

/** Regresiones del hotfix D21: ninguna excepción de red esperable debe escapar. */
class D21NetworkFailureRegressionTest {

    private class ThrowingAuthApi(private val error: Exception) : AuthApi {
        override suspend fun register(body: RegisterRequestDto): Response<AuthResponseDto> = throw error
        override suspend fun login(body: LoginRequestDto): Response<AuthResponseDto> = throw error
        override suspend fun refresh(body: RefreshRequestDto): Response<AuthResponseDto> = throw error
        override suspend fun getCurrentUser(authHeader: String): Response<CurrentUserDto> = throw error
        override suspend fun logout(authHeader: String?): Response<Unit> = throw error
    }

    private fun repository(error: Exception): Pair<AuthRepositoryImpl, InMemorySessionStore> {
        val store = InMemorySessionStore()
        return AuthRepositoryImpl(
            api = ThrowingAuthApi(error),
            sessionStore = store,
            refreshTokenStorage = InMemorySessionTokenStorage(),
        ) to store
    }

    @Test
    fun `login socket timeout returns typed failure instead of throwing`() = runTest {
        val (repo) = repository(SocketTimeoutException("timeout"))
        val result = repo.login("user@test.com", "secret")
        assertTrue(result.isFailure)
        assertEquals(DomainError.Timeout, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `login unknown host returns typed network failure`() = runTest {
        val (repo) = repository(UnknownHostException("no DNS"))
        val result = repo.login("user@test.com", "secret")
        assertEquals(DomainError.Network, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `register timeout is typed as well`() = runTest {
        val (repo) = repository(SocketTimeoutException("timeout"))
        val result = repo.register("Name", "user@test.com", "secret", true, true)
        assertEquals(DomainError.Timeout, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `real active session survives a transient network failure`() = runTest {
        val store = InMemorySessionStore()
        store.activate(
            com.thehouse.app.domain.model.UserSession(
                userId = "u1", displayName = "User", email = "user@test.com",
                createdAt = "2026-01-01T00:00:00Z",
            ),
            "access-token",
        )
        val repo = AuthRepositoryImpl(
            api = ThrowingAuthApi(UnknownHostException("no DNS")),
            sessionStore = store,
            refreshTokenStorage = InMemorySessionTokenStorage(),
        )
        val result = repo.login("user@test.com", "secret")
        assertTrue(result.isFailure)
        assertTrue(store.state.value is SessionState.Active)
        assertEquals("access-token", store.accessToken)
    }

    @Test
    fun `cancellation keeps propagating instead of becoming a network failure`() = runTest {
        val cancellation = object : CancellationException("cancelled by test") {}
        val (repo) = repository(cancellation)
        var propagated = false
        try {
            repo.login("user@test.com", "secret")
        } catch (cancelled: CancellationException) {
            propagated = true
        }
        assertTrue(propagated)
    }
}
