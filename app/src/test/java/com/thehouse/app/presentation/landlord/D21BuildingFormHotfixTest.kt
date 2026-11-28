package com.thehouse.app.presentation.landlord

import com.thehouse.app.data.remote.api.LandlordApi
import com.thehouse.app.data.remote.dto.landlord.BuildingDto
import com.thehouse.app.data.landlord.repository.LandlordRepositoryImpl
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.location.DeviceLocationProvider
import com.thehouse.app.domain.location.LocationResult
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.NearbyUniversity
import com.thehouse.app.domain.usecase.CreateBuildingUseCase
import com.thehouse.app.domain.usecase.CreateRequestUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.GetUserRequestsUseCase
import com.thehouse.app.domain.usecase.SearchNearbyUniversitiesUseCase
import com.thehouse.app.domain.usecase.UpdateBuildingUseCase
import com.thehouse.app.domain.model.PagedBuildings
import com.thehouse.app.domain.model.RequestRoom
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository
import com.thehouse.app.presentation.requests.CreateRequestEffect
import com.thehouse.app.presentation.requests.CreateRequestEvent
import com.thehouse.app.presentation.requests.CreateRequestViewModel
import com.thehouse.app.presentation.requests.RequestErrorMessages
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class D21BuildingFormHotfixTest {

    private val dispatcher = StandardTestDispatcher()
    @get:Rule val mainRule = TestMainDispatcherRule(dispatcher)

    private lateinit var server: MockWebServer
    private lateinit var repository: LandlordRepositoryImpl

    private val catalog = PagedResult(
        items = listOf(
            University("uaeh", "UAEH", "UAEH Este", "Pachuca", "MX", "2026-01-01T00:00:00Z"),
        ),
        page = 1, pageSize = 20, totalItems = 1, totalPages = 1,
    )
    private val nearby = listOf(NearbyUniversity("uaeh", "UAEH", "UAEH Este", "Pachuca", 0.4))

    private val buildingJson = """
        {"id":"b1","type":"rooms_building","name":"Edificio Centro",
         "address":{"street":"Av Juárez","number":"12","neighborhood":"Centro","city":"Pachuca","state":"Hidalgo","postalCode":"43600"},
         "location":{"lat":19.9185,"lng":-99.3645},"nearestUniversity":{"id":"uaeh","code":"UAEH","name":"UAEH Este"},
         "distanceToUniversityKm":0.4,"walkingMinutes":5,"allowedTenants":"any","services":[],
         "rules":null,"status":"draft","version":1,"roomsTotal":0,"roomsAvailable":0,
         "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
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
        repository = LandlordRepositoryImpl(retrofit.create(LandlordApi::class.java), json)
    }

    @After
    fun tearDown() = server.close()

    private fun jsonResponse(code: Int, raw: String) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(raw).build()

    private fun vm(repo: com.thehouse.app.domain.repository.LandlordRepository = repository): BuildingFormViewModel {
        val universityRepo = object : com.thehouse.app.domain.repository.UniversityRepository {
            override suspend fun getUniversities(page: Int, pageSize: Int) = Result.success(catalog)
            override suspend fun getUniversityById(id: String) = error("no usado")
            override suspend fun searchNearbyUniversities(lat: Double, lng: Double) = Result.success(nearby)
        }
        return BuildingFormViewModel(
            buildingId = null,
            createBuilding = CreateBuildingUseCase(repo),
            updateBuilding = UpdateBuildingUseCase(repo),
            getBuilding = GetLandlordBuildingUseCase(repo),
            getUniversities = GetUniversitiesUseCase(universityRepo),
            searchNearbyUniversities = SearchNearbyUniversitiesUseCase(universityRepo),
            deviceLocationProvider = object : DeviceLocationProvider {
                override suspend fun currentLocation(
                    fineGranted: Boolean, coarseGranted: Boolean, timeoutMillis: Long,
                ): LocationResult = LocationResult.Unavailable
            },
        )
    }

    /** Formulario completo y válido (todas las reglas del Backend). */
    private fun fillValid(vm: BuildingFormViewModel) {
        vm.onEvent(BuildingFormEvent.NameChanged("Edificio Centro"))
        vm.onEvent(BuildingFormEvent.StreetChanged("Av Juárez"))
        vm.onEvent(BuildingFormEvent.NumberChanged("12"))
        vm.onEvent(BuildingFormEvent.NeighborhoodChanged("Centro"))
        vm.onEvent(BuildingFormEvent.CityChanged("Pachuca"))
        vm.onEvent(BuildingFormEvent.StateChanged("Hidalgo"))
        vm.onEvent(BuildingFormEvent.PostalCodeChanged("43600"))
        vm.onEvent(BuildingFormEvent.LatChanged("19.9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("-99.3645"))
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `complete valid form has no pending fields and is valid`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        fillValid(vm)
        assertTrue(vm.uiState.value.isValid)
        assertTrue(vm.uiState.value.validationErrors.isEmpty())
        assertEquals("uaeh", vm.uiState.value.universityId)
    }

    @Test
    fun `each missing required field is reported with a Spanish message`() = runTest {
        val cases = listOf(
            BuildingFormValidation.FIELD_NAME,
            BuildingFormValidation.FIELD_STREET,
            BuildingFormValidation.FIELD_NUMBER,
            BuildingFormValidation.FIELD_NEIGHBORHOOD,
            BuildingFormValidation.FIELD_CITY,
            BuildingFormValidation.FIELD_STATE,
            BuildingFormValidation.FIELD_POSTAL_CODE,
            BuildingFormValidation.FIELD_LAT,
            BuildingFormValidation.FIELD_LNG,
            BuildingFormValidation.FIELD_UNIVERSITY,
        )
        val errors = BuildingFormValidation.errors(
            name = "", street = "", number = "", neighborhood = "",
            city = "", state = "", postalCode = "", lat = "", lng = "",
            universityId = "", rules = "",
        )
        assertEquals(cases.toSet(), errors.keys)
        assertTrue(errors.values.all { it.isNotBlank() })
        assertTrue(errors[BuildingFormValidation.FIELD_NAME]!!.contains("Nombre"))
        assertTrue(errors[BuildingFormValidation.FIELD_POSTAL_CODE]!!.contains("C.P."))
    }

    @Test
    fun `postal code with outer spaces is normalized and accepted`() = runTest {
        val errors = BuildingFormValidation.errors(
            name = "Edificio Centro", street = "Av Juárez", number = "12",
            neighborhood = "Centro", city = "Pachuca", state = "Hidalgo",
            postalCode = "  43600  ", lat = "19.9185", lng = "-99.3645",
            universityId = "uaeh", rules = "",
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `submit with invalid form reports all fields and performs no HTTP request`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.submitAttempted)
        assertFalse(state.isValid)
        assertTrue(state.pendingFields.size >= 9)
        assertNotNull(state.firstInvalidField)
        assertEquals(0, server.requestCount)
        assertFalse(state.isSaving)
    }

    @Test
    fun `invalid submit preserves all typed data`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.NameChanged("Mi edificio"))
        vm.onEvent(BuildingFormEvent.PostalCodeChanged("123"))
        vm.onEvent(BuildingFormEvent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Mi edificio", vm.uiState.value.name)
        assertEquals("123", vm.uiState.value.postalCode)
    }

    @Test
    fun `empty nearby result does not clear a valid university selection`() = runTest {
        // Se simuló una búsqueda previa que preseleccionó "uaeh"; una respuesta
        // vacía posterior no debe dejar universityId en blanco.
        val errors = BuildingFormValidation.errors(
            name = "Edificio Centro", street = "Av Juárez", number = "12",
            neighborhood = "Centro", city = "Pachuca", state = "Hidalgo",
            postalCode = "43600", lat = "19.9185", lng = "-99.3645",
            universityId = "uaeh", rules = "",
        )
        assertTrue(errors.isEmpty())
        val cleared = BuildingFormValidation.errors(
            name = "Edificio Centro", street = "Av Juárez", number = "12",
            neighborhood = "Centro", city = "Pachuca", state = "Hidalgo",
            postalCode = "43600", lat = "19.9185", lng = "-99.3645",
            universityId = "", rules = "",
        )
        assertEquals(BuildingFormValidation.FIELD_UNIVERSITY, cleared.keys.first())
    }

    @Test
    fun `manual university selection survives a new nearby search`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.LatChanged("19.9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("-99.3645"))
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.UniversityChanged("uaeh"))
        vm.onEvent(BuildingFormEvent.RetryNearbySearch)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("uaeh", vm.uiState.value.universityId)
    }

    @Test
    fun `repository posts valid building with trimmed payload and gets 201`() = runTest {
        server.enqueue(jsonResponse(201, buildingJson))
        // El ViewModel normaliza con trim() antes de llamar al repositorio.
        val result = repository.createBuilding(
            type = BuildingType.ROOMS_BUILDING,
            name = "Edificio Centro",
            address = com.thehouse.app.domain.model.BuildingAddress(
                street = "Av Juárez", number = "12", neighborhood = "Centro",
                city = "Pachuca", state = "Hidalgo", postalCode = "43600",
            ),
            location = com.thehouse.app.domain.model.BuildingLocation(19.9185, -99.3645),
            nearestUniversityId = "uaeh",
            allowedTenants = AllowedTenants.ANY,
            services = emptyList(),
            rules = null,
        )
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/landlord/buildings", recorded.url.encodedPath)
        val body = recorded.body!!.utf8()
        assertTrue(body.contains("\"name\":\"Edificio Centro\""))
        assertTrue(body.contains("\"postalCode\":\"43600\""))
        assertTrue(body.contains("\"lat\":19.9185"))
        assertTrue(body.contains("\"lng\":-99.3645"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `double submit performs exactly one domain call`() = runTest {
        val counting = CountingCreateRepository()
        val vm = vm(counting)
        dispatcher.scheduler.advanceUntilIdle()
        fillValid(vm)
        vm.onEvent(BuildingFormEvent.Submit)
        vm.onEvent(BuildingFormEvent.Submit)
        vm.onEvent(BuildingFormEvent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, counting.createCalls)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `network failure releases isSaving and keeps the form`() = runTest {
        val failing = object : com.thehouse.app.domain.repository.LandlordRepository by repository {
            override suspend fun createBuilding(
                type: BuildingType, name: String, address: com.thehouse.app.domain.model.BuildingAddress,
                location: com.thehouse.app.domain.model.BuildingLocation, nearestUniversityId: String,
                allowedTenants: AllowedTenants, services: List<Service>, rules: String?,
            ): Result<com.thehouse.app.domain.model.Building> =
                Result.failure(java.net.UnknownHostException("sin red"))
        }
        val vm = vm(failing)
        dispatcher.scheduler.advanceUntilIdle()
        fillValid(vm)
        vm.onEvent(BuildingFormEvent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSaving)
        assertEquals("Edificio Centro", vm.uiState.value.name)
        assertTrue(vm.uiState.value.errorMessage != null)
    }

    // --- 409 de solicitudes ---

    @Test
    fun `all three 409 codes are mapped to specific Spanish messages`() {
        assertEquals(
            "Ya tienes una solicitud activa para este cuarto.",
            RequestErrorMessages.map(RequestErrorCode.ACTIVE_REQUEST_EXISTS),
        )
        assertEquals(
            "Hay una solicitud en proceso. Espera un momento e intenta de nuevo.",
            RequestErrorMessages.map(RequestErrorCode.REQUEST_IN_PROGRESS),
        )
        assertEquals(
            "Llegaste al límite de solicitudes activas.",
            RequestErrorMessages.map(RequestErrorCode.ACTIVE_REQUESTS_LIMIT),
        )
        assertEquals(RequestErrorCode.REQUEST_IN_PROGRESS, RequestErrorCode.fromApi("request_in_progress"))
    }

    @Test
    fun `active request 409 recovers real conversation id from user requests`() = runTest {
        val existing = RoomRequest(
            id = "req-existing", status = RequestStatus.PENDING,
            room = RequestRoom("room-1", "b-1", "Cuarto", "Pachuca"),
            quickQuestions = listOf(QuickQuestion.DEPOSIT), message = null,
            student = null, landlord = null, visit = null,
            conversationId = "conv REAL 123", rejection = null,
            roomAvailable = true, createdAt = null, updatedAt = null,
        )
        val repo = object : RequestsRepository {
            var createCalls = 0
            override suspend fun createRequest(
                roomId: String, quickQuestions: List<QuickQuestion>, message: String?,
            ): Result<RoomRequest> {
                createCalls++
                return Result.failure(
                    RequestException(DomainError.Conflict, RequestErrorCode.ACTIVE_REQUEST_EXISTS),
                )
            }
            override suspend fun getRequests(status: RequestStatus?, page: Int, pageSize: Int) =
                Result.success(PagedResult(listOf(existing), 1, 20, 1, 1))
            override suspend fun getRequest(id: String) = Result.success(existing)
            override suspend fun cancelRequest(id: String) = Result.success(existing)
            override suspend fun getLandlordRequests(
                status: RequestStatus?, roomId: String?, page: Int, pageSize: Int,
            ) = Result.success(PagedResult(listOf(existing), 1, 20, 1, 1))
            override suspend fun getLandlordRequest(id: String) = Result.success(existing)
            override suspend fun acceptRequest(id: String) = Result.success(existing)
            override suspend fun rejectRequest(id: String, reason: String, note: String?) = Result.success(existing)
        }
        val vm = CreateRequestViewModel(
            roomId = "room-1",
            createRequest = CreateRequestUseCase(repo),
            getUserRequests = GetUserRequestsUseCase(repo),
        )
        vm.onEvent(CreateRequestEvent.QuickQuestionToggled(QuickQuestion.DEPOSIT))
        vm.onEvent(CreateRequestEvent.Send)
        runCurrent()
        assertEquals(1, repo.createCalls)
        assertEquals("conv REAL 123", vm.uiState.value.existingConversationId)

        var opened: String? = null
        val job = launch {
            vm.effects.collect { effect ->
                if (effect is CreateRequestEffect.OpenConversation) opened = effect.conversationId
            }
        }
        vm.onEvent(CreateRequestEvent.OpenExistingConversation)
        runCurrent()
        assertEquals("conv REAL 123", opened)
        job.cancel()
    }
}



/** Repositorio falso sin red que cuenta cuántas veces se crea un edificio. */
private class CountingCreateRepository : com.thehouse.app.domain.repository.LandlordRepository {
    var createCalls = 0

    override suspend fun createBuilding(
        type: BuildingType, name: String, address: com.thehouse.app.domain.model.BuildingAddress,
        location: com.thehouse.app.domain.model.BuildingLocation, nearestUniversityId: String,
        allowedTenants: AllowedTenants, services: List<Service>, rules: String?,
    ): Result<com.thehouse.app.domain.model.Building> {
        createCalls++
        return Result.success(sampleBuilding())
    }

    override suspend fun getBuildings(page: Int, pageSize: Int) =
        Result.success(PagedBuildings(emptyList(), page, pageSize, 0, 0, null))
    override suspend fun getBuilding(id: String) = Result.success(sampleBuilding())
    override suspend fun updateBuilding(
        id: String, version: Int, patch: com.thehouse.app.domain.model.BuildingPatch,
    ) = Result.success(sampleBuilding())

    override suspend fun createProfile(
        fullName: String, phone: String, rentalMode: com.thehouse.app.domain.model.RentalMode,
        rfc: String?, acceptedTermsVersion: String,
    ) = error("no usado")
    override suspend fun getProfile() = error("no usado")
    override suspend fun deleteBuilding(id: String, version: Int) = error("no usado")
    override suspend fun generateRooms(
        buildingId: String, floors: Int, roomsPerFloor: List<Int>, base: com.thehouse.app.domain.model.RoomBase,
    ) = error("no usado")
    override suspend fun getRooms(buildingId: String, page: Int, pageSize: Int) = error("no usado")
    override suspend fun updateRoom(
        id: String, version: Int, patch: com.thehouse.app.domain.model.RoomPatch,
    ) = error("no usado")
    override suspend fun deleteRoom(id: String, version: Int) = error("no usado")
    override suspend fun duplicateRoom(id: String) = error("no usado")
    override suspend fun copyFromRoom(id: String, version: Int, sourceRoomId: String) = error("no usado")
    override suspend fun pauseRoom(id: String) = error("no usado")
    override suspend fun resumeRoom(id: String) = error("no usado")
    override suspend fun getPublication(buildingId: String) = error("no usado")
    override suspend fun publishBuilding(id: String) = error("no usado")
    override suspend fun uploadBuildingPhoto(buildingId: String, file: java.io.File, mimeType: String) = error("no usado")
    override suspend fun deleteBuildingPhoto(buildingId: String) = error("no usado")
    override suspend fun uploadRoomPhoto(roomId: String, file: java.io.File, mimeType: String) = error("no usado")
    override suspend fun deleteRoomPhoto(roomId: String, photoId: String) = error("no usado")
    override suspend fun uploadIneDocument(buildingId: String, file: java.io.File, mimeType: String) = error("no usado")
    override suspend fun uploadPropertyProofDocument(buildingId: String, file: java.io.File, mimeType: String) = error("no usado")
    override suspend fun getBuildingDocuments(buildingId: String) = error("no usado")
    override suspend fun getLandlordStatsRooms(
        from: String, to: String, buildingId: String?, page: Int, pageSize: Int,
    ) = error("no usado")

    private fun sampleBuilding() = com.thehouse.app.domain.model.Building(
        id = "b1", type = BuildingType.ROOMS_BUILDING, name = "Edificio Centro",
        address = com.thehouse.app.domain.model.BuildingAddress(
            "Av Juárez", "12", "Centro", "Pachuca", "Hidalgo", "43600",
        ),
        location = com.thehouse.app.domain.model.BuildingLocation(19.9185, -99.3645),
        nearestUniversity = com.thehouse.app.domain.model.NearestUniversity("uaeh", "UAEH", "UAEH Este"),
        distanceToUniversityKm = 0.4, walkingMinutes = 5, allowedTenants = AllowedTenants.ANY,
        services = emptyList(), rules = null, status = com.thehouse.app.domain.model.BuildingStatus.DRAFT,
        roomsTotal = 0, roomsAvailable = 0, version = 1, createdAt = null, updatedAt = null,
    )
}
