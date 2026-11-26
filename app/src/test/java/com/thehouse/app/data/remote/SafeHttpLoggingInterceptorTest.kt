package com.thehouse.app.data.remote

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Logging HTTP seguro (secciones 2 y 3 del hardening):
 * método + ruta relativa + status + duración + request id, sin query,
 * sin headers sensibles y con PII de ruta enmascarada.
 */
class SafeHttpLoggingInterceptorTest {

    private lateinit var server: MockWebServer
    private val lines = mutableListOf<String>()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        lines.clear()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun client(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(SafeHttpLoggingInterceptor { message -> lines += message })
            .build()

    private fun enqueue(code: Int, requestId: String?) {
        val builder = MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body("{}")
        if (requestId != null) builder.addHeader("x-request-id", requestId)
        server.enqueue(builder.build())
    }

    @Test
    fun `log incluye metodo ruta status duracion y request id sin query`() {
        enqueue(200, "req-123")

        client().newCall(
            Request.Builder().url(server.url("/api/v1/properties?page=1&pageSize=20")).build(),
        ).execute().use { /* cerrar */ }

        assertEquals(1, lines.size)
        val line = lines.first()
        assertTrue(
            "formato inesperado: $line",
            Regex("""^GET /api/v1/properties -> 200 \(\d+ms\) requestId=req-123$""").matches(line),
        )
        // Nunca se imprime la query completa.
        assertFalse(line.contains("pageSize"))
        assertFalse(line.contains("page="))
        // Nunca se imprimen headers.
        assertFalse(line.contains("Content-Type"))
        assertFalse(line.contains("x-request-id"))
    }

    @Test
    fun `log sin request id omitido mantiene el formato`() {
        enqueue(500, null)

        client().newCall(
            Request.Builder().url(server.url("/api/v1/universities?page=2")).build(),
        ).execute().use { /* cerrar */ }

        val line = lines.first()
        assertTrue(
            "formato inesperado: $line",
            Regex("""^GET /api/v1/universities -> 500 \(\d+ms\)$""").matches(line),
        )
    }

    @Test
    fun `enmascara segmentos de ruta con posible PII`() {
        assertEquals(
            "/api/v1/owners/***/profile",
            SafeHttpLoggingInterceptor.sanitizePath("/api/v1/owners/carlos@correo.mx/profile"),
        )
        assertEquals(
            "/api/v1/properties/000000000000000000000012",
            SafeHttpLoggingInterceptor.sanitizePath("/api/v1/properties/000000000000000000000012"),
        )
    }
}
