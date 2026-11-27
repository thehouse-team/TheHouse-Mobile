package com.thehouse.app.data.visits

import com.thehouse.app.data.remote.api.VisitsApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.data.visits.repository.VisitsRepositoryImpl
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * D20 hotfix: transporte REAL de `POST /api/v1/visits` con la interfaz de
 * producción (sin fakes), Retrofit, Json real y MockWebServer. Demuestra que
 * la petición EMITE y que el cuerpo/headers son los contractuales.
 */
class VisitPostHttpIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: VisitsRepositoryImpl

    private val visitJson = """
        {"id":"visit-1","room":{"id":"room-666f1a2b3c4d5e6f7a8b9c0d","buildingId":"b-1",
         "title":"Cuarto real","city":"Pachuca"},"property":null,
         "scheduledAt":"2026-11-30T21:00:00+00:00","timeZone":"America/Mexico_City",
         "status":"requested","message":"Hola","createdAt":null,"updatedAt":null,"cancelledAt":null}
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
        repository = VisitsRepositoryImpl(retrofit.create(VisitsApi::class.java), json)
    }

    @After
    fun tearDown() { server.close() }

    private fun response(code: Int, body: String = visitJson) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    @Test
    fun `post room visit emits POST with roomId only and UUID key`() = runTest {
        server.enqueue(response(201))
        val creation = repository.createRoomVisit(
            roomId = "room-666f1a2b3c4d5e6f7a8b9c0d",
            scheduledAt = "2026-11-30T15:00:00-06:00",
            message = "Hola",
            idempotencyKey = "5d5f0a9e-2c74-4f5b-9e8f-1234567890ab",
        ).getOrThrow()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/visits", request.url.encodedPath)
        assertEquals("5d5f0a9e-2c74-4f5b-9e8f-1234567890ab", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"roomId\":\"room-666f1a2b3c4d5e6f7a8b9c0d\""))
        assertFalse(body.contains("propertyId"))
        assertTrue(body.contains("2026-11-30T15:00:00-06:00"))
        assertTrue(body.contains("\"message\":\"Hola\""))
        assertFalse(creation.replayed)
        assertEquals("room-666f1a2b3c4d5e6f7a8b9c0d", creation.visit.roomId)
    }

    @Test
    fun `200 replay keeps same key and marks replayed`() = runTest {
        server.enqueue(response(200))
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Idempotent-Replayed", "true")
                .addHeader("Content-Type", "application/json")
                .body(visitJson)
                .build(),
        )
        val key = "5d5f0a9e-2c74-4f5b-9e8f-1234567890ab"
        val first = repository.createRoomVisit("room-666f1a2b3c4d5e6f7a8b9c0d", "2026-11-30T15:00:00-06:00", null, key).getOrThrow()
        val second = repository.createRoomVisit("room-666f1a2b3c4d5e6f7a8b9c0d", "2026-11-30T15:00:00-06:00", null, key).getOrThrow()
        assertFalse(first.replayed)
        assertTrue(second.replayed)
        assertEquals(key, server.takeRequest().headers["Idempotency-Key"])
        assertEquals(key, server.takeRequest().headers["Idempotency-Key"])
    }

    @Test
    fun `422 too soon is surfaced as validation error`() = runTest {
        server.enqueue(
            response(422, """{"type":"about:blank","title":"Unprocessable","status":422,"code":"too_soon"}"""),
        )
        val failure = repository.createRoomVisit(
            "room-666f1a2b3c4d5e6f7a8b9c0d", "2026-11-27T13:00:00-06:00", null, "key-1",
        ).exceptionOrNull() as VisitException
        assertEquals(DomainError.ValidationError, failure.domainError)
        assertEquals(VisitErrorCode.TOO_SOON, failure.code)
        assertEquals("/api/v1/visits", server.takeRequest().url.encodedPath)
    }
}
