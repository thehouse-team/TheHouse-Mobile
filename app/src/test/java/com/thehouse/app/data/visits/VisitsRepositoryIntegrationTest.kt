package com.thehouse.app.data.visits

import com.thehouse.app.data.remote.api.VisitsApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.data.visits.repository.VisitsRepositoryImpl
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.VisitStatus
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

class VisitsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: VisitsRepositoryImpl

    private val visitJson = """
        {
          "id": "visit-1",
          "property": {"id": "prop-1", "title": "Estudio", "city": "Tulancingo"},
          "scheduledAt": "2026-11-20T21:00:00+00:00",
          "timeZone": "America/Mexico_City",
          "status": "requested",
          "message": "Hola",
          "createdAt": "2026-11-19T12:00:00+00:00",
          "updatedAt": "2026-11-19T12:00:00+00:00",
          "cancelledAt": null
        }
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
        repository = VisitsRepositoryImpl(
            api = retrofit.create(VisitsApi::class.java),
            json = json,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun jsonResponse(code: Int, raw: String): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(raw)
            .build()

    @Test
    fun `create 201 returns visit`() = runTest {
        server.enqueue(jsonResponse(201, visitJson))
        val result = repository.createVisit(
            propertyId = "prop-1",
            scheduledAt = "2026-11-20T15:00:00-06:00",
            message = "Hola",
            idempotencyKey = "key-1",
        )
        val creation = result.getOrThrow()
        assertEquals("visit-1", creation.visit.id)
        assertFalse(creation.replayed)
        assertEquals("key-1", server.takeRequest().headers["Idempotency-Key"])
    }

    @Test
    fun `replay 200 marks replayed`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Idempotent-Replayed", "true")
                .body(visitJson)
                .build(),
        )
        val creation = repository.createVisit(
            "prop-1",
            "2026-11-20T15:00:00-06:00",
            null,
            "key-1",
        ).getOrThrow()
        assertTrue(creation.replayed)
    }

    @Test
    fun `list maps requested visits`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[$visitJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )
        val page = repository.getVisits(VisitStatus.REQUESTED).getOrThrow()
        assertEquals(1, page.items.size)
        assertEquals(VisitStatus.REQUESTED, page.items.first().status)
    }

    @Test
    fun `detail 200 returns visit`() = runTest {
        server.enqueue(jsonResponse(200, visitJson))
        assertEquals("visit-1", repository.getVisit("visit-1").getOrThrow().id)
    }

    @Test
    fun `cancel 200 updates status`() = runTest {
        server.enqueue(jsonResponse(200, visitJson.replace("requested", "cancelled")))
        val visit = repository.cancelVisit("visit-1", "sin tiempo").getOrThrow()
        assertEquals(VisitStatus.CANCELLED, visit.status)
    }

    @Test
    fun `cancel repeated 200 is idempotent`() = runTest {
        val cancelled = visitJson.replace("requested", "cancelled")
        server.enqueue(jsonResponse(200, cancelled))
        server.enqueue(jsonResponse(200, cancelled))
        val first = repository.cancelVisit("visit-1")
        val firstError = first.exceptionOrNull() as? VisitException
        assertTrue("${firstError?.domainError} ${firstError?.code}", first.isSuccess)
        assertEquals(VisitStatus.CANCELLED, first.getOrThrow().status)
        val second = repository.cancelVisit("visit-1")
        assertTrue(second.toString(), second.isSuccess)
        assertEquals(VisitStatus.CANCELLED, second.getOrThrow().status)
    }

    @Test
    fun `409 exposes business code`() = runTest {
        server.enqueue(
            jsonResponse(
                409,
                """{"type":"about:blank","title":"Conflict","status":409,"code":"active_visit_for_property"}""",
            ),
        )
        val failure = repository.createVisit("prop-1", "2026-11-20T15:00:00-06:00", null, "k")
            .exceptionOrNull() as VisitException
        assertEquals(DomainError.Conflict, failure.domainError)
        assertEquals(VisitErrorCode.ACTIVE_VISIT_FOR_PROPERTY, failure.code)
    }

    @Test
    fun `422 exposes validation code`() = runTest {
        server.enqueue(
            jsonResponse(
                422,
                """{"type":"about:blank","title":"Unprocessable","status":422,"code":"too_soon"}""",
            ),
        )
        val failure = repository.createVisit("prop-1", "2026-11-20T15:00:00-06:00", null, "k")
            .exceptionOrNull() as VisitException
        assertEquals(VisitErrorCode.TOO_SOON, failure.code)
    }

    @Test
    fun `429 is rate limited`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .addHeader("Retry-After", "30")
                .body("")
                .build(),
        )
        val failure = repository.getVisits().exceptionOrNull() as VisitException
        assertEquals(DomainError.RateLimited, failure.domainError)
        assertEquals(30L, failure.retryAfterSeconds)
    }
}