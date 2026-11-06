package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.remote.auth.RefreshAuthenticator
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** D3 NV-06 Persistent Session + NV-27 refresh/me. */
class PersistentSessionIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var store: InMemorySessionStore
    private lateinit var storage: InMemorySessionTokenStorage
    private lateinit var repository: com.thehouse.app.data.auth.repository.AuthRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val ok = OkHttpClient.Builder().build()
        val json = JsonProvider.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(ok)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        store = InMemorySessionStore()
        storage = InMemorySessionTokenStorage()
        repository = com.thehouse.app.data.auth.repository.AuthRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.AuthApi::class.java),
            sessionStore = store,
            refreshTokenStorage = storage,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun body(raw: String): MockResponse =
        MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `login guarda refreshToken`() = runTest {
        server.enqueue(
            body(
                """{"userId":"u9","email":"v@correo.mx","accessToken":"a1","refreshToken":"r1","tokenType":"Bearer","expiresIn":900}""",
            ),
        )
        val result = repository.login("v@correo.mx", "password123")
        assertTrue(result.isSuccess)
        assertEquals("r1", storage.lastRaw)
        assertEquals("a1", store.accessToken)
        assertTrue(repository.session.value is SessionState.Active)
    }

    @Test
    fun `refresh 200 rota y reemplaza anterior`() = runTest {
        storage.writeRefreshToken("r-old".toByteArray())
        store.activate(
            UserSession(userId = "u1", displayName = "u@correo.mx", email = "u@correo.mx"),
            "a-old",
        )
        server.enqueue(
            body(
                """{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}""",
            ),
        )
        val result = repository.refreshAccessToken()
        assertTrue(result.isSuccess)
        assertEquals("a-new", result.getOrNull())
        assertEquals("a-new", store.accessToken)
        assertEquals("r-new", storage.lastRaw)
        assertTrue(storage.lastRaw != "r-old")
        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/auth/refresh"))
        assertTrue(req.body!!.utf8().contains("r-old"))
    }

    @Test
    fun `refresh 401 limpia sesion y storage`() = runTest {
        storage.writeRefreshToken("r-old".toByteArray())
        store.activate(
            UserSession(userId = "u1", displayName = "u@correo.mx", email = "u@correo.mx"),
            "a-old",
        )
        server.enqueue(MockResponse.Builder().code(401).body("").build())
        val result = repository.refreshAccessToken()
        assertTrue(result.isFailure)
        assertEquals(DomainError.Unauthorized, (result.exceptionOrNull() as DomainException).error)
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(store.accessToken)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `users-me 200 hidrata identidad real`() = runTest {
        store.activate(
            UserSession(userId = null, displayName = "Bienvenido", email = ""),
            "a1",
        )
        server.enqueue(
            body("""{"id":"u-real","email":"v@correo.mx","createdAt":"2026-11-05T00:00:00Z"}"""),
        )
        val result = repository.getCurrentUser()
        assertTrue(result.isSuccess)
        assertEquals("u-real", result.getOrNull()?.userId)
        assertEquals("v@correo.mx", result.getOrNull()?.email)
        assertEquals("2026-11-05T00:00:00Z", result.getOrNull()?.createdAt)
        assertEquals("v@correo.mx", result.getOrNull()?.displayName)
        assertEquals("Bearer a1", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `users-me 401 con refresh reintenta una vez`() = runTest {
        store.activate(
            UserSession(userId = "u1", displayName = "x", email = "x@correo.mx"),
            "a-old",
        )
        storage.writeRefreshToken("r-old".toByteArray())
        server.enqueue(MockResponse.Builder().code(401).body("").build())
        server.enqueue(
            body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}"""),
        )
        server.enqueue(
            body("""{"id":"u1","email":"x@correo.mx","createdAt":"2026-11-05T00:00:00Z"}"""),
        )
        val result = repository.getCurrentUser()
        assertTrue(result.isSuccess)
        assertEquals("r-new", storage.lastRaw)
        assertEquals("a-new", store.accessToken)
    }

    @Test
    fun `retry de me tambien 401 no refresca mas y limpia`() = runTest {
        store.activate(
            UserSession(userId = "u1", displayName = "x", email = "x@correo.mx"),
            "a-old",
        )
        storage.writeRefreshToken("r-old".toByteArray())
        val refreshCount = AtomicInteger(0)
        val meCount = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath.endsWith("/api/v1/auth/refresh") -> {
                        refreshCount.incrementAndGet()
                        body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}""")
                    }
                    request.url.encodedPath.endsWith("/api/v1/users/me") -> {
                        meCount.incrementAndGet()
                        MockResponse.Builder().code(401).body("").build()
                    }
                    else -> MockResponse.Builder().code(404).body("").build()
                }
            }
        }
        val result = repository.getCurrentUser()
        assertTrue(result.isFailure)
        assertEquals(2, meCount.get())
        assertEquals(1, refreshCount.get())
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `restart restaura sesion con refresh y users-me`() = runTest {
        storage.writeRefreshToken("r-old".toByteArray())
        assertEquals(SessionState.Anonymous, repository.session.value)
        server.enqueue(
            body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}"""),
        )
        server.enqueue(
            body("""{"id":"u-real","email":"v@correo.mx","createdAt":"2026-11-05T00:00:00Z"}"""),
        )
        val result = repository.restoreSession()
        assertTrue(result.isSuccess)
        assertEquals("u-real", result.getOrNull()?.userId)
        assertEquals("v@correo.mx", result.getOrNull()?.displayName)
        assertEquals("r-new", storage.lastRaw)
        assertEquals("a-new", store.accessToken)
        assertTrue(repository.session.value is SessionState.Active)
    }

    @Test
    fun `restart sin refresh queda Anonymous sin red`() = runTest {
        assertNull(storage.lastRaw)
        val result = repository.restoreSession()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `restart refresh 401 limpia y queda Anonymous`() = runTest {
        storage.writeRefreshToken("r-old".toByteArray())
        server.enqueue(MockResponse.Builder().code(401).body("").build())
        val result = repository.restoreSession()
        assertTrue(result.isFailure)
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(store.accessToken)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `multiples 401 concurrentes ejecutan exactamente 1 refresh`() = runTest {
        store.activate(
            UserSession(userId = "u1", displayName = "x@correo.mx", email = "x@correo.mx"),
            "a-old",
        )
        storage.writeRefreshToken("r-old".toByteArray())
        val refreshCount = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath.endsWith("/api/v1/auth/refresh") -> {
                        refreshCount.incrementAndGet()
                        body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}""")
                    }
                    request.url.encodedPath.endsWith("/api/v1/users/me") &&
                        request.headers["Authorization"] == "Bearer a-new" -> {
                        body("""{"id":"u1","email":"x@correo.mx","createdAt":"2026-11-05T00:00:00Z"}""")
                    }
                    request.url.encodedPath.endsWith("/api/v1/users/me") -> {
                        MockResponse.Builder().code(401).body("").build()
                    }
                    else -> MockResponse.Builder().code(404).body("").build()
                }
            }
        }
        val results = List(5) {
            async { repository.getCurrentUser() }
        }.awaitAll()
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, refreshCount.get())
        assertEquals("r-new", storage.lastRaw)
        assertEquals("a-new", store.accessToken)
    }

    @Test
    fun `logout mantiene limpieza local`() = runTest {
        store.activate(
            UserSession(userId = "u1", displayName = "x@correo.mx", email = "x@correo.mx"),
            "a1",
        )
        storage.writeRefreshToken("r1".toByteArray())
        server.enqueue(MockResponse.Builder().code(204).body("").build())
        val result = repository.logout()
        assertTrue(result.isSuccess)
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(store.accessToken)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `register sin regresion body solo email-password`() = runTest {
        server.enqueue(
            body(
                """{"id":"u7","name":"Victor","email":"v@correo.mx","accessToken":"access-abc","refreshToken":"refresh-xyz","tokenType":"Bearer","expiresIn":900}""",
            ),
        )
        val result = repository.register("Victor", "v@correo.mx", "PasswordSegura123!", true, true)
        assertTrue(result.isSuccess)
        assertEquals("u7", result.getOrNull()?.userId)
        assertEquals("refresh-xyz", storage.lastRaw)
        val sent = server.takeRequest().body!!.utf8()
        val jsonSent = kotlinx.serialization.json.Json.parseToJsonElement(sent).jsonObject
        assertEquals(setOf("email", "password"), jsonSent.keys)
    }

    @Test
    fun `authenticator single-flight ante 401 concurrentes`() = runTest {
        store.activate(
            UserSession(userId = "u1", displayName = "x@correo.mx", email = "x@correo.mx"),
            "a-old",
        )
        storage.writeRefreshToken("r-old".toByteArray())
        val refreshCount = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath.endsWith("/api/v1/auth/refresh") -> {
                        refreshCount.incrementAndGet()
                        body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}""")
                    }
                    request.headers["Authorization"] == "Bearer a-new" -> body("""{"ok":true}""")
                    else -> MockResponse.Builder().code(401).body("").build()
                }
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(
                com.thehouse.app.data.remote.auth.BearerTokenInterceptor(store),
            )
            .authenticator(
                RefreshAuthenticator(server.url("/").toString(), store, storage),
            )
            .build()
        val codes = List(5) {
            async {
                val req = Request.Builder().url(server.url("/protected")).build()
                client.newCall(req).execute().code
            }
        }.awaitAll()
        assertTrue(codes.all { it == 200 })
        assertEquals(1, refreshCount.get())
        assertEquals("r-new", storage.lastRaw)
    }
}
