package com.thehouse.app.device

import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.room.RoomRepository
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.usecase.GetRoomDetailUseCase
import com.thehouse.app.presentation.navigation.RoomDetailRoute
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** D21 adenda: room target consume GET /rooms/{id} real; 404/401 sin crash. */
class D21RoomTargetHttpTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: GetRoomDetailUseCase
    private val roomId = "6aef1234-1111-2222-3333-444455556666"

    private val roomJson = """
        {"id":"$roomId","buildingId":"b-1","title":"Cuarto real","price":3200,
         "currency":"MXN","roomType":"single","bathroom":"private",
         "sizeM2":12.0,"furniture":[],"availableFrom":null,"minContract":null,
         "distanceKm":2.3,"walkingMinutes":28,
         "approxLocation":{"lat":19.55,"lng":-99.35},
         "nearestUniversity":null,
         "building":{"id":"b-1","name":"Edificio real","type":"rooms_building",
                     "allowedTenants":"women","services":["wifi"],"rules":null},
         "thumbnailUrl":null,"demo":false,"photos":[]}
    """.trimIndent()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JsonProvider.create().asConverterFactory("application/json".toMediaType()))
            .build()
        useCase = GetRoomDetailUseCase(RoomRepository(retrofit.create(RoomApi::class.java)))
    }

    @After
    fun tearDown() { server.close() }

    private fun enqueue(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun room_200_maps_real_room_without_propertyId() = runTest {
        enqueue(200, roomJson)
        val result = useCase(roomId)
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/rooms/$roomId", request.target)
    }

    @Test
    fun room_404_is_handled_without_crash() = runTest {
        enqueue(404, """{"status":404,"detail":"El cuarto no existe."}""")
        val result = useCase(roomId)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun room_401_requires_auth() = runTest {
        enqueue(401, """{"status":401}""")
        val result = useCase(roomId)
        assertTrue(result.isFailure)
        server.takeRequest()
    }

    @Test
    fun room_route_uses_real_id_and_never_the_literal_placeholder() {
        val route = RoomDetailRoute(roomId).route
        assertEquals("cuarto/$roomId", route)
        assertFalse(route.contains("{roomId}"))
        assertFalse(route.contains("property"))
        assertTrue(RoomDetailRoute.isValid(roomId))
        assertFalse(RoomDetailRoute.isValid(""))
    }
}
