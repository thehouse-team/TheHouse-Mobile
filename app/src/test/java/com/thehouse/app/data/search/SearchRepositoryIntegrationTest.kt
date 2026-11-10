package com.thehouse.app.data.search

import com.thehouse.app.data.catalog.repository.PropertyRepositoryImpl
import com.thehouse.app.data.remote.api.SearchApi
import com.thehouse.app.data.remote.dto.search.PlaceSearchResponseDto
import com.thehouse.app.data.remote.dto.search.RoomDetailDto
import com.thehouse.app.data.remote.dto.search.RoomMapResponseDto
import com.thehouse.app.data.remote.dto.search.RoomSearchResponseDto
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.MapBounds
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSort
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

/**
 * Integración D10 NV-07 con MockWebServer: contrato real de
 * /rooms/search, /rooms/map, /places/search y /rooms/{id}.
 */
class SearchRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var properties: PropertyRepositoryImpl

    private val universityId = "6578a8b8c29c000000000001"
    private val roomId = "6578a8b8c29c000000000010"
    private val buildingId = "6578a8b8c29c000000000011"

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
        properties = PropertyRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java),
            searchApi = retrofit.create(SearchApi::class.java),
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
    fun `search 200 mapea items reales y envia filtros confirmados`() = runTest {
        server.enqueue(
            body(
                """{"items":[{"id":"$roomId","buildingId":"$buildingId","title":"Cuarto individual","price":3500,"currency":"MXN","roomType":"single","bathroom":"shared","buildingType":"house_rooms","allowedTenants":"women","services":["wifi","security"],"distanceKm":1.2,"walkingMinutes":15,"approxLocation":{"lat":20.08,"lng":-98.36},"thumbnailUrl":null,"demo":true}],"page":2,"pageSize":10,"totalItems":11,"totalPages":2}""",
            ),
        )
        val filters = RoomSearchFilters(
            minPrice = 1500,
            maxPrice = 8000,
            roomType = "single",
            bathroom = "shared",
            buildingType = "house_rooms",
            allowedTenants = "women",
            services = setOf("wifi", "security"),
            maxDistanceKm = 5.0,
            sort = RoomSort.PRICE,
        )
        val result = properties.searchRooms(universityId, filters, page = 2, pageSize = 10)
        assertTrue(result.isSuccess)
        val page = result.getOrNull()
        assertEquals(1, page?.items?.size)
        assertEquals("Cuarto individual", page?.items?.first()?.title)
        assertEquals(3500, page?.items?.first()?.price)
        assertEquals("single", page?.items?.first()?.roomType)
        assertEquals(listOf("wifi", "security"), page?.items?.first()?.services)
        assertEquals(1.2, page?.items?.first()?.distanceKm)
        assertEquals(20.08, page?.items?.first()?.approxLocation?.latitude)
        assertEquals(11L, page?.totalItems)
        assertEquals(2, page?.totalPages)

        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/rooms/search"))
        assertEquals(universityId, req.url.queryParameter("universityId"))
        assertEquals("1500", req.url.queryParameter("minPrice"))
        assertEquals("8000", req.url.queryParameter("maxPrice"))
        assertEquals("single", req.url.queryParameter("roomType"))
        assertEquals("shared", req.url.queryParameter("bathroom"))
        assertEquals("house_rooms", req.url.queryParameter("buildingType"))
        assertEquals("women", req.url.queryParameter("allowedTenants"))
        assertEquals("security,wifi", req.url.queryParameter("services"))
        assertEquals("5.0", req.url.queryParameter("maxDistanceKm"))
        assertEquals("price", req.url.queryParameter("sort"))
        assertEquals("2", req.url.queryParameter("page"))
        assertEquals("10", req.url.queryParameter("pageSize"))
    }

    @Test
    fun `search empty responde success sin items`() = runTest {
        server.enqueue(
            body("""{"items":[],"page":1,"pageSize":20,"totalItems":0,"totalPages":0}"""),
        )
        val result = properties.searchRooms(universityId, RoomSearchFilters())
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.items?.isEmpty() == true)
    }

    @Test
    fun `search 400 mapea BadRequest`() = runTest {
        server.enqueue(MockResponse.Builder().code(400).body("").build())
        val result = properties.searchRooms(universityId, RoomSearchFilters(minPrice = 499))
        assertTrue(result.isFailure)
        assertEquals(DomainError.BadRequest, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `search 429 mapea RateLimited`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).body("").build())
        val result = properties.searchRooms(universityId, RoomSearchFilters())
        assertTrue(result.isFailure)
        assertEquals(DomainError.RateLimited, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `search 503 mapea Server`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).body("").build())
        val result = properties.searchRooms(universityId, RoomSearchFilters())
        assertTrue(result.isFailure)
        assertEquals(DomainError.Server, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `map 200 mapea marcadores y envia bbox exacto`() = runTest {
        server.enqueue(
            body(
                """{"items":[{"id":"$roomId","price":3500,"approxLocation":{"lat":20.08,"lng":-98.36},"distanceKm":1.2}]}""",
            ),
        )
        val bounds = MapBounds(
            minLongitude = -98.37,
            minLatitude = 20.07,
            maxLongitude = -98.35,
            maxLatitude = 20.09,
        )
        val result = properties.getRoomMapMarkers(universityId, RoomSearchFilters(), bounds)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(3500, result.getOrNull()?.first()?.price)
        assertEquals(-98.36, result.getOrNull()?.first()?.approxLocation?.longitude)

        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/rooms/map"))
        assertEquals(universityId, req.url.queryParameter("universityId"))
        assertEquals("-98.370000,20.070000,-98.350000,20.090000", req.url.queryParameter("bbox"))
    }

    @Test
    fun `map 400 mapea BadRequest`() = runTest {
        server.enqueue(MockResponse.Builder().code(400).body("").build())
        val result = properties.getRoomMapMarkers(
            universityId,
            RoomSearchFilters(),
            MapBounds(-98.37, 20.07, -98.35, 20.09),
        )
        assertTrue(result.isFailure)
        assertEquals(DomainError.BadRequest, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `places 200 usa q y universityId y mapea colonia`() = runTest {
        server.enqueue(
            body(
                """{"items":[{"id":"place-1","name":"Centro","type":"neighborhood","approxLocation":{"lat":20.08,"lng":-98.36},"distanceKm":0.8}]}""",
            ),
        )
        val result = properties.searchPlaces("centro", universityId)
        assertTrue(result.isSuccess)
        assertEquals("Centro", result.getOrNull()?.first()?.name)
        assertEquals("neighborhood", result.getOrNull()?.first()?.type)
        assertEquals(0.8, result.getOrNull()?.first()?.distanceKm)

        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/places/search"))
        assertEquals("centro", req.url.queryParameter("q"))
        assertEquals(universityId, req.url.queryParameter("universityId"))
    }

    @Test
    fun `room detail 200 mapea edificio y universidad sin direccion`() = runTest {
        server.enqueue(
            body(
                """{"id":"$roomId","buildingId":"$buildingId","title":"Cuarto individual","price":3500,"currency":"MXN","roomType":"single","bathroom":"shared","sizeM2":14.0,"furniture":["bed","desk"],"availableFrom":"2026-12-01","minContract":"six_months","distanceKm":1.2,"walkingMinutes":15,"approxLocation":{"lat":20.08,"lng":-98.36},"nearestUniversity":{"id":"$universityId","code":"UTTT","name":"Universidad Tecnologica de Tulancingo"},"building":{"id":"$buildingId","name":"Casa Universidad","type":"house_rooms","allowedTenants":"any","services":["wifi","water"],"rules":"No fiestas"},"thumbnailUrl":null,"demo":true}""",
            ),
        )
        val result = properties.getRoomById(roomId)
        assertTrue(result.isSuccess)
        val room = result.getOrNull()
        assertNotNull(room)
        assertEquals("Cuarto individual", room?.title)
        assertEquals(14.0, room?.sizeM2)
        assertEquals(listOf("bed", "desk"), room?.furniture)
        assertEquals("Casa Universidad", room?.building?.name)
        assertEquals(listOf("wifi", "water"), room?.building?.services)
        assertEquals("UTTT", room?.nearestUniversity?.code)
        assertEquals(20.08, room?.approxLocation?.latitude)

        val req = server.takeRequest()
        assertTrue(req.url.encodedPath.endsWith("/api/v1/rooms/$roomId"))
    }

    @Test
    fun `room detail 401 mapea Unauthorized`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).body("").build())
        val result = properties.getRoomById(roomId)
        assertTrue(result.isFailure)
        assertEquals(DomainError.Unauthorized, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `network IOException mapea Network`() = runTest {
        val repo = failingRepository(IOException("sin red"))
        val result = repo.searchRooms(universityId, RoomSearchFilters())
        assertTrue(result.isFailure)
        assertEquals(DomainError.Network, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `timeout mapea Timeout`() = runTest {
        val repo = failingRepository(SocketTimeoutException("timeout"))
        val result = repo.searchRooms(universityId, RoomSearchFilters())
        assertTrue(result.isFailure)
        assertEquals(DomainError.Timeout, (result.exceptionOrNull() as DomainException).error)
    }

    @Test
    fun `cancellation se propaga sin convertirse en Result failure`() = runTest {
        val repo = failingRepository(CancellationException("cancelado"))
        val failure = runCatching {
            repo.searchRooms(universityId, RoomSearchFilters())
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    private fun failingRepository(failure: Throwable): PropertyRepositoryImpl {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(
                JsonProvider.create().asConverterFactory("application/json".toMediaType()),
            )
            .build()
        return PropertyRepositoryImpl(
            api = retrofit.create(com.thehouse.app.data.remote.api.PropertyApi::class.java),
            searchApi = object : SearchApi {
                override suspend fun searchRooms(
                    universityId: String,
                    minPrice: Int?,
                    maxPrice: Int?,
                    roomType: String?,
                    bathroom: String?,
                    buildingType: String?,
                    allowedTenants: String?,
                    services: String?,
                    maxDistanceKm: Double?,
                    sort: String?,
                    page: Int,
                    pageSize: Int,
                ): retrofit2.Response<RoomSearchResponseDto> = throw failure

                override suspend fun getRoomMap(
                    universityId: String,
                    minPrice: Int?,
                    maxPrice: Int?,
                    roomType: String?,
                    bathroom: String?,
                    buildingType: String?,
                    allowedTenants: String?,
                    services: String?,
                    maxDistanceKm: Double?,
                    bbox: String,
                ): retrofit2.Response<RoomMapResponseDto> = throw failure

                override suspend fun searchPlaces(
                    query: String,
                    universityId: String,
                ): retrofit2.Response<PlaceSearchResponseDto> = throw failure

                override suspend fun getRoomById(
                    id: String,
                ): retrofit2.Response<RoomDetailDto> = throw failure
            },
        )
    }
}
