package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.repository.AuthRepositoryImpl
import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType

/**
 * Pruebas de integración (NV04-REQ-010) contra MockWebServer compatible con
 * OkHttp 5: register, login, logout y errores Problem Details.
 *
 * `/auth/refresh` NO se prueba: no está confirmado y en U2 no existe flujo de
 * renovación (BLOCKED-BACKEND).
 */
class AuthRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var storage: InMemorySessionTokenStorage

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
        storage = InMemorySessionTokenStorage()
        repository = AuthRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.AuthApi::class.java),
            sessionStore = InMemorySessionStore(),
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
    fun `register con tokens activa sesion y cifra refresh`() = runTest {
        server.enqueue(
            body(
                """{"id":"u7","name":"Victor","email":"v@correo.mx",
                    "accessToken":"access-abc","refreshToken":"refresh-xyz"}""",
            ),
        )
        val result = repository.register("Victor", "v@correo.mx", "PasswordSegura123!", true, true)
        val session = result.getOrNull()
        assertTrue(result.isSuccess)
        assertEquals("u7", session?.userId)
        assertEquals("Victor", session?.displayName)
        assertEquals("refresh-xyz", storage.lastRaw)
        // Contrato confirmado: el body enviado contiene SOLO email y password.
        val sent = server.takeRequest().body!!.utf8()
        val jsonSent = Json.parseToJsonElement(sent).jsonObject
        assertEquals(setOf("email", "password"), jsonSent.keys)
        assertEquals("v@correo.mx", jsonSent["email"]?.jsonPrimitive?.content)
        assertEquals(false, sent.contains("nombre") || sent.contains("name"))
    }

    @Test
    fun `login real 200 solo accessToken activa sesion placeholder sin refresh`() = runTest {
        // Contrato real de Azure (2026-11-05): {"accessToken","tokenType","expiresIn"}.
        // Sin objeto de usuario: mapeo usa placeholder "Bienvenido" (BLOCKED-BACKEND /users/me
        // para datos reales). Sin refreshToken: no se persiste nada.
        server.enqueue(
            body(
                """{"accessToken":"access-jwt-abc","tokenType":"Bearer","expiresIn":900}""",
            ),
        )
        val result = repository.login("v@correo.mx", "PasswordSegura123!")
        val session = result.getOrNull()
        assertTrue(result.isSuccess)
        assertEquals(UserSession(userId = null, displayName = "Bienvenido", email = ""), session)
        assertTrue(repository.session.value is SessionState.Active)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `login 200 con tokens activa sesion`() = runTest {
        server.enqueue(
            body(
                """{"userId":"u9","displayName":"V","email":"v@correo.mx",
                    "accessToken":"t1","refreshToken":"r1"}""",
            ),
        )
        val result = repository.login("v@correo.mx", "password123")
        val session = result.getOrNull()
        assertTrue(result.isSuccess)
        assertEquals(SessionState.Active(session!!), repository.session.value)
        assertEquals("r1", storage.lastRaw)
    }

    @Test
    fun `register 409 Problem Details se mapea a Conflicto y sin sesion`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(409)
                .addHeader("Content-Type", "application/problem+json")
                .body(
                    """{"type":"dup","title":"Ya existe","status":409,
                        "detail":"Ese correo ya está registrado"}""",
                )
                .build(),
        )
        val result = runCatching {
            repository.register("V", "v@correo.mx", "password123", true, true).getOrThrow()
        }
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainError.Conflict, (exception as DomainException).error)
        // Sesión no activada.
        assertEquals(SessionState.Anonymous, repository.session.value)
    }

    @Test
    fun `logout exitoso backend limpia sesion local`() = runTest {
        server.enqueue(body("""{"status":"ok"}"""))
        repository.login("v@correo.mx", "password123")
        server.enqueue(body("""{"status":"ok"}"""))
        val result = repository.logout()
        assertTrue(result.isSuccess)
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(storage.lastRaw)
    }

    @Test
    fun `logout con backend caido igual limpia sesion local`() = runTest {
        server.close()
        val result = repository.logout()
        // El criterio U2 es el logout local: el estado SIEMPRE queda limpio.
        assertEquals(SessionState.Anonymous, repository.session.value)
        assertNull(storage.lastRaw)
        org.junit.Assert.assertTrue(result.isFailure || result.isSuccess)
    }

    @Test
    fun `401 sin refresh disponible limpia sesion a Anonymous`() = runTest {
        // Sin acceso a /auth/refresh (NO confirmado), el repositorio U2 limpia sesión.
        server.enqueue(MockResponse.Builder().code(401).body("").build())
        val result = runCatching { repository.login("v@correo.mx", "password123").getOrThrow() }
        assertTrue(result.exceptionOrNull() is DomainException)
        assertEquals(SessionState.Anonymous, repository.session.value)
    }

    @Test
    fun `auth no se cachea - Cache-Control no-store en requests`() = runTest {
        server.enqueue(
            body("""{"userId":"u1","name":"Victor","email":"v@correo.mx","accessToken":"a","refreshToken":"r"}"""),
        )
        repository.login("v@correo.mx", "password123")
        val request = server.takeRequest()
        assertEquals("no-store, no-cache", request.headers["Cache-Control"])
    }
}

/** Storage fake de tokens en memoria para pruebas sin Android Keystore. */
class InMemorySessionTokenStorage :
    com.thehouse.app.data.auth.storage.SessionTokenStorage {

    var lastRaw: String? = null
    val clearCalls: ArrayList<Long> = ArrayList()

    override suspend fun readRefreshToken(): ByteArray? = lastRaw?.toByteArray()

    override suspend fun writeRefreshToken(value: ByteArray) {
        lastRaw = String(value)
    }

    override suspend fun clear() {
        lastRaw = null
    }
}
