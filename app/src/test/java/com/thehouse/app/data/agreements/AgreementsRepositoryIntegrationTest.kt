package com.thehouse.app.data.agreements

import com.thehouse.app.data.agreements.repository.AgreementsRepositoryImpl
import com.thehouse.app.data.remote.api.AgreementsApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.AgreementErrorCode
import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.AgreementStatus
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AgreementsRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AgreementsRepositoryImpl

    private val landlordAgreementJson = """
        {
          "id":"ag-1",
          "room":{"id":"room-1","buildingId":"b-1","title":"Cuarto 1","city":"Tulancingo"},
          "student":{"name":"Ana","university":"UAEH"},
          "terms":{"monthlyRent":4500,"deposit":4500,"startDate":"2026-12-01","durationMonths":6,"endDate":"2027-05-31","paymentDay":5,"currency":"MXN"},
          "status":"pending",
          "closingCode":null,
          "createdAt":"2026-11-13T10:00:00Z",
          "updatedAt":"2026-11-13T10:00:00Z",
          "activatedAt":null,
          "endedAt":null,
          "cancelledAt":null
        }
    """.trimIndent()

    private val landlordAgreementWithCodeJson = landlordAgreementJson.replace(
        "\"closingCode\":null",
        "\"closingCode\":{\"qrToken\":\"qt\",\"backupCode\":\"K7PM-2QXA\",\"expiresAt\":\"2026-11-13T10:10:00Z\"}",
    )

    private val previewJson = """
        {
          "id":"ag-1",
          "room":{"id":"room-1","buildingId":"b-1","title":"Cuarto 1","city":"Tulancingo"},
          "building":{"name":"Casa Norte","rules":"No fumar"},
          "landlord":{"name":"Carlos"},
          "terms":{"monthlyRent":4500,"deposit":4500,"startDate":"2026-12-01","durationMonths":6,"endDate":"2027-05-31","paymentDay":5,"currency":"MXN"},
          "codeExpiresAt":"2026-11-13T10:10:00Z"
        }
    """.trimIndent()

    private val rentalJson = """
        {
          "agreementId":"ag-1",
          "room":{"id":"room-1","buildingId":"b-1","title":"Cuarto 1","city":"Tulancingo"},
          "building":{"name":"Casa Norte","rules":"No fumar"},
          "terms":{"monthlyRent":4500,"deposit":4500,"startDate":"2026-12-01","durationMonths":6,"endDate":"2027-05-31","paymentDay":5,"currency":"MXN"},
          "nextPayment":{"dueDate":"2026-12-05","amount":4500},
          "address":{"street":"Calle 1","number":"10","neighborhood":"Centro","city":"Tulancingo","state":"Hidalgo","postalCode":"43600"},
          "location":{"lat":20.08,"lng":-98.36},
          "landlord":{"name":"Carlos"},
          "conversationId":"conv-1",
          "activatedAt":"2026-11-13T10:05:00Z"
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
        repository = AgreementsRepositoryImpl(
            api = retrofit.create(AgreementsApi::class.java),
            json = json,
        )
    }

    @After
    fun tearDown() { server.close() }

    private fun jsonResponse(code: Int, raw: String): MockResponse =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(raw).build()

    @Test
    fun `create 201 sends exact body`() = runTest {
        server.enqueue(jsonResponse(201, landlordAgreementWithCodeJson))
        val result = repository.createAgreement("req-1", 4500, 4500, "2026-12-01", 6, 5).getOrThrow()
        assertEquals("ag-1", result.id)
        assertEquals("K7PM-2QXA", result.closingCode?.backupCode)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/landlord/agreements", recorded.url.encodedPath)
        val body = recorded.body!!.utf8()
        assertTrue(body.contains("\"requestId\":\"req-1\""))
        assertTrue(body.contains("\"monthlyRent\":4500"))
        assertTrue(body.contains("\"deposit\":4500"))
        assertTrue(body.contains("\"startDate\":\"2026-12-01\""))
        assertTrue(body.contains("\"durationMonths\":6"))
        assertTrue(body.contains("\"paymentDay\":5"))
    }

    @Test
    fun `list filter pagination and detail closingCode null`() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[$landlordAgreementJson],"page":2,"pageSize":20,"totalItems":1,"totalPages":2}"""))
        val page = repository.getLandlordAgreements(AgreementStatus.PENDING, "room-1", 2, 20).getOrThrow()
        assertEquals(1, page.items.size)
        assertEquals(2, page.page)
        val req = server.takeRequest()
        assertEquals("pending", req.url.queryParameter("status"))
        assertEquals("room-1", req.url.queryParameter("roomId"))
        assertEquals("2", req.url.queryParameter("page"))

        server.enqueue(jsonResponse(200, landlordAgreementJson))
        val detail = repository.getLandlordAgreement("ag-1").getOrThrow()
        assertNull(detail.closingCode)
    }

    @Test
    fun `issue code and end request`() = runTest {
        server.enqueue(jsonResponse(200, landlordAgreementWithCodeJson))
        val issued = repository.issueAgreementCode("ag-1").getOrThrow()
        assertNotNull(issued.closingCode)
        assertEquals("/api/v1/landlord/agreements/ag-1/code", server.takeRequest().url.encodedPath)

        server.enqueue(jsonResponse(200, landlordAgreementJson.replace("\"pending\"", "\"ended\"")))
        repository.endAgreement("ag-1", "other", "nota").getOrThrow()
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body.contains("\"reason\":\"other\""))
        assertTrue(body.contains("\"note\":\"nota\""))
    }

    @Test
    fun `landlord conflict and forbidden errors`() = runTest {
        server.enqueue(jsonResponse(409, """{"status":409,"code":"agreement_open"}"""))
        val open = repository.createAgreement("req-1", 4500, 4500, "2026-12-01", 6, 5).exceptionOrNull() as AgreementException
        assertEquals(DomainError.Conflict, open.domainError)
        assertEquals(AgreementErrorCode.AGREEMENT_OPEN, open.code)

        server.enqueue(jsonResponse(403, """{"status":403,"code":"landlord_required"}"""))
        val forbidden = repository.getLandlordAgreements().exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.LANDLORD_REQUIRED, forbidden.code)
    }

    @Test
    fun `student preview accept and rental`() = runTest {
        server.enqueue(jsonResponse(200, previewJson))
        val preview = repository.previewAgreement(code = "k7pm-2qxa", qrToken = null).getOrThrow()
        assertEquals("ag-1", preview.id)
        assertEquals("Casa Norte", preview.building.name)
        val prevReq = server.takeRequest()
        assertTrue(prevReq.body!!.utf8().contains("\"code\":\"k7pm-2qxa\""))

        server.enqueue(jsonResponse(200, rentalJson))
        val rental = repository.acceptAgreement(code = "k7pm-2qxa", qrToken = null, acceptedTerms = true).getOrThrow()
        assertEquals("conv-1", rental.conversationId)
        assertEquals(4500, rental.nextPayment?.amount)
        val accBody = server.takeRequest().body!!.utf8()
        assertTrue(accBody.contains("\"acceptedTerms\":true"))

        server.enqueue(jsonResponse(200, rentalJson))
        val myRental = repository.getMyRental().getOrThrow()
        assertEquals("ag-1", myRental.agreementId)
        assertEquals("/api/v1/users/me/rental", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `student code errors and retry after`() = runTest {
        server.enqueue(jsonResponse(404, """{"status":404,"code":"code_invalid"}"""))
        val invalid = repository.previewAgreement("bad", null).exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.CODE_INVALID, invalid.code)

        server.enqueue(jsonResponse(410, """{"status":410,"code":"code_expired"}"""))
        val expired = repository.previewAgreement("x", null).exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.CODE_EXPIRED, expired.code)
        assertEquals(DomainError.Gone, expired.domainError)

        server.enqueue(jsonResponse(410, """{"status":410,"code":"code_used"}"""))
        val used = repository.previewAgreement("x", null).exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.CODE_USED, used.code)

        server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "900").addHeader("Content-Type", "application/json").body("""{"status":429,"code":"too_many_attempts"}""").build())
        val limited = repository.previewAgreement("x", null).exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.TOO_MANY_ATTEMPTS, limited.code)
        assertEquals(900L, limited.retryAfterSeconds)

        server.enqueue(jsonResponse(404, """{"status":404,"code":"no_active_rental"}"""))
        val none = repository.getMyRental().exceptionOrNull() as AgreementException
        assertEquals(AgreementErrorCode.NO_ACTIVE_RENTAL, none.code)
    }
}
