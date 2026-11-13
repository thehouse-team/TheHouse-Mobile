package com.thehouse.app.data.reports

import com.thehouse.app.data.remote.api.ReportsApi
import com.thehouse.app.data.reports.repository.ReportsRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.ReportErrorCode
import com.thehouse.app.domain.error.ReportException
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.model.VerificationDecision
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
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

/**
 * D13 (NV-21 §6/§6.1): reportes y verificación del dueño contra el contrato real.
 * Sin endpoints nuevos: solo `POST /api/v1/reports` y `GET .../verification`.
 */
class ReportsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ReportsRepositoryImpl

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
        repository = ReportsRepositoryImpl(
            api = retrofit.create(ReportsApi::class.java),
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

    private suspend fun failure(
        targetType: ReportTargetType = ReportTargetType.ROOM,
        targetId: String = "room-1",
        reason: ReportReason = ReportReason.SPAM,
        details: String? = null,
    ): ReportException =
        repository.createReport(targetType, targetId, reason, details)
            .exceptionOrNull() as ReportException

    @Test
    fun `create 201 sends exact body without userId`() = runTest {
        server.enqueue(
            jsonResponse(
                201,
                """{"id":"rep-1","status":"open","createdAt":"2026-11-12T12:00:00+00:00"}""",
            ),
        )
        val created = repository.createReport(
            targetType = ReportTargetType.ROOM,
            targetId = "room-1",
            reason = ReportReason.FRAUD,
            details = "Precio distinto al publicado",
        ).getOrThrow()
        assertEquals("rep-1", created.id)
        assertEquals("open", created.status)
        assertEquals("2026-11-12T12:00:00+00:00", created.createdAt)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/reports", recorded.url.encodedPath)

        val sent = Json.parseToJsonElement(recorded.body!!.utf8()).jsonObject
        assertEquals(
            setOf("targetType", "targetId", "reason", "details"),
            sent.keys,
        )
        assertEquals("room", sent["targetType"]?.jsonPrimitive?.content)
        assertEquals("room-1", sent["targetId"]?.jsonPrimitive?.content)
        assertEquals("fraud", sent["reason"]?.jsonPrimitive?.content)
        assertEquals("Precio distinto al publicado", sent["details"]?.jsonPrimitive?.content)
        assertFalse(recorded.body!!.utf8().contains("userId"))
        assertFalse(recorded.body!!.utf8().contains("user_id"))
    }

    @Test
    fun `create with building target keeps contract enums`() = runTest {
        server.enqueue(
            jsonResponse(
                201,
                """{"id":"rep-2","status":"open","createdAt":"2026-11-12T13:00:00+00:00"}""",
            ),
        )
        repository.createReport(ReportTargetType.BUILDING, "b-1", ReportReason.OTHER, null)
            .getOrThrow()
        val sent = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals("building", sent["targetType"]?.jsonPrimitive?.content)
        assertEquals("other", sent["reason"]?.jsonPrimitive?.content)
        // details opcional: encodeDefaults envía null y el Backend lo ignora.
        assertTrue(sent.containsKey("details"))
    }

    @Test
    fun `400 validation exposes VALIDATION code`() = runTest {
        server.enqueue(jsonResponse(400, """{"status":400,"title":"Entrada inválida"}"""))
        val failure = failure()
        assertEquals(ReportErrorCode.VALIDATION, failure.code)
        assertEquals(DomainError.Unknown, failure.domainError)
    }

    @Test
    fun `404 unknown target maps to NotFound`() = runTest {
        server.enqueue(jsonResponse(404, """{"status":404,"title":"No encontrado"}"""))
        assertEquals(DomainError.NotFound, failure().domainError)
    }

    @Test
    fun `409 report_exists exposes code and Conflict`() = runTest {
        server.enqueue(
            jsonResponse(409, """{"status":409,"code":"report_exists"}"""),
        )
        val failure = failure()
        assertEquals(ReportErrorCode.REPORT_EXISTS, failure.code)
        assertEquals(DomainError.Conflict, failure.domainError)
    }

    @Test
    fun `429 keeps Retry-After`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .addHeader("Retry-After", "60")
                .body("")
                .build(),
        )
        val failure = failure()
        assertEquals(DomainError.RateLimited, failure.domainError)
        assertEquals(60L, failure.retryAfterSeconds)
    }

    @Test
    fun `503 maps to Server`() = runTest {
        server.enqueue(jsonResponse(503, """{"status":503,"title":"Temporal"}"""))
        assertEquals(DomainError.Server, failure().domainError)
    }

    @Test
    fun `401 maps to Unauthorized`() = runTest {
        server.enqueue(jsonResponse(401, """{"status":401,"title":"Unauthorized"}"""))
        assertEquals(DomainError.Unauthorized, failure().domainError)
    }

    @Test
    fun `403 account_suspended maps to AccountSuspended`() = runTest {
        server.enqueue(
            jsonResponse(403, """{"status":403,"code":"account_suspended"}"""),
        )
        val failure = failure()
        assertEquals(DomainError.AccountSuspended, failure.domainError)
        assertEquals(ReportErrorCode.ACCOUNT_SUSPENDED, failure.code)
    }

    @Test
    fun `verification 200 rejected keeps status and decision reason`() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"status":"rejected","lastDecision":{
                    "decision":"rejected","reason":"Documentos inválidos",
                    "decidedAt":"2026-11-10T10:00:00+00:00"}}""",
            ),
        )
        val info = repository.getBuildingVerification("b-1").getOrThrow()
        assertEquals(BuildingStatus.REJECTED, info.status)
        assertEquals(VerificationDecision.REJECTED, info.lastDecision?.decision)
        assertEquals("Documentos inválidos", info.lastDecision?.reason)
        assertEquals(
            "/api/v1/landlord/buildings/b-1/verification",
            server.takeRequest().url.encodedPath,
        )
    }

    @Test
    fun `verification 200 suspended status parses without decision`() = runTest {
        server.enqueue(jsonResponse(200, """{"status":"suspended"}"""))
        val info = repository.getBuildingVerification("b-1").getOrThrow()
        assertEquals(BuildingStatus.SUSPENDED, info.status)
        assertNull(info.lastDecision)
    }

    @Test
    fun `verification 404 of another owner maps to NotFound`() = runTest {
        server.enqueue(jsonResponse(404, """{"status":404,"title":"No encontrado"}"""))
        val failure = repository.getBuildingVerification("b-2").exceptionOrNull() as ReportException
        assertEquals(DomainError.NotFound, failure.domainError)
    }

    @Test
    fun `verification 403 account_suspended maps to AccountSuspended`() = runTest {
        server.enqueue(jsonResponse(403, """{"status":403,"code":"account_suspended"}"""))
        val failure = repository.getBuildingVerification("b-1").exceptionOrNull() as ReportException
        assertEquals(DomainError.AccountSuspended, failure.domainError)
        assertEquals(ReportErrorCode.ACCOUNT_SUSPENDED, failure.code)
    }

    @Test
    fun `building status nuevos se parsean rejected y suspended`() {
        assertEquals(BuildingStatus.REJECTED, BuildingStatus.fromApi("rejected"))
        assertEquals(BuildingStatus.SUSPENDED, BuildingStatus.fromApi("suspended"))
        assertEquals(BuildingStatus.VERIFIED, BuildingStatus.fromApi("verified"))
        // Valor desconocido no inventa estado: cae a DRAFT (comportamiento D12).
        assertEquals(BuildingStatus.DRAFT, BuildingStatus.fromApi("desconocido"))
    }

    @Test
    fun `report enums del contrato son exactos`() {
        assertEquals(
            listOf("building", "room", "user"),
            ReportTargetType.entries.map { it.api },
        )
        assertEquals(
            listOf("fake", "fraud", "inappropriate", "spam", "other"),
            ReportReason.entries.map { it.api },
        )
    }
}
