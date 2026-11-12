package com.thehouse.app.data.requests

import com.thehouse.app.data.remote.api.RequestsApi
import com.thehouse.app.data.requests.repository.RequestsRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestStatus
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RequestsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RequestsRepositoryImpl

    private val requestJson = """
        {
          "id": "req-1",
          "status": "pending",
          "room": {"id": "room-1", "buildingId": "b-1", "title": "Cuarto", "city": "Tulancingo"},
          "quickQuestions": ["available_now"],
          "message": "Hola",
          "student": null,
          "landlord": {"name": "Arrendador"},
          "visit": null,
          "conversationId": "conv-1",
          "rejection": null,
          "roomAvailable": true,
          "createdAt": "2026-11-12T12:00:00+00:00",
          "updatedAt": "2026-11-12T12:00:00+00:00"
        }
    """.trimIndent()

    private val landlordRequestJson = requestJson
        .replace("\"student\": null", "\"student\": {\"name\": \"Estudiante\", \"university\": \"UAEH\"}")

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
        repository = RequestsRepositoryImpl(
            api = retrofit.create(RequestsApi::class.java),
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
    fun `create 201 returns request`() = runTest {
        server.enqueue(jsonResponse(201, requestJson))
        val result = repository.createRequest(
            roomId = "room-1",
            quickQuestions = listOf(QuickQuestion.AVAILABLE_NOW),
            message = "Hola",
        )
        val request = result.getOrThrow()
        assertEquals("req-1", request.id)
        assertEquals(RequestStatus.PENDING, request.status)
        assertEquals(listOf(QuickQuestion.AVAILABLE_NOW), request.quickQuestions)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/rooms/room-1/requests", recorded.url.encodedPath)
        assertTrue(recorded.body!!.utf8().contains("available_now"))
        assertTrue(recorded.body!!.utf8().contains("Hola"))
        // Nota: el Bearer lo agrega el interceptor de sesión (no presente en este cliente de prueba).
    }

    @Test
    fun `create without message sends no message field`() = runTest {
        server.enqueue(jsonResponse(201, requestJson))
        repository.createRequest("room-1", emptyList(), null).getOrThrow()
        val body = server.takeRequest().body!!.utf8()
        // encodeDefaults serializa "message":null (válido según contrato); no debe haber valor 1–500.
        assertTrue(body.contains("quickQuestions"))
        assertTrue(!body.contains("\"message\":\""))
    }

    @Test
    fun `409 active_request_exists exposes code`() = runTest {
        server.enqueue(
            jsonResponse(409, """{"status":409,"code":"active_request_exists"}"""),
        )
        val failure = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), null)
            .exceptionOrNull() as RequestException
        assertEquals(DomainError.Conflict, failure.domainError)
        assertEquals(RequestErrorCode.ACTIVE_REQUEST_EXISTS, failure.code)
    }

    @Test
    fun `409 active_requests_limit exposes code`() = runTest {
        server.enqueue(
            jsonResponse(409, """{"status":409,"code":"active_requests_limit"}"""),
        )
        val failure = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), null)
            .exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.ACTIVE_REQUESTS_LIMIT, failure.code)
    }

    @Test
    fun `409 participant_suspended exposes code`() = runTest {
        server.enqueue(
            jsonResponse(409, """{"status":409,"code":"participant_suspended"}"""),
        )
        val failure = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), null)
            .exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.PARTICIPANT_SUSPENDED, failure.code)
    }

    @Test
    fun `422 own_room exposes code`() = runTest {
        server.enqueue(
            jsonResponse(422, """{"status":422,"code":"own_room"}"""),
        )
        val failure = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), null)
            .exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.OWN_ROOM, failure.code)
    }

    @Test
    fun `429 is rate limited`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(429).addHeader("Retry-After", "60").body("").build(),
        )
        val failure = repository.createRequest("room-1", listOf(QuickQuestion.DEPOSIT), null)
            .exceptionOrNull() as RequestException
        assertEquals(DomainError.RateLimited, failure.domainError)
        assertEquals(60L, failure.retryAfterSeconds)
    }

    @Test
    fun `list maps requests with status filter`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[$requestJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )
        val page = repository.getRequests(RequestStatus.PENDING, 1, 20).getOrThrow()
        assertEquals(1, page.items.size)
        assertEquals("room-1", page.items.first().room.id)
        assertEquals("pending", server.takeRequest().url.queryParameter("status"))
    }

    @Test
    fun `detail returns request`() = runTest {
        server.enqueue(jsonResponse(200, requestJson))
        assertEquals("req-1", repository.getRequest("req-1").getOrThrow().id)
    }

    @Test
    fun `cancel 200 updates status`() = runTest {
        server.enqueue(jsonResponse(200, requestJson.replace("pending", "cancelled")))
        val request = repository.cancelRequest("req-1").getOrThrow()
        assertEquals(RequestStatus.CANCELLED, request.status)
    }

    @Test
    fun `cancel 409 request_state exposes code`() = runTest {
        server.enqueue(jsonResponse(409, """{"status":409,"code":"request_state"}"""))
        val failure = repository.cancelRequest("req-1").exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.REQUEST_STATE, failure.code)
    }

    @Test
    fun `landlord list 403 landlord_required`() = runTest {
        server.enqueue(
            jsonResponse(403, """{"status":403,"code":"landlord_required"}"""),
        )
        val failure = repository.getLandlordRequests(null, null, 1, 20).exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.LANDLORD_REQUIRED, failure.code)
        assertEquals("/api/v1/landlord/requests", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `landlord list maps student info`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"items":[$landlordRequestJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""",
            ),
        )
        val page = repository.getLandlordRequests(RequestStatus.PENDING, null, 1, 20).getOrThrow()
        assertEquals("Estudiante", page.items.first().student?.name)
        assertEquals("UAEH", page.items.first().student?.university)
    }

    @Test
    fun `landlord detail maps student`() = runTest {
        server.enqueue(jsonResponse(200, landlordRequestJson))
        val request = repository.getLandlordRequest("req-1").getOrThrow()
        assertEquals("Estudiante", request.student?.name)
    }

    @Test
    fun `accept 200 returns accepted request`() = runTest {
        server.enqueue(jsonResponse(200, requestJson.replace("pending", "accepted")))
        val request = repository.acceptRequest("req-1").getOrThrow()
        assertEquals(RequestStatus.ACCEPTED, request.status)
        assertEquals("/api/v1/landlord/requests/req-1/accept", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `accept 409 room_unavailable exposes code`() = runTest {
        server.enqueue(jsonResponse(409, """{"status":409,"code":"room_unavailable"}"""))
        val failure = repository.acceptRequest("req-1").exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.ROOM_UNAVAILABLE, failure.code)
    }

    @Test
    fun `accept 409 request_state exposes code`() = runTest {
        server.enqueue(jsonResponse(409, """{"status":409,"code":"request_state"}"""))
        val failure = repository.acceptRequest("req-1").exceptionOrNull() as RequestException
        assertEquals(RequestErrorCode.REQUEST_STATE, failure.code)
    }

    @Test
    fun `reject sends reason and note`() = runTest {
        server.enqueue(jsonResponse(200, requestJson.replace("pending", "rejected")))
        val request = repository.rejectRequest("req-1", "not_available", "Sin disponibilidad").getOrThrow()
        assertEquals(RequestStatus.REJECTED, request.status)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body.contains("not_available"))
        assertTrue(body.contains("Sin disponibilidad"))
    }

    @Test
    fun `reject other without note is a backend validation 400`() = runTest {
        server.enqueue(
            jsonResponse(400, """{"status":400,"title":"Entrada inválida"}"""),
        )
        val failure = repository.rejectRequest("req-1", "other", null)
            .exceptionOrNull() as RequestException
        assertEquals(DomainError.Unknown, failure.domainError)
        assertEquals(RequestErrorCode.UNKNOWN, failure.code)
    }
}
