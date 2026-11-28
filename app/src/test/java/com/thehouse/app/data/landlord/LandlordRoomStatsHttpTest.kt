package com.thehouse.app.data.landlord

import com.thehouse.app.data.landlord.repository.LandlordRepositoryImpl
import com.thehouse.app.data.remote.api.LandlordApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.landlord.LandlordFlowPolicies
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

/**
 * D21: contrato REAL de `GET /api/v1/landlord/stats/rooms` con MockWebServer.
 * Verifica ruta, query parameters y respuestas (200/400/401/503).
 * El rango se genera con la zona de Ciudad de México, igual que el Backend.
 */
class LandlordRoomStatsHttpTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: LandlordRepositoryImpl

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
        repository = LandlordRepositoryImpl(retrofit.create(LandlordApi::class.java), json)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun response(code: Int, body: String) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private val statsBody = """
        {
          "items": [{
             "roomId":"r1","buildingId":"b1","buildingName":"UD3","number":"101",
             "status":"ready","price":3500,"views":12,"favorites":2,"requests":3,
             "visits":1,"agreements":0,"conversion":0.25,"rating":{"average":4.5,"count":2}
          }],
          "page":1,"pageSize":50,"totalItems":1,"totalPages":1
        }
    """.trimIndent()

    @Test
    fun `stats use exact route and mexico city date range`() = runTest {
        server.enqueue(response(200, statsBody))
        val (from, to) = LandlordFlowPolicies.statsRange()
        val result = repository.getLandlordStatsRooms(from, to)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().items.size)
        val request = server.takeRequest()
        assertEquals("/api/v1/landlord/stats/rooms", request.url.encodedPath)
        assertEquals(from, request.url.queryParameter("from"))
        assertEquals(to, request.url.queryParameter("to"))
        assertEquals(null, request.url.queryParameter("buildingId"))
        assertEquals("1", request.url.queryParameter("page"))
        assertEquals("50", request.url.queryParameter("pageSize"))
    }

    @Test
    fun `stats send building id as query parameter when provided`() = runTest {
        server.enqueue(response(200, statsBody))
        repository.getLandlordStatsRooms("2026-10-30", "2026-11-28", "b1")
        val request = server.takeRequest()
        assertEquals("b1", request.url.queryParameter("buildingId"))
    }

    @Test
    fun `stats 400 for future to is not converted into fake data`() = runTest {
        server.enqueue(response(400, """{"type":"about:blank","status":400,"code":"invalid_range"}"""))
        val result = repository.getLandlordStatsRooms("2026-11-01", "2099-01-01")
        assertTrue(result.isFailure)
    }

    @Test
    fun `stats 401 and 503 propagate as failures`() = runTest {
        server.enqueue(response(401, """{"type":"about:blank","status":401}"""))
        assertTrue(repository.getLandlordStatsRooms("2026-10-30", "2026-11-28").isFailure)
        server.enqueue(response(503, """{"type":"about:blank","status":503}"""))
        assertTrue(repository.getLandlordStatsRooms("2026-10-30", "2026-11-28").isFailure)
    }

    @Test
    fun `stats empty response is honest empty list`() = runTest {
        server.enqueue(response(200, """{"items":[],"page":1,"pageSize":50,"totalItems":0,"totalPages":0}"""))
        val result = repository.getLandlordStatsRooms("2026-10-30", "2026-11-28").getOrThrow()
        assertTrue(result.items.isEmpty())
        assertEquals(0L, result.totalItems)
    }
}
