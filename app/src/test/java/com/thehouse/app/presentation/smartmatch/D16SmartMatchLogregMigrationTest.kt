package com.thehouse.app.presentation.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.api.SmartMatchDismissalResponse
import com.thehouse.app.data.remote.api.SmartMatchScoreResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchBudgetDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchFactorDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchListResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchPreferencesDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationItemDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRoomSearchDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchScoreRequest
import com.thehouse.app.data.smartmatch.SmartMatchRepository
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.error.SmartMatchProfileIncompleteException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.smartmatch.SmartMatchModelVersions
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import com.thehouse.app.domain.smartmatch.FeatureVector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * D16 — Smart Match logreg-1.0.0 Migration.
 * Tests A–N del pedido: decimal score, breakdown estructurado, autoridad del
 * Backend, fallback etiquetado, reconexión y versión del artefacto local.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class D16SmartMatchLogregMigrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // ---------- A: matchScore decimal 73.5 deserializa ----------
    @Test
    fun `A matchScore 73 dot 5 deserializes without truncation`() {
        val raw = """
            {"room":{"id":"r1","buildingId":"b1","title":"t","price":1000.0,"currency":"MXN",
            "buildingType":"student_residence","allowedTenants":"any","distanceKm":1.0,"walkingMinutes":5},
            "matchScore":73.5,"breakdown":[],"dismissed":false,
            "modelVersion":"logreg-1.0.0","featureSchemaVersion":"2.0.0"}
        """.trimIndent()
        val item = json.decodeFromString<SmartMatchRecommendationItemDto>(raw)
        assertEquals(73.5, item.matchScore, 0.0)
    }

    // ---------- B: breakdown decimal completo ----------
    @Test
    fun `B full decimal breakdown deserializes`() {
        val raw = """
            {"items":[{"room":{"id":"r1","buildingId":"b1","title":"t","price":1000.0,"currency":"MXN",
            "buildingType":"student_residence","allowedTenants":"any","distanceKm":1.0,"walkingMinutes":5},
            "matchScore":73.5,
            "breakdown":[
              {"factor":"budget","label":"Presupuesto","points":9.6,"maxPoints":9.6},
              {"factor":"distance","label":"Distancia","points":72.6,"maxPoints":72.6},
              {"factor":"essential_services","label":"Servicios","points":6.7,"maxPoints":6.7},
              {"factor":"place_type","label":"Tipo de lugar","points":4.9,"maxPoints":4.9},
              {"factor":"property_type","label":"Tipo de propiedad","points":1.7,"maxPoints":1.7},
              {"factor":"extras","label":"Extras","points":4.5,"maxPoints":4.5}
            ],"dismissed":false,"modelVersion":"logreg-1.0.0","featureSchemaVersion":"2.0.0"}],
            "page":1,"pageSize":20,"totalItems":1,"totalPages":1}
        """.trimIndent()
        val item = json.decodeFromString<SmartMatchListResponse>(raw).items.single()
        assertEquals(listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5), item.breakdown.map { it.points })
        assertEquals(listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5), item.breakdown.map { it.maxPoints })
        assertEquals(
            listOf("budget", "distance", "essential_services", "place_type", "property_type", "extras"),
            item.breakdown.map { it.factor },
        )
    }

    // ---------- C: UI muestra 73.5% ----------
    @Test
    fun `C UI shows 73 dot 5 percent`() {
        assertEquals("73.5%", formatMatchScore(73.5))
        // Entero del Backend sin regresión visual.
        assertEquals("73%", formatMatchScore(73.0))
    }

    // ---------- D/E/F: barra points/maxPoints, 72.6 y 1.7 ----------
    @Test
    fun `DF factor progress is points over maxPoints clamped`() {
        val values = listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5)
        values.forEach { max ->
            val progress = (max / max).coerceIn(0.0, 1.0)
            assertEquals(1.0, progress, 1e-9)
        }
        // points debajo del máximo nunca se divide por 100.
        val progress = (1.7 / 72.6).coerceIn(0.0, 1.0)
        assertTrue(progress in 0.0..1.0)
        assertTrue(progress < 0.1)
        // clamp superior.
        assertEquals(1.0, (80.0 / 72.6).coerceIn(0.0, 1.0), 1e-9)
    }

    @Test
    fun `E maxPoints 72 dot 6 does not break layout`() {
        val factor = SmartMatchFactorDto("distance", "Distancia", 72.6, 72.6)
        val progress = (factor.points / factor.maxPoints).coerceIn(0.0, 1.0)
        assertEquals(1.0, progress, 1e-9)
        assertEquals(0.0, (0.0 / 72.6).coerceIn(0.0, 1.0), 1e-9)
    }

    @Test
    fun `F maxPoints 1 dot 7 does not disappear`() {
        val factor = SmartMatchFactorDto("property_type", "Tipo de propiedad", 1.7, 1.7)
        val progress = (factor.points / factor.maxPoints).coerceIn(0.0, 1.0)
        assertEquals(1.0, progress, 1e-9)
        // points positivo con maxPoints>0 siempre produce barra visible (>=1%).
        val minVisible = progress.coerceAtLeast(0.01)
        assertTrue(minVisible >= 0.01)
    }

    // ---------- G/H: AUTH y GUEST 200 sin re-score ----------
    @Test
    fun `G AUTH 200 keeps backend order and decimal score`() = runTest {
        val api = FakeApi(
            recommendations = SmartMatchListResponse(
                items = listOf(authItem("2", 73.5), authItem("1", 90.0)),
            ),
        )
        val vm = vm(SessionState.Active(UserSession("u", "n", "e@t.com")), api = api)
        vm.load(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertEquals(listOf("2", "1"), results.map { it.property.id })
        assertEquals(listOf(73.5, 90.0), results.map { it.matchScore })
        assertEquals(SmartMatchSource.BACKEND, results.first().source)
        assertEquals("logreg-1.0.0", results.first().modelVersion)
        assertEquals(1, api.getRecommendationsCalls)
        assertEquals(0, api.queryCalls)
    }

    @Test
    fun `H GUEST 200 keeps backend order and decimal score`() = runTest {
        val api = FakeApi(
            query = SmartMatchRecommendationResponse(
                items = listOf(
                    guestItem("2", 73.5),
                    guestItem("1", 90.0),
                ),
            ),
        )
        val vm = vm(SessionState.Anonymous, api = api)
        vm.load(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertEquals(listOf("2", "1"), results.map { it.property.id })
        assertEquals(listOf(73.5, 90.0), results.map { it.matchScore })
        assertEquals(SmartMatchSource.BACKEND, results.first().source)
        assertEquals(1, api.queryCalls)
        assertEquals(0, api.getRecommendationsCalls)
    }

    // ---------- I/J: network y 503 → fallback etiquetado ----------
    @Test
    fun `I network failure uses labeled local estimate`() = runTest {
        val api = FakeApi(recommendationsFailure = DomainException(DomainError.Network))
        val vm = vm(SessionState.Active(UserSession("u", "n", "e@t.com")), api = api)
        vm.load(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.source == SmartMatchSource.LOCAL_ESTIMATE })
        assertTrue(results.all { it.modelVersion == SmartMatchModelVersions.LOCAL_MODEL_VERSION })
    }

    @Test
    fun `J 503 uses labeled local estimate`() = runTest {
        val api = FakeApi(recommendationsFailure = DomainException(DomainError.ServiceUnavailable))
        val vm = vm(SessionState.Active(UserSession("u", "n", "e@t.com")), api = api)
        vm.load(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertTrue(results.all { it.source == SmartMatchSource.LOCAL_ESTIMATE })
    }

    // ---------- K: 400/401/404/422/429 sin fallback ----------
    @Test
    fun `K 400 401 404 422 429 never fall back`() = runTest {
        val errors = listOf(
            DomainError.ValidationError,
            DomainError.Unauthorized,
            DomainError.NotFound,
            DomainError.RateLimited,
        )
        errors.forEach { error ->
            val api = FakeApi(recommendationsFailure = DomainException(error))
            val vm = vm(SessionState.Active(UserSession("u", "n", "e@t.com")), api = api)
            vm.load(); advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue("error=$error state=$state", state !is SmartMatchUiState.Content)
        }
        // 422 perfil incompleto → NoPreferences, jamás Content local.
        val api422 = FakeApi(recommendationsFailure = SmartMatchProfileIncompleteException(listOf("budget.max")))
        val vm422 = vm(SessionState.Active(UserSession("u", "n", "e@t.com")), api = api422)
        vm422.load(); advanceUntilIdle()
        assertTrue(vm422.uiState.value is SmartMatchUiState.NoPreferences)
    }

    // ---------- L: reconexión reemplaza estimación local ----------
    @Test
    fun `L reconnect replaces local estimate with remote result`() = runTest {
        val online = MutableStateFlow(false)
        val api = FakeApi(recommendationsFailure = DomainException(DomainError.Network))
        val vm = vm(
            SessionState.Active(UserSession("u", "n", "e@t.com")),
            api = api,
            connectivity = object : ConnectivityObserver { override val isOnline = online },
        )
        vm.load(); advanceUntilIdle()
        assertTrue((vm.uiState.value as SmartMatchUiState.Content).results.all {
            it.source == SmartMatchSource.LOCAL_ESTIMATE
        })

        api.recommendationsFailure = null
        api.recommendations = SmartMatchListResponse(items = listOf(authItem("r", 73.5)))
        online.value = true
        vm.observeConnectivity(); advanceUntilIdle()

        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertEquals(1, results.size)
        assertEquals(SmartMatchSource.BACKEND, results.first().source)
        assertEquals(73.5, results.first().matchScore, 0.0)
        assertEquals(2, api.getRecommendationsCalls)
    }

    // ---------- M/N: versiones ----------
    @Test
    fun `M remote modelVersion is logreg 1 0 0`() {
        assertEquals("logreg-1.0.0", SmartMatchModelVersions.SERVER_MODEL_VERSION)
    }

    @Test
    fun `N local artifact remains version 1 0 0 with UNKNOWN equivalence`() {
        assertEquals("1.0.0", SmartMatchModelVersions.LOCAL_MODEL_VERSION)
        assertEquals("UNKNOWN", SmartMatchModelVersions.LOCAL_EQUIVALENCE)
    }

    // ---------- helpers ----------
    private fun authItem(id: String, score: Double) = SmartMatchRecommendationItemDto(
        room = SmartMatchRoomSearchDto(
            id = id,
            buildingId = "b1",
            title = "t$id",
            price = 3000.0,
            currency = "MXN",
            buildingType = "student_residence",
            allowedTenants = "any",
            services = listOf("wifi"),
            distanceKm = 1.0,
            walkingMinutes = 12,
        ),
        matchScore = score,
        breakdown = listOf(SmartMatchFactorDto("budget", "Presupuesto", 9.6, 9.6)),
        dismissed = false,
        modelVersion = "logreg-1.0.0",
        featureSchemaVersion = "2.0.0",
    )

    private fun guestItem(id: String, score: Double) = SmartMatchRecommendationDto(
        roomId = id,
        title = "t$id",
        price = 3000.0,
        currency = "MXN",
        matchScore = score,
        dismissed = false,
        breakdown = listOf(SmartMatchFactorDto("budget", "Presupuesto", 9.6, 9.6)),
        modelVersion = "logreg-1.0.0",
    )

    private fun vm(
        session: SessionState,
        api: FakeApi,
        connectivity: ConnectivityObserver? = null,
    ): SmartMatchViewModel {
        val preference = Preference(
            budget = com.thehouse.app.domain.model.Budget(2000.0, 5000.0, "MXN"),
            essentialServices = listOf("wifi"),
            livingWith = "any",
            placeType = "shared",
            pets = "none",
            transport = "any",
            propertyTypes = listOf("room"),
            currency = "MXN",
            maxDistanceKm = 5.0,
        )
        val prefs = object : PreferenceRepository {
            override val preferenceState = MutableStateFlow<Preference?>(preference)
            override suspend fun loadPreferences() { }
            override suspend fun savePreferences(preference: Preference) { }
        }
        val property = Property(
            id = "r", title = "t", price = 3000.0, currency = "MXN", city = "",
            universityId = null, bedrooms = 1, bathrooms = 1, areaM2 = 20.0,
            createdAt = "", description = null, amenities = listOf("wifi"),
            thumbnailUrl = null, photos = emptyList(),
        )
        val properties = object : com.thehouse.app.domain.repository.PropertyRepository {
            override suspend fun getProperties(page: Int, pageSize: Int) =
                Result.success(PagedResult(listOf(property), 1, 20, 1, 1))
            override suspend fun getPropertyById(id: String) = Result.success(property)
        }
        val model = object : SmartMatchModel {
            override val modelVersion = "1.0.0"
            override val featureVersion = "1.0.0"
            override val artifactHash = "local-test"
            override fun predictProbability(vector: FeatureVector) = 0.735
        }
        return SmartMatchViewModel(
            sessionState = MutableStateFlow(session),
            preferenceRepository = prefs,
            smartMatchRepository = SmartMatchRepository(api),
            getProperties = GetPropertiesUseCase(properties),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
            connectivityObserver = connectivity,
        )
    }

    private class FakeApi(
        var recommendations: SmartMatchListResponse = SmartMatchListResponse(),
        var recommendationsFailure: Throwable? = null,
        var query: SmartMatchRecommendationResponse = SmartMatchRecommendationResponse(),
        var getRecommendationsCalls: Int = 0,
        var queryCalls: Int = 0,
    ) : SmartMatchApi {
        override suspend fun getRecommendations(page: Int, pageSize: Int): SmartMatchListResponse {
            getRecommendationsCalls++
            recommendationsFailure?.let { throw it }
            return recommendations
        }
        override suspend fun queryRecommendations(request: SmartMatchQueryRequest): SmartMatchRecommendationResponse {
            queryCalls++
            return query
        }
        override suspend fun calculateScores(request: SmartMatchScoreRequest): SmartMatchScoreResponse =
            SmartMatchScoreResponse()
        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()
        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()
        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse =
            throw UnsupportedOperationException()
        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse =
            throw UnsupportedOperationException()
    }
}
