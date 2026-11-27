package com.thehouse.app.data.room

import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.room.RoomSearchFilters
import com.thehouse.app.domain.usecase.SearchRoomsUseCase
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * D20 hotfix: prueba REAL de transporte HTTP. Usa la interfaz [RoomApi] de
 * producción (sin fakes), Retrofit con el Json real y MockWebServer.
 * Demuestra que la petición llega al servidor y que se deserializa.
 */
class RoomSearchHttpIntegrationTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(kotlinx.coroutines.test.UnconfinedTestDispatcher())

    private lateinit var server: MockWebServer
    private lateinit var api: RoomApi
    private lateinit var repository: RoomSearchRepositoryImpl

    private val roomJson = """
        {"id":"room-666f1a2b3c4d5e6f7a8b9c0d","buildingId":"b-1","title":"Cuarto Tepeji",
         "price":3200,"currency":"MXN","roomType":"single","bathroom":"private",
         "buildingType":"rooms_building","allowedTenants":"women","services":["wifi"],
         "distanceKm":2.3,"walkingMinutes":28,"approxLocation":{"lat":19.55,"lng":-99.35},
         "thumbnailUrl":null,"demo":true}
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
        // RoomApi de producción, sin fake: el proxy Retrofit debe emitir la petición.
        api = retrofit.create(RoomApi::class.java)
        repository = RoomSearchRepositoryImpl(api)
    }

    @After
    fun tearDown() { server.close() }

    private fun enqueue(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun request(): RecordedRequest = server.takeRequest()

    /** Espera a que OkHttp termine de leer la respuesta y el VM aplique estado. */
    private fun waitForContent(vm: com.thehouse.app.presentation.home.HomeViewModel): com.thehouse.app.presentation.catalog.RoomSearchState {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val state = vm.uiState.value.rooms
            if (state !is RoomSearchState.Loading) return state
            Thread.sleep(25)
        }
        return vm.uiState.value.rooms
    }

    private fun waitForContent(vm: com.thehouse.app.presentation.explore.ExploreViewModel): com.thehouse.app.presentation.catalog.RoomSearchState {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val state = vm.uiState.value.rooms
            if (state !is RoomSearchState.Loading) return state
            Thread.sleep(25)
        }
        return vm.uiState.value.rooms
    }

    @Test
    fun `GET rooms search real reaches server with contractual parameters and deserializes`() = runTest {
        enqueue("""{"items":[$roomJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""")
        val filters = RoomSearchFilters(
            universityId = "6aeecc11f3c9f3f762946293",
            minPrice = 500, maxPrice = 50000, roomType = "single", bathroom = "private",
            buildingType = "rooms_building", allowedTenants = "women",
            services = listOf("wifi"), maxDistanceKm = 10.0, sort = "distance",
            page = 1, pageSize = 20,
        )
        val result = repository.searchRooms(filters).getOrThrow()
        val sent = request()
        assertEquals("GET", sent.method)
        assertEquals("/api/v1/rooms/search", sent.url.encodedPath)
        val q = sent.url.queryParameterNames.orEmpty().associateWith { sent.url.queryParameter(it) }
        assertEquals("6aeecc11f3c9f3f762946293", q["universityId"])
        assertEquals("500", q["minPrice"])
        assertEquals("50000", q["maxPrice"])
        assertEquals("single", q["roomType"])
        assertEquals("private", q["bathroom"])
        assertEquals("rooms_building", q["buildingType"])
        assertEquals("women", q["allowedTenants"])
        assertEquals("wifi", q["services"])
        assertEquals("10.0", q["maxDistanceKm"])
        assertEquals("distance", q["sort"])
        assertEquals("1", q["page"])
        assertEquals("20", q["pageSize"])
        assertEquals("q", false, q.containsKey("q"))
        assertEquals(1, result.items.size)
        assertEquals("room-666f1a2b3c4d5e6f7a8b9c0d", result.items.single().roomId)
        assertEquals(1L, result.totalItems)
    }

    @Test
    fun `HTTP 200 empty list produces empty page not error`() = runTest {
        enqueue("""{"items":[],"page":1,"pageSize":20,"totalItems":0,"totalPages":0}""")
        val page = repository.searchRooms(RoomSearchFilters(universityId = "u-empty")).getOrThrow()
        assertEquals(0, page.items.size)
        request()
    }

    @Test
    fun `HTTP 400 is contractual validation error not empty success`() = runTest {
        enqueue("""{"type":"validation","title":"Bad Request","status":400}""", 400)
        val result = repository.searchRooms(RoomSearchFilters(universityId = "bad"))
        assertTrue(result.isFailure)
        assertEquals(DomainError.ValidationError, (result.exceptionOrNull() as DomainException).error)
        request()
    }

    @Test
    fun `HTTP 503 is service unavailable not empty success`() = runTest {
        enqueue("""{"type":"service","title":"Unavailable","status":503}""", 503)
        val result = repository.searchRooms(RoomSearchFilters(universityId = "down"))
        assertTrue(result.isFailure)
        assertEquals(DomainError.ServiceUnavailable, (result.exceptionOrNull() as DomainException).error)
        request()
    }

    @Test
    fun `network failure is controlled error not empty success`() = runTest {
        server.close()
        val result = repository.searchRooms(RoomSearchFilters(universityId = "offline"))
        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as DomainException).error
        assertTrue(error == DomainError.Network || error == DomainError.Unknown)
    }

    @Test
    fun `invalid JSON is deserialization error not empty success`() = runTest {
        enqueue("""{"items":"NOT_AN_ARRAY","page":"x"}""")
        val result = repository.searchRooms(RoomSearchFilters(universityId = "bad-json"))
        assertTrue(result.isFailure)
        request()
    }

    @Test
    fun `cancellation is not converted into false empty`() = runTest {
        enqueue("""{"items":[$roomJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""")
        var cancelled = false
        kotlinx.coroutines.coroutineScope {
            val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
                repository.searchRooms(RoomSearchFilters(universityId = "cancel"))
            }
            job.cancel(CancellationException("test"))
            kotlinx.coroutines.yield()
            cancelled = job.isCancelled
        }
        assertTrue(cancelled)
    }

    @Test
    fun `home reaches Content with real Retrofit repository over MockWebServer`() = runTest {
        enqueue("""{"items":[$roomJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""")
        val vm = HomeViewModel(
            logoutUseCase = com.thehouse.app.domain.usecase.LogoutUseCase(
                object : com.thehouse.app.domain.repository.AuthRepository {
                    private val state = kotlinx.coroutines.flow.MutableStateFlow<com.thehouse.app.domain.model.SessionState>(com.thehouse.app.domain.model.SessionState.Anonymous)
                    override val session = state
                    override suspend fun register(n: String, e: String, p: String, t: Boolean, pr: Boolean) = Result.success(null)
                    override suspend fun login(e: String, p: String) = Result.success(null)
                    override suspend fun logout() = Result.success(Unit)
                    override suspend fun refreshAccessToken() = Result.success(null)
                    override suspend fun getCurrentUser() = Result.success(null)
                    override suspend fun restoreSession() = Result.success(null)
                },
            ),
            sessionState = kotlinx.coroutines.flow.MutableStateFlow(com.thehouse.app.domain.model.SessionState.Anonymous),
            searchRooms = SearchRoomsUseCase(repository),
            selectedRepository = object : com.thehouse.app.domain.repository.SelectedUniversityRepository {
                private val id = kotlinx.coroutines.flow.MutableStateFlow<String?>("6aeecc11f3c9f3f762946293")
                override val selectedUniversityId: kotlinx.coroutines.flow.StateFlow<String?> = id
                override suspend fun loadSelected() { id.value = "6aeecc11f3c9f3f762946293" }
                override suspend fun select(universityId: String) { id.value = universityId }
                override suspend fun clear() { id.value = null }
            },
        )
        // Ejecutar coroutines hasta iniciar la petición; OkHttp sale en hilo propio.
        runCurrent()
        val sent = request()
        assertEquals("/api/v1/rooms/search", sent.url.encodedPath)
        advanceUntilIdle()
        val current = waitForContent(vm)
        assertTrue("state=$current requests=${server.requestCount}", current is RoomSearchState.Content)
        val state = current as RoomSearchState.Content
        assertEquals("room-666f1a2b3c4d5e6f7a8b9c0d", state.items.single().roomId)
    }

    @Test
    fun `explore reaches Content with real Retrofit repository over MockWebServer`() = runTest {
        enqueue("""{"items":[$roomJson],"page":1,"pageSize":20,"totalItems":1,"totalPages":1}""")
        val selected = object : com.thehouse.app.domain.repository.SelectedUniversityRepository {
            private val id = kotlinx.coroutines.flow.MutableStateFlow<String?>("6aeecc11f3c9f3f762946293")
            override val selectedUniversityId: kotlinx.coroutines.flow.StateFlow<String?> = id
            override suspend fun loadSelected() { id.value = "6aeecc11f3c9f3f762946293" }
            override suspend fun select(universityId: String) { id.value = universityId }
            override suspend fun clear() { id.value = null }
        }
        val vm = ExploreViewModel(
            searchRooms = SearchRoomsUseCase(repository),
            selectedRepository = selected,
        )
        runCurrent()
        val sent = request()
        assertEquals("/api/v1/rooms/search", sent.url.encodedPath)
        advanceUntilIdle()
        val current = waitForContent(vm)
        assertTrue("state=$current requests=${server.requestCount}", current is RoomSearchState.Content)
        val state = current as RoomSearchState.Content
        assertEquals("room-666f1a2b3c4d5e6f7a8b9c0d", state.items.single().roomId)
    }
}
