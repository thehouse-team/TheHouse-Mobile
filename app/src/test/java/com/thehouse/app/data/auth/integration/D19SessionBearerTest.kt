package com.thehouse.app.data.auth.integration

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.data.auth.storage.SessionTokenStorage
import com.thehouse.app.data.remote.auth.BearerTokenInterceptor
import com.thehouse.app.data.remote.auth.RefreshAuthenticator
import com.thehouse.app.data.remote.auth.TokenRefresher
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * D19 T01: una sesión activa sin access token debe recuperarse mediante
 * refresh single-flight y reintentar la request protegida una sola vez.
 */
class D19SessionBearerTest {

    private class MemoryStorage : SessionTokenStorage {
        private var bytes: ByteArray? = null
        override suspend fun writeRefreshToken(value: ByteArray) { bytes = value }
        override suspend fun readRefreshToken(): ByteArray? = bytes
        override suspend fun clear() { bytes = null }
    }

    @Test
    fun `active session without access token refreshes once and retries with bearer`() = runBlocking {
        val server = MockWebServer()
        server.start()
        val store = InMemorySessionStore()
        val storage = MemoryStorage()
        store.activate(
            UserSession(userId = "u1", displayName = "u@correo.mx", email = "u@correo.mx"),
            accessToken = null,
        )
        storage.writeRefreshToken("r-old".toByteArray())
        var refreshCount = 0
        var protectedCount = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.url.encodedPath.endsWith("/api/v1/auth/refresh") -> {
                        refreshCount++
                        MockResponse.Builder()
                            .code(200)
                            .body("""{"accessToken":"a-new","refreshToken":"r-new","tokenType":"Bearer","expiresIn":900}""")
                            .build()
                    }
                    request.headers["Authorization"] == "Bearer a-new" -> {
                        protectedCount++
                        MockResponse.Builder().code(200).body("""{"ok":true}""").build()
                    }
                    else -> MockResponse.Builder().code(401).body("").build()
                }
            }
        }
        val refresher = TokenRefresher(server.url("/").toString(), store, storage)
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(store, refresher))
            .authenticator(RefreshAuthenticator(server.url("/").toString(), store, storage, tokenRefresher = refresher))
            .build()
        val code = client.newCall(Request.Builder().url(server.url("/api/v1/users/me/profile")).build())
            .execute().code
        assertEquals(200, code)
        assertEquals(1, refreshCount)
        assertEquals(1, protectedCount)
        assertEquals("a-new", store.accessToken)
        server.close()
    }

    @Test
    fun `active session without access token and invalid refresh ends anonymous`() = runBlocking {
        val server = MockWebServer()
        server.start()
        val store = InMemorySessionStore()
        val storage = MemoryStorage()
        store.activate(
            UserSession(userId = "u1", displayName = "u@correo.mx", email = "u@correo.mx"),
            accessToken = null,
        )
        storage.writeRefreshToken("r-bad".toByteArray())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse.Builder().code(401).body("").build()
        }
        val refresher = TokenRefresher(server.url("/").toString(), store, storage)
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(store, refresher))
            .authenticator(RefreshAuthenticator(server.url("/").toString(), store, storage, tokenRefresher = refresher))
            .build()
        val code = client.newCall(Request.Builder().url(server.url("/api/v1/users/me/profile")).build())
            .execute().code
        assertEquals(401, code)
        assertNull(store.accessToken)
        assertEquals(SessionState.Anonymous, store.state.value)
        server.close()
    }
}
