package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.location.DeviceLocation
import com.thehouse.app.domain.location.DeviceLocationProvider
import com.thehouse.app.domain.location.LocationResult
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.repository.NearbyUniversity
import com.thehouse.app.domain.usecase.CreateBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SearchNearbyUniversitiesUseCase
import com.thehouse.app.domain.usecase.UpdateBuildingUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildingFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @get:Rule val mainRule = TestMainDispatcherRule(dispatcher)

    private val nearby = listOf(
        NearbyUniversity("uaeh", "UAEH", "UAEH Este", "Pachuca", 0.4),
        NearbyUniversity("uttt", "UTTT", "UTTT", "Tula de Allende", 12.3),
    )

    private val catalog = PagedResult(
        items = listOf(
            com.thehouse.app.domain.model.University(
                id = "other", code = "OTRA", name = "Otra universidad", city = "Pachuca",
                country = "MX", createdAt = "2026-01-01T00:00:00Z",
            ),
        ),
        page = 1, pageSize = 20, totalItems = 1, totalPages = 1,
    )

    private var nearbyCalls = 0
    private var lastNearby: Pair<Double, Double>? = null
    private var locationResult: LocationResult = LocationResult.Unavailable

    private fun viewModel(): BuildingFormViewModel {
        nearbyCalls = 0
        lastNearby = null
        val getUniversities = GetUniversitiesUseCase(object : com.thehouse.app.domain.repository.UniversityRepository {
            override suspend fun getUniversities(page: Int, pageSize: Int) = Result.success(catalog)
            override suspend fun getUniversityById(id: String) = error("no usado")
            override suspend fun searchNearbyUniversities(lat: Double, lng: Double): Result<List<NearbyUniversity>> {
                nearbyCalls++
                lastNearby = lat to lng
                return Result.success(nearby)
            }
        })
        return BuildingFormViewModel(
            buildingId = null,
            createBuilding = CreateBuildingUseCase(BuildingFormFakeLandlordRepository()),
            updateBuilding = UpdateBuildingUseCase(BuildingFormFakeLandlordRepository()),
            getBuilding = GetLandlordBuildingUseCase(BuildingFormFakeLandlordRepository()),
            getUniversities = getUniversities,
            searchNearbyUniversities = SearchNearbyUniversitiesUseCase(
                object : com.thehouse.app.domain.repository.UniversityRepository {
                    override suspend fun getUniversities(page: Int, pageSize: Int) = Result.success(catalog)
                    override suspend fun getUniversityById(id: String) = error("no usado")
                    override suspend fun searchNearbyUniversities(lat: Double, lng: Double): Result<List<NearbyUniversity>> {
                        nearbyCalls++
                        lastNearby = lat to lng
                        return Result.success(nearby)
                    }
                },
            ),
            deviceLocationProvider = object : DeviceLocationProvider {
                override suspend fun currentLocation(
                    fineGranted: Boolean, coarseGranted: Boolean, timeoutMillis: Long,
                ): LocationResult = locationResult
            },
        )
    }

    @Test
    fun `valid coordinates from report are accepted without errors`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.LatChanged("19.9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("-99.3645"))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.latError)
        assertNull(vm.uiState.value.lngError)
        assertTrue(vm.uiState.value.latTouched && vm.uiState.value.lngTouched)
    }

    @Test
    fun `comma decimal and unicode minus are normalized before conversion`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.LatChanged("19,9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("\u221299.3645"))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.latError)
        assertNull(vm.uiState.value.lngError)
        assertEquals("19,9185", vm.uiState.value.lat) // texto original conservado
        assertEquals(19.9185 to -99.3645, lastNearby)
    }

    @Test
    fun `empty fields do not show errors before interaction`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.showLatError)
        assertNull(vm.uiState.value.latError)
        assertNull(vm.uiState.value.lngError)
        vm.onEvent(BuildingFormEvent.LatChanged(""))
        assertNull(vm.uiState.value.latError) // todavía en edición, sin pérdida de foco
        vm.onEvent(BuildingFormEvent.LatFocusLost(""))
        assertTrue(vm.uiState.value.latError!!.contains("Escribe la latitud"))
    }

    @Test
    fun `nearby search is debounced and cancellable`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.LatChanged("19.9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("-99.3645"))
        advanceTimeBy(300)
        assertEquals(0, nearbyCalls)
        advanceTimeBy(250)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, nearbyCalls)
        assertEquals("uaeh", vm.uiState.value.universityId)
        assertEquals(0.4, vm.uiState.value.nearbyUniversities.first().distanceKm!!, 0.0)
    }

    @Test
    fun `manual university selection is not overwritten by recomposition`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.LatChanged("19.9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("-99.3645"))
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.UniversityChanged("uttt"))
        vm.onEvent(BuildingFormEvent.RetryNearbySearch)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("uttt", vm.uiState.value.universityId)
    }

    @Test
    fun `gps success fills coordinates and asks for confirmation`() = runTest {
        locationResult = LocationResult.Success(
            DeviceLocation(19.9185, -99.3645, accuracyMeters = 5f),
        )
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.UseCurrentLocationClicked)
        vm.onEvent(BuildingFormEvent.LocationPermissionResult(true, false))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("19.9185", vm.uiState.value.lat)
        assertEquals("-99.3645", vm.uiState.value.lng)
        assertTrue(vm.uiState.value.locationMessage!!.contains("Confirma"))
        assertFalse(vm.uiState.value.approximateLocation)
        assertEquals(1, nearbyCalls)
    }

    @Test
    fun `denied permission keeps form usable`() = runTest {
        locationResult = LocationResult.PermissionDenied
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.UseCurrentLocationClicked)
        vm.onEvent(BuildingFormEvent.LocationPermissionResult(false, false))
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isLocating)
        assertTrue(vm.uiState.value.locationMessage!!.contains("manualmente"))
        assertFalse(vm.uiState.value.isValid)
    }

    @Test
    fun `submit sends numeric lat and lng and never a string`() = runTest {
        val repo = CapturingLandlordRepository()
        val vm = BuildingFormViewModel(
            buildingId = null,
            createBuilding = CreateBuildingUseCase(repo),
            updateBuilding = UpdateBuildingUseCase(repo),
            getBuilding = GetLandlordBuildingUseCase(repo),
            getUniversities = GetUniversitiesUseCase(object : com.thehouse.app.domain.repository.UniversityRepository {
                override suspend fun getUniversities(page: Int, pageSize: Int) = Result.success(catalog)
                override suspend fun getUniversityById(id: String) = error("no usado")
                override suspend fun searchNearbyUniversities(lat: Double, lng: Double) = Result.success(nearby)
            }),
            searchNearbyUniversities = SearchNearbyUniversitiesUseCase(object : com.thehouse.app.domain.repository.UniversityRepository {
                override suspend fun getUniversities(page: Int, pageSize: Int) = Result.success(catalog)
                override suspend fun getUniversityById(id: String) = error("no usado")
                override suspend fun searchNearbyUniversities(lat: Double, lng: Double) = Result.success(nearby)
            }),
            deviceLocationProvider = object : DeviceLocationProvider {
                override suspend fun currentLocation(fineGranted: Boolean, coarseGranted: Boolean, timeoutMillis: Long) = locationResult
            },
        )
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.NameChanged("Edificio Centro"))
        vm.onEvent(BuildingFormEvent.StreetChanged("Av Juárez"))
        vm.onEvent(BuildingFormEvent.CityChanged("Pachuca"))
        vm.onEvent(BuildingFormEvent.StateChanged("Hidalgo"))
        vm.onEvent(BuildingFormEvent.PostalCodeChanged("43600"))
        vm.onEvent(BuildingFormEvent.LatChanged("19,9185"))
        vm.onEvent(BuildingFormEvent.LngChanged("\u221299.3645"))
        dispatcher.scheduler.advanceUntilIdle()
        vm.onEvent(BuildingFormEvent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(19.9185, repo.createLat!!, 0.0)
        assertEquals(-99.3645, repo.createLng!!, 0.0)
        assertEquals("uaeh", repo.createUniversityId)
    }
}

/** Captura los argumentos reales enviados al repositorio de dominio. */
private class CapturingLandlordRepository : BuildingFormFakeLandlordRepository() {
    var createLat: Double? = null
    var createLng: Double? = null
    var createUniversityId: String? = null

    override suspend fun createBuilding(
        type: BuildingType,
        name: String,
        address: BuildingAddress,
        location: BuildingLocation,
        nearestUniversityId: String,
        allowedTenants: AllowedTenants,
        services: List<Service>,
        rules: String?,
    ): Result<Building> {
        createLat = location.lat
        createLng = location.lng
        createUniversityId = nearestUniversityId
        return super.createBuilding(type, name, address, location, nearestUniversityId, allowedTenants, services, rules)
    }
}

/** Repositorio falso (sin red). */
private open class BuildingFormFakeLandlordRepository : com.thehouse.app.domain.repository.LandlordRepository {

    override suspend fun createProfile(
        fullName: String, phone: String, rentalMode: com.thehouse.app.domain.model.RentalMode,
        rfc: String?, acceptedTermsVersion: String,
    ) = error("no usado")

    override suspend fun getProfile() = error("no usado")

    override suspend fun createBuilding(
        type: BuildingType,
        name: String,
        address: BuildingAddress,
        location: BuildingLocation,
        nearestUniversityId: String,
        allowedTenants: AllowedTenants,
        services: List<Service>,
        rules: String?,
    ): Result<Building> {
        // Los tests que necesitan capturar usan su propia instancia; ver abajo.
        return Result.success(building())
    }

    override suspend fun getBuildings(page: Int, pageSize: Int) = Result.success(
        com.thehouse.app.domain.model.PagedBuildings(emptyList(), page, pageSize, 0, 0, null),
    )

    override suspend fun getBuilding(id: String) = Result.success(building())
    override suspend fun updateBuilding(id: String, version: Int, patch: com.thehouse.app.domain.model.BuildingPatch) = Result.success(building())

    private fun building() = Building(
        id = "b1", type = BuildingType.ROOMS_BUILDING, name = "Edificio Centro",
        address = BuildingAddress("Av Juárez", "12", "Centro", "Pachuca", "Hidalgo", "43600"),
        location = BuildingLocation(19.9185, -99.3645),
        nearestUniversity = com.thehouse.app.domain.model.NearestUniversity("uaeh", "UAEH", "UAEH Este"),
        distanceToUniversityKm = 0.4, walkingMinutes = 5, allowedTenants = AllowedTenants.ANY,
        services = emptyList(), rules = null, status = BuildingStatus.DRAFT,
        roomsTotal = 0, roomsAvailable = 0, version = 1,
        createdAt = null, updatedAt = null,
    )

    override suspend fun deleteBuilding(id: String, version: Int) = Result.success(Unit)
    override suspend fun generateRooms(buildingId: String, floors: Int, roomsPerFloor: List<Int>, base: com.thehouse.app.domain.model.RoomBase) = error("no usado")
    override suspend fun getRooms(buildingId: String, page: Int, pageSize: Int) = error("no usado")
    override suspend fun updateRoom(id: String, version: Int, patch: com.thehouse.app.domain.model.RoomPatch) = error("no usado")
    override suspend fun deleteRoom(id: String, version: Int) = error("no usado")
    override suspend fun duplicateRoom(id: String) = error("no usado")
    override suspend fun copyFromRoom(id: String, version: Int, sourceRoomId: String) = error("no usado")
    override suspend fun pauseRoom(id: String) = error("no usado")
    override suspend fun resumeRoom(id: String) = error("no usado")
    override suspend fun getPublication(buildingId: String) = error("no usado")
    override suspend fun publishBuilding(id: String) = error("no usado")
    override suspend fun uploadBuildingPhoto(buildingId: String, file: File, mimeType: String) = error("no usado")
    override suspend fun deleteBuildingPhoto(buildingId: String) = error("no usado")
    override suspend fun uploadRoomPhoto(roomId: String, file: File, mimeType: String) = error("no usado")
    override suspend fun deleteRoomPhoto(roomId: String, photoId: String) = error("no usado")
    override suspend fun uploadIneDocument(buildingId: String, file: File, mimeType: String) = error("no usado")
    override suspend fun uploadPropertyProofDocument(buildingId: String, file: File, mimeType: String) = error("no usado")
    override suspend fun getBuildingDocuments(buildingId: String) = error("no usado")
    override suspend fun getLandlordStatsRooms(from: String, to: String, buildingId: String?, page: Int, pageSize: Int) = error("no usado")
}
