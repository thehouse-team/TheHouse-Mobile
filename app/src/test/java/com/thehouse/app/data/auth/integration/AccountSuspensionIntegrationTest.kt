package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.remote.auth.AccountSuspendedIOException
import com.thehouse.app.data.remote.auth.BearerTokenInterceptor
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * D13 (NV-21 §3): 403 `account_suspended` en login/refresh/restauración y en
 * llamadas protegidas. La suspensión es un estado de sesión; NO hace logout
 * automático ni borra el refresh token.
 */
class AccountSuspensionIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var storage: InMemorySessionTokenStorage

    private val loginOk = """
        {"accessToken":"access-1","refreshToken":"refresh-1",
         "tokenType":"Bearer","expiresIn":900}
    """.trimIndent()

    private val meOk = """
        {"id":"u-1","email":"v@correo.mx","createdAt":"2026-11-05T00:00:00Z"}
    """.trimIndent()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        sessionStore = InMemorySessionStore()
        storage = InMemorySessionTokenStorage()
        repository = AuthRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.AuthApi::class.java),
            sessionStore = sessionStore,
            refreshTokenStorage = storage,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun response(code: Int, raw: String): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    /** Login real D3: 200 tokens + /users/me → sesión activa con identidad. */
    private suspend fun loginActive() {
        server.enqueue(response(200, loginOk))
        server.enqueue(response(200, meOk))
        val result = repository.login("v@correo.mx", "PasswordSegura123!")
        assertTrue(result.isSuccess)
        assertTrue(sessionStore.state.value is SessionState.Active)
    }

    private fun domainErrorOf(result: Result<*>): DomainError =
        (result.exceptionOrNull() as DomainException).error

    @Test
    fun `login 403 account_suspended keeps identity and refresh token`() = runTest {
        loginActive()
        assertEquals("refresh-1", storage.lastRaw)

        server.enqueue(
            response(403, """{"status":403,"code":"account_suspended"}"""),
        )
        val result = repository.login("v@correo.mx", "PasswordSegura123!")

        assertTrue(result.isFailure)
        assertEquals(DomainError.AccountSuspended, domainErrorOf(result))
        val state = sessionStore.state.value
        assertTrue(state is SessionState.Suspended)
        // Sin logout destructivo: la identidad y el refresh token sobreviven.
        assertEquals("u-1", (state as SessionState.Suspended).session?.userId)
        assertEquals("refresh-1", storage.lastRaw)
    }

    @Test
    fun `restoreSession blocked while suspended without clearing refresh token`() = runTest {
        loginActive()
        sessionStore.suspendAccount()
        val requestsBefore = server.requestCount

        val result = repository.restoreSession()

        assertTrue(result.isFailure)
        assertEquals(DomainError.AccountSuspended, domainErrorOf(result))
        assertEquals(requestsBefore, server.requestCount)
        assertEquals("refresh-1", storage.lastRaw)
    }

    @Test
    fun `refresh blocked while suspended without network call`() = runTest {
        loginActive()
        sessionStore.suspendAccount()
        val requestsBefore = server.requestCount

        val result = repository.refreshAccessToken()

        assertTrue(result.isFailure)
        assertEquals(DomainError.AccountSuspended, domainErrorOf(result))
        assertEquals(requestsBefore, server.requestCount)
    }

    @Test
    fun `getCurrentUser blocked while suspended without network call`() = runTest {
        loginActive()
        sessionStore.suspendAccount()
        val requestsBefore = server.requestCount

        val result = repository.getCurrentUser()

        assertTrue(result.isFailure)
        assertEquals(DomainError.AccountSuspended, domainErrorOf(result))
        assertEquals(requestsBefore, server.requestCount)
    }

    @Test
    fun `interceptor blocks protected calls while suspended`() {
        sessionStore.suspendAccount()
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(sessionStore))
            .build()
        val request = Request.Builder().url(server.url("/api/v1/requests")).build()

        assertThrows(AccountSuspendedIOException::class.java) {
            client.newCall(request).execute()
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `interceptor still allows explicit logout while suspended`() {
        sessionStore.suspendAccount()
        server.enqueue(response(200, """{"status":"ok"}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(sessionStore))
            .build()
        val request = Request.Builder()
            .url(server.url("/api/v1/auth/logout"))
            .post("".toByteArray().toRequestBody())
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `in flight 403 account_suspended suspends the session`() {
        sessionStore.activate(
            com.thehouse.app.domain.model.UserSession(
                userId = "u-1",
                displayName = "V",
                email = "v@correo.mx",
                createdAt = "2026-11-05T00:00:00Z",
            ),
            accessToken = "access-1",
        )
        server.enqueue(response(403, """{"status":403,"code":"account_suspended"}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(sessionStore))
            .build()
        val request = Request.Builder().url(server.url("/api/v1/requests")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(403, response.code)
        }
        val state = sessionStore.state.value
        assertTrue(state is SessionState.Suspended)
        assertEquals("u-1", (state as SessionState.Suspended).session?.userId)
    }

    @Test
    fun `suspended store rejects hydration and token updates`() {
        sessionStore.activate(
            com.thehouse.app.domain.model.UserSession(
                userId = "u-1",
                displayName = "V",
                email = "v@correo.mx",
                createdAt = "2026-11-05T00:00:00Z",
            ),
            accessToken = "access-1",
        )
        sessionStore.suspendAccount()

        sessionStore.activate(
            com.thehouse.app.domain.model.UserSession(
                userId = "u-2",
                displayName = "Otro",
                email = "otro@correo.mx",
                createdAt = "2026-11-06T00:00:00Z",
            ),
            accessToken = "access-otro",
        )
        sessionStore.updateAccessToken("access-nuevo")

        val state = sessionStore.state.value
        assertTrue(state is SessionState.Suspended)
        assertEquals("u-1", (state as SessionState.Suspended).session?.userId)
        assertEquals("access-1", sessionStore.accessToken)
    }

    @Test
    fun `explicit clear is the only way back to Anonymous`() {
        sessionStore.suspendAccount()
        sessionStore.clear()
        assertEquals(SessionState.Anonymous, sessionStore.state.value)
        assertNull(sessionStore.accessToken)
    }
}
