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
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.smartmatch.FeatureVector
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.smartmatch.SmartMatchModelVersions
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * D16 — Contrato productivo confirmado (2026-11-26):
 * - matchScore SIEMPRE Int 0..100 (NO decimal).
 * - breakdown points/maxPoints Double con decimales reales.
 * - GUEST solo códigos contractuales, sin textos visuales ni campos extra.
 * - GUEST 0 GET /users/me/preferences y 1 POST recommendations/query.
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

    // ---------- 1: matchScore Int 73 ----------
    @Test
    fun `1 matchScore 73 decodes as Int`() {
        val raw = """
            {"room":{"id":"r1","buildingId":"b1","title":"t","price":1000.0,"currency":"MXN",
            "buildingType":"student_residence","allowedTenants":"any","distanceKm":1.0,"walkingMinutes":5},
            "matchScore":73,"breakdown":[],"dismissed":false,
            "modelVersion":"logreg-1.0.0","featureSchemaVersion":"2.0.0"}
        """.trimIndent()
        assertEquals(73, json.decodeFromString<SmartMatchRecommendationItemDto>(raw).matchScore)
    }

    // ---------- 2/3/4: breakdown decimal y cero como Double ----------
    @Test
    fun `2 3 4 breakdown decimals and zero decode as Double`() {
        val raw = """
            {"items":[{"room":{"id":"r1","buildingId":"b1","title":"t","price":1000.0,"currency":"MXN",
            "buildingType":"student_residence","allowedTenants":"any","distanceKm":1.0,"walkingMinutes":5},
            "matchScore":73,
            "breakdown":[
              {"factor":"budget","label":"Presupuesto","points":3.5,"maxPoints":9.6},
              {"factor":"distance","label":"Distancia","points":0,"maxPoints":72.6},
              {"factor":"essential_services","label":"Servicios","points":6.7,"maxPoints":6.7},
              {"factor":"place_type","label":"Tipo de lugar","points":4.9,"maxPoints":4.9},
              {"factor":"property_type","label":"Tipo de propiedad","points":1.7,"maxPoints":1.7},
              {"factor":"extras","label":"Extras","points":4.5,"maxPoints":4.5}
            ],"dismissed":false,"modelVersion":"logreg-1.0.0","featureSchemaVersion":"2.0.0"}],
            "page":1,"pageSize":20,"totalItems":1,"totalPages":1}
        """.trimIndent()
        val item = json.decodeFromString<SmartMatchListResponse>(raw).items.single()
        assertEquals(3.5, item.breakdown[0].points, 0.0)
        assertEquals(0.0, item.breakdown[1].points, 0.0)
        assertEquals(72.6, item.breakdown[1].maxPoints, 0.0)
        assertEquals(listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5), item.breakdown.map { it.maxPoints })
    }

    @Test
    fun `fraction is points over maxPoints clamped with zero max`() {
        fun fraction(points: Double, maxPoints: Double) =
            if (maxPoints > 0.0) (points / maxPoints).coerceIn(0.0, 1.0) else 0.0
        assertEquals(3.5 / 9.6, fraction(3.5, 9.6), 1e-9)
        assertEquals(0.0, fraction(0.0, 72.6), 0.0)
        assertEquals(1.0, fraction(1.7, 1.7), 1e-9)
        assertEquals(0.0, fraction(5.0, 0.0), 0.0)
        assertTrue(!fraction(0.0, 0.0).isNaN())
    }

    @Test
    fun `UI shows integer percent never decimal`() {
        assertEquals("73%", formatMatchScore(73))
        assertEquals("100%", formatMatchScore(100))
    }

    // ---------- 5–10: códigos contractuales y sin campos extra ----------
    @Test
    fun `5 6 7 8 9 10 guest request uses only contractual codes`() = runTest {
        val api = FakeApi(query = SmartMatchRecommendationResponse(items = listOf(guestItem("g", 73))))
        val prefs = CountingLocalPrefRepo(
            Preference(
                budget = Budget(2000.0, 5000.0, "MXN"),
                essentialServices = listOf("Internet", "Agua", "Luz", "Lavandería"),
                livingWith = "Mujeres",
                placeType = "Cuarto",
                pets = "Sí",
                transport = "Caminar",
                propertyTypes = listOf("Departamento", "Residencia estudiantil"),
                currency = "MXN",
                maxDistanceKm = 5.0,
            ),
        )
        val vm = vmWithPrefs(SessionState.Anonymous, api, prefs)
        vm.load(); advanceUntilIdle()

        assertEquals(1, api.queryCalls)
        assertEquals(0, prefs.loadPreferencesCalls)
        val request = api.lastQueryRequest!!
        val p = request.preferences!!
        assertEquals(listOf("wifi", "water", "electricity", "laundry"), p.essentialServices)
        assertEquals("women", p.livingWith)
        assertEquals("room", p.placeType)
        assertEquals("walk", p.transport)
        assertEquals(listOf("apartment", "student_residence"), p.propertyTypes)

        val raw = json.encodeToString(SmartMatchQueryRequest.serializer(), request)
        assertFalse(raw.contains("Mujeres"))
        assertFalse(raw.contains("Cuarto"))
        assertFalse(raw.contains("Caminar"))
        assertFalse(raw.contains("Departamento"))
        assertFalse(raw.contains("Internet"))
        assertFalse(raw.contains("\"pets\""))
        val root = json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
        val prefsJson = root["preferences"] as kotlinx.serialization.json.JsonObject
        assertFalse(prefsJson.containsKey("pets"))
        assertFalse(prefsJson.containsKey("currency")) // currency solo vive dentro de budget
    }

    @Test
    fun `invalid visual labels are dropped instead of sent`() = runTest {
        val api = FakeApi(query = SmartMatchRecommendationResponse())
        val prefs = CountingLocalPrefRepo(
            Preference(
                budget = Budget(2000.0, 5000.0, "MXN"),
                essentialServices = listOf("Internet"),
                livingWith = "Mujeres",
                placeType = "Cuarto",
                pets = "none",
                transport = "Volar",
                propertyTypes = listOf("Castillo"),
                currency = "MXN",
                maxDistanceKm = 5.0,
            ),
        )
        val vm = vmWithPrefs(SessionState.Anonymous, api, prefs)
        vm.load(); advanceUntilIdle()
        val p = api.lastQueryRequest!!.preferences!!
        assertEquals("women", p.livingWith)
        assertEquals("room", p.placeType)
        assertEquals(null, p.transport)
        assertEquals(null, p.propertyTypes)
        assertEquals(listOf("wifi"), p.essentialServices)
    }

    // ---------- 11/12: GUEST HTTP ----------
    @Test
    fun `11 12 guest makes zero auth preference calls and one query`() = runTest {
        val api = FakeApi(query = SmartMatchRecommendationResponse(items = listOf(guestItem("g", 73))))
        val prefs = CountingLocalPrefRepo(preference())
        val vm = vmWithPrefs(SessionState.Anonymous, api, prefs)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Content)
        assertEquals(0, prefs.loadPreferencesCalls)
        assertEquals(1, api.queryCalls)
        assertEquals(73, (vm.uiState.value as SmartMatchUiState.Content).results.single().matchScore)
    }

    @Test
    fun `guest without preferences never calls query`() = runTest {
        val api = FakeApi()
        val prefs = CountingLocalPrefRepo(null)
        val vm = vmWithPrefs(SessionState.Anonymous, api, prefs)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        assertEquals(0, prefs.loadPreferencesCalls)
        assertEquals(0, api.queryCalls)
    }

    // ---------- Autoridad Backend, fallback y versión (preservado) ----------
    @Test
    fun `AUTH 200 keeps order score and breakdown without rescoring`() = runTest {
        val api = FakeApi(recommendations = SmartMatchListResponse(items = listOf(
            authItem("2", 70), authItem("1", 90),
        )))
        val vm = vmWithPrefs(SessionState.Active(UserSession("u", "n", "e@t.com")), api, CountingLocalPrefRepo(preference()))
        vm.load(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertEquals(listOf("2", "1"), results.map { it.property.id })
        assertEquals(listOf(70, 90), results.map { it.matchScore })
        assertEquals(SmartMatchSource.BACKEND, results.first().source)
        assertEquals("logreg-1.0.0", results.first().modelVersion)
        assertEquals(0, api.queryCalls)
    }

    @Test
    fun `network and 503 produce labeled estimate`() = runTest {
        listOf(DomainError.Network, DomainError.ServiceUnavailable).forEach { error ->
            val api = FakeApi(recommendationsFailure = DomainException(error))
            val vm = vmWithPrefs(SessionState.Active(UserSession("u", "n", "e@t.com")), api, CountingLocalPrefRepo(preference()))
            vm.load(); advanceUntilIdle()
            val results = (vm.uiState.value as SmartMatchUiState.Content).results
            assertTrue(results.all { it.source == SmartMatchSource.LOCAL_ESTIMATE })
        }
    }

    @Test
    fun `400 401 404 422 429 never fall back`() = runTest {
        listOf(DomainError.ValidationError, DomainError.Unauthorized, DomainError.NotFound, DomainError.RateLimited).forEach { error ->
            val api = FakeApi(recommendationsFailure = DomainException(error))
            val vm = vmWithPrefs(SessionState.Active(UserSession("u", "n", "e@t.com")), api, CountingLocalPrefRepo(preference()))
            vm.load(); advanceUntilIdle()
            assertTrue(vm.uiState.value !is SmartMatchUiState.Content)
        }
        val api = FakeApi(recommendationsFailure = SmartMatchProfileIncompleteException(listOf("budget.max")))
        val vm = vmWithPrefs(SessionState.Active(UserSession("u", "n", "e@t.com")), api, CountingLocalPrefRepo(preference()))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
    }

    @Test
    fun `reconnect replaces local estimate with remote`() = runTest {
        val online = MutableStateFlow(false)
        val api = FakeApi(recommendationsFailure = DomainException(DomainError.Network))
        val vm = vmWithPrefs(
            SessionState.Active(UserSession("u", "n", "e@t.com")), api, CountingLocalPrefRepo(preference()),
            object : ConnectivityObserver { override val isOnline = online },
        )
        vm.load(); advanceUntilIdle()
        assertTrue((vm.uiState.value as SmartMatchUiState.Content).results.all { it.source == SmartMatchSource.LOCAL_ESTIMATE })
        api.recommendationsFailure = null
        api.recommendations = SmartMatchListResponse(items = listOf(authItem("r", 73)))
        online.value = true
        vm.observeConnectivity(); advanceUntilIdle()
        val results = (vm.uiState.value as SmartMatchUiState.Content).results
        assertEquals(SmartMatchSource.BACKEND, results.first().source)
        assertEquals(73, results.first().matchScore)
    }

    @Test
    fun `versions remain distinct`() {
        assertEquals("logreg-1.0.0", SmartMatchModelVersions.SERVER_MODEL_VERSION)
        assertEquals("1.0.0", SmartMatchModelVersions.LOCAL_MODEL_VERSION)
        assertEquals("UNKNOWN", SmartMatchModelVersions.LOCAL_EQUIVALENCE)
    }

    // ---------- helpers ----------
    private fun preference() = Preference(
        budget = Budget(2000.0, 5000.0, "MXN"),
        essentialServices = listOf("wifi"),
        livingWith = "any",
        placeType = "shared",
        pets = "none",
        transport = "any",
        propertyTypes = listOf("room"),
        currency = "MXN",
        maxDistanceKm = 5.0,
    )

    private class CountingLocalPrefRepo(preference: Preference?) : PreferenceRepository {
        private val state = MutableStateFlow(preference)
        override val preferenceState = state
        var loadPreferencesCalls = 0
            private set
        override suspend fun loadPreferences() { loadPreferencesCalls++ }
        override suspend fun savePreferences(preference: Preference) { state.value = preference }
        override suspend fun restoreLocalOrNull(): Preference? = state.value
    }

    private fun authItem(id: String, score: Int) = SmartMatchRecommendationItemDto(
        room = SmartMatchRoomSearchDto(
            id = id, buildingId = "b1", title = "t$id", price = 3000.0, currency = "MXN",
            buildingType = "student_residence", allowedTenants = "any", services = listOf("wifi"),
            distanceKm = 1.0, walkingMinutes = 12,
        ),
        matchScore = score,
        breakdown = listOf(SmartMatchFactorDto("budget", "Presupuesto", 9.6, 9.6)),
        dismissed = false,
        modelVersion = "logreg-1.0.0",
        featureSchemaVersion = "2.0.0",
    )

    private fun guestItem(id: String, score: Int) = SmartMatchRecommendationDto(
        roomId = id, title = "t$id", price = 3000.0, currency = "MXN", matchScore = score,
        dismissed = false,
        breakdown = listOf(SmartMatchFactorDto("budget", "Presupuesto", 3.5, 9.6)),
        modelVersion = "logreg-1.0.0",
    )

    private fun vmWithPrefs(
        session: SessionState,
        api: FakeApi,
        prefs: PreferenceRepository,
        connectivity: ConnectivityObserver? = null,
    ): SmartMatchViewModel {
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
            override fun predictProbability(vector: FeatureVector) = 0.73
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
        var lastQueryRequest: SmartMatchQueryRequest? = null,
    ) : SmartMatchApi {
        override suspend fun getRecommendations(page: Int, pageSize: Int): SmartMatchListResponse {
            getRecommendationsCalls++
            recommendationsFailure?.let { throw it }
            return recommendations
        }
        override suspend fun queryRecommendations(request: SmartMatchQueryRequest): SmartMatchRecommendationResponse {
            queryCalls++
            lastQueryRequest = request
            return query
        }
        override suspend fun calculateScores(request: SmartMatchScoreRequest): SmartMatchScoreResponse = SmartMatchScoreResponse()
        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest): SmartMatchDismissalResponse = SmartMatchDismissalResponse()
        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse = SmartMatchDismissalResponse()
        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse = throw UnsupportedOperationException()
        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse = throw UnsupportedOperationException()
    }
}
