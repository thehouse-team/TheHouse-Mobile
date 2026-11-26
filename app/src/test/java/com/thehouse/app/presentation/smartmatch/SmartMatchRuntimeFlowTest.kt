package com.thehouse.app.presentation.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.api.SmartMatchDismissalResponse
import com.thehouse.app.data.remote.api.SmartMatchScoreResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchListResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationDto
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchScoreRequest
import com.thehouse.app.data.smartmatch.SmartMatchRepository
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.smartmatch.FeatureVector
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Runbook SMART-MATCH RUNTIME — sección 9 (A–F).
 *
 * Cubre disparador de carga, sesión, preferencias, candidatos, retry y logs.
 * Sin backend real, sin ML modificado y sin fallback artificial.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmartMatchRuntimeFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private class FakePrefRepo(
        private val initial: Preference?,
        private val failure: Throwable? = null,
    ) : PreferenceRepository {
        private val state = MutableStateFlow(initial)
        override val preferenceState: Flow<Preference?> = state
        var loadCalls = 0
            private set

        override suspend fun loadPreferences() {
            loadCalls++
            if (failure != null) throw failure
            state.value = initial
        }

        override suspend fun savePreferences(preference: Preference) {
            state.value = preference
        }
    }

    private class FakePropertyRepo(
        private val result: Result<PagedResult<Property>>,
    ) : PropertyRepository {
        var calls = 0
            private set

        override suspend fun getProperties(page: Int, pageSize: Int): Result<PagedResult<Property>> {
            calls++
            return result
        }

        override suspend fun getPropertyById(id: String): Result<Property> =
            Result.failure(UnsupportedOperationException())
    }

    private class CountingApi(
        private val response: SmartMatchRecommendationResponse = SmartMatchRecommendationResponse(),
        private val recommendations: SmartMatchListResponse = SmartMatchListResponse(),
        private val recommendationsFailure: Throwable? = null,
    ) : SmartMatchApi {
        var queryCalls = 0
            private set
        var recommendationCalls = 0
            private set

        override suspend fun calculateScores(request: SmartMatchScoreRequest): SmartMatchScoreResponse =
            SmartMatchScoreResponse()

        override suspend fun getRecommendations(page: Int, pageSize: Int): SmartMatchListResponse {
            recommendationCalls++
            recommendationsFailure?.let { throw it }
            return recommendations
        }

        override suspend fun queryRecommendations(request: SmartMatchQueryRequest): SmartMatchRecommendationResponse {
            queryCalls++
            return response
        }

        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()

        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()

        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse = explanation()
        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse =
            explanation()

        private fun explanation() = SmartMatchExplanationResponse(
            roomId = "room", score = 90.0, summary = "s", generatedBy = "template",
            modelVersion = "v1", explanationVersion = "v1",
        )
    }

    private val model = object : SmartMatchModel {
        override val modelVersion = "test"
        override val featureVersion = "test"
        override val artifactHash = "hash"
        override fun predictProbability(vector: FeatureVector): Double = 0.75
    }

    private fun pref() = Preference(
        budget = Budget(2000.0, 4000.0, "MXN"),
        maxDistanceKm = null,
        essentialServices = listOf("wifi"),
        livingWith = null,
        placeType = null,
        pets = null,
        transport = null,
        propertyTypes = null,
        currency = "MXN",
    )

    private fun property(id: String) = Property(
        id = id, title = "t$id", price = 3000.0, currency = "MXN", city = "Puebla",
        universityId = null, bedrooms = 1, bathrooms = 1, areaM2 = 20.0,
        createdAt = "2026-01-01T00:00:00Z", thumbnailUrl = null,
    )

    private fun vm(
        session: SessionState,
        prefs: FakePrefRepo,
        properties: FakePropertyRepo,
        api: CountingApi,
    ) = SmartMatchViewModel(
        sessionState = MutableStateFlow(session),
        preferenceRepository = prefs,
        smartMatchRepository = SmartMatchRepository(api),
        getProperties = GetPropertiesUseCase(properties),
        runSmartMatch = RunSmartMatchUseCase(model),
        dispatchers = dispatchers,
    )

    private fun captureLogs(block: () -> Unit): List<String> {
        val logs = mutableListOf<String>()
        val previous = SmartMatchFlowLogger.sink
        SmartMatchFlowLogger.sink = logs::add
        try {
            block()
        } finally {
            SmartMatchFlowLogger.sink = previous
        }
        return logs
    }

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    /** A — Anonymous con preferencias completas: load ocurre y query exactamente 1 vez. */
    @Test
    fun `A anonymous with preferences queries once`() = runTest {
        val api = CountingApi(
            SmartMatchRecommendationResponse(items = listOf(
                SmartMatchRecommendationDto("1", "t1", 3000.0, "MXN", 80.0, false),
            )),
        )
        val logs = captureLogs {
            val vm = vm(SessionState.Anonymous, FakePrefRepo(pref()), FakePropertyRepo(
                Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1))), api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.Content)
        }
        assertEquals(1, api.queryCalls)
        assertTrue(logs.any { it == "session=Anonymous" })
        assertTrue(logs.any { it == "guest_query_start" })
        assertTrue(logs.any { it == "guest_backend_authority" })
        assertTrue(logs.none { it == "ranking_start" })
    }

    /** B — Anonymous sin preferencias: NoPreferences, 0 llamadas al endpoint. */
    @Test
    fun `B anonymous without preferences shows NoPreferences and no HTTP`() = runTest {
        val api = CountingApi()
        val logs = captureLogs {
            val vm = vm(SessionState.Anonymous, FakePrefRepo(null), FakePropertyRepo(
                Result.success(PagedResult(emptyList(), 1, 50, 0, 0))), api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        }
        assertEquals(0, api.queryCalls)
        assertTrue(logs.any { it == "preferences_result=missing" })
        assertTrue(logs.any { it == "state=NoPreferences" })
        assertTrue(logs.none { it == "guest_query_start" })
    }

    /** B2 — Preferencias presentes pero incompletas: NoPreferences, no error genérico. */
    @Test
    fun `B2 anonymous with incomplete preferences is NoPreferences`() = runTest {
        val emptyPref = Preference(null, null, null, null, null, null, null, null, null)
        val api = CountingApi()
        captureLogs {
            val vm = vm(SessionState.Anonymous, FakePrefRepo(emptyPref), FakePropertyRepo(
                Result.success(PagedResult(emptyList(), 1, 50, 0, 0))), api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        }
        assertEquals(0, api.queryCalls)
    }

    /** C — D16: GUEST 200 usa el Backend aunque fallase el catálogo local. */
    @Test
    fun `C guest backend authority ignores local catalog failure`() = runTest {
        val api = CountingApi(
            SmartMatchRecommendationResponse(items = listOf(
                SmartMatchRecommendationDto("1", "t1", 3000.0, "MXN", 80.0, false),
            )),
        )
        val logs = captureLogs {
            val vm = vm(
                SessionState.Anonymous,
                FakePrefRepo(pref()),
                FakePropertyRepo(Result.failure(IllegalStateException("simulated"))),
                api,
            )
            vm.load()
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state is SmartMatchUiState.Content)
            assertEquals(80.0, (state as SmartMatchUiState.Content).results.single().matchScore, 0.0)
        }
        assertTrue(logs.any { it == "guest_backend_authority" })
        assertTrue(logs.none { it == "ranking_start" })
        assertTrue(logs.none { it.contains("simulated") })
    }

    /** C2 — Preferencias fallan (401 simulado): NoPreferences + excepción observable. */
    @Test
    fun `C2 preference load failure is observable and not generic Error`() = runTest {
        val api = CountingApi()
        val logs = captureLogs {
            val vm = vm(
                SessionState.Anonymous,
                FakePrefRepo(null, failure = DomainException(DomainError.Unauthorized)),
                FakePropertyRepo(Result.success(PagedResult(emptyList(), 1, 50, 0, 0))),
                api,
            )
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        }
        assertTrue(logs.any { it == "exception=DomainException operation=loadPreferences" })
        assertEquals(0, api.queryCalls)
    }

    /** D — Retry dispara exactamente una carga adicional (sin catálogo local). */
    @Test
    fun `D retry triggers exactly one additional attempt`() = runTest {
        val api = CountingApi(
            SmartMatchRecommendationResponse(items = listOf(
                SmartMatchRecommendationDto("1", "t1", 3000.0, "MXN", 80.0, false),
            )),
        )
        val prefs = FakePrefRepo(pref())
        val properties = FakePropertyRepo(Result.success(PagedResult(emptyList(), 1, 50, 0, 0)))
        val vm = vm(SessionState.Anonymous, prefs, properties, api)
        vm.load()
        advanceUntilIdle()
        assertEquals(1, prefs.loadCalls)
        vm.load()
        advanceUntilIdle()
        assertEquals(2, prefs.loadCalls)
        // D16: GUEST 200 ya no consulta catálogo ni re-scorea.
        assertEquals(0, properties.calls)
        assertEquals(2, api.queryCalls)
    }

    /** E — Active: NV-23 autoridad Backend (GET recommendations), sin ranking local. */
    @Test
    fun `E active session keeps authenticated flow`() = runTest {
        val active = SessionState.Active(UserSession("u1", "n", "e@t.com"))
        val api = CountingApi(recommendations = authList(authItem("1", 90.0)))
        val prefs = FakePrefRepo(pref())
        val properties = FakePropertyRepo(Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1)))
        val logs = captureLogs {
            val vm = vm(active, prefs, properties, api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.Content)
        }
        assertTrue(logs.any { it == "session=Active" })
        assertTrue(logs.any { it == "auth_query_start" })
        assertTrue(logs.any { it == "auth_query_result count=1" })
        assertTrue(logs.none { it == "ranking_start" })
        assertTrue(logs.none { it == "candidates_start" })
        assertEquals(1, api.recommendationCalls)
        assertEquals(0, api.queryCalls)
        assertEquals(0, prefs.loadCalls) // Online AUTH NO carga preferencias locales.
        assertEquals(0, properties.calls) // Online AUTH NO consulta el catálogo.
    }

    private fun authItem(id: String, score: Double) =
        com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationItemDto(
            room = com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRoomSearchDto(
                id = id, buildingId = "b1", title = "t$id", price = 3000.0, currency = "MXN",
                buildingType = "student_residence", allowedTenants = "any",
                services = listOf("wifi"), distanceKm = 1.0, walkingMinutes = 12,
            ),
            matchScore = score,
            breakdown = listOf(
                com.thehouse.app.data.remote.dto.smartmatch.SmartMatchFactorDto(
                    factor = "budget", label = "Presupuesto", points = 30.0, maxPoints = 30.0,
                ),
            ),
            dismissed = false, modelVersion = "logreg-1.0.0", featureSchemaVersion = "2.0.0",
        )

    private fun authList(vararg items: com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationItemDto) =
        SmartMatchListResponse(items = items.toList())

    private fun activeVm(
        api: CountingApi,
        prefs: FakePrefRepo = FakePrefRepo(pref()),
        properties: FakePropertyRepo = FakePropertyRepo(Result.success(PagedResult(emptyList(), 1, 50, 0, 0))),
    ) = vm(SessionState.Active(UserSession("u1", "n", "e@t.com")), prefs, properties, api)

    /** A — Active + 200 items: Content, orden y matchScore del Backend intactos. */
    @Test
    fun `A active 200 items keeps backend authority`() = runTest {
        val api = CountingApi(recommendations = authList(authItem("2", 70.0), authItem("1", 90.0)))
        val vm = activeVm(api)
        vm.load(); advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is SmartMatchUiState.Content)
        val results = (state as SmartMatchUiState.Content).results
        assertEquals(listOf("2", "1"), results.map { it.property.id })
        assertEquals(listOf(70.0, 90.0), results.map { it.matchScore })
        assertEquals(listOf("Presupuesto"), results.first().factors.map { it.label })
        assertEquals(0, api.queryCalls)
    }

    /** B — Active + 200 items=[]: Empty. */
    @Test
    fun `B active empty recommendations is Empty`() = runTest {
        val vm = activeVm(CountingApi(recommendations = authList()))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Empty)
    }

    /** C — Active + 422 profile_incomplete: NoPreferences con missing, nunca Error. */
    @Test
    fun `C active 422 profile incomplete maps to NoPreferences`() = runTest {
        val missing = listOf("university", "budget.max", "livingWith", "placeType")
        val vm = activeVm(CountingApi(recommendationsFailure =
            com.thehouse.app.domain.error.SmartMatchProfileIncompleteException(missing)))
        val logs = captureLogs {
            vm.load(); advanceUntilIdle()
        }
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        assertEquals(missing, vm.profileMissing.value)
        assertTrue(logs.none { it.startsWith("state=Error") })
    }

    /** D — Active + 401: RequiresAuth. */
    @Test
    fun `D active 401 maps to RequiresAuth`() = runTest {
        val vm = activeVm(CountingApi(recommendationsFailure = DomainException(DomainError.Unauthorized)))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.RequiresAuth)
    }

    /** E2 — Active + 429: Error tipado, sin fallback ML. */
    @Test
    fun `E2 active 429 is typed error without fallback`() = runTest {
        val prefs = FakePrefRepo(pref())
        val properties = FakePropertyRepo(Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1)))
        val logs = captureLogs {
            val vm = activeVm(
                CountingApi(recommendationsFailure = DomainException(DomainError.RateLimited)),
                prefs, properties,
            )
            vm.load(); advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state is SmartMatchUiState.Error)
            assertEquals(com.thehouse.app.presentation.common.UiError.RateLimited, (state as SmartMatchUiState.Error).error)
        }
        assertTrue(logs.none { it.startsWith("fallback_start") })
        assertEquals(0, prefs.loadCalls)
        assertEquals(0, properties.calls)
    }

    /** F — Active + 503: fallback ML local etiquetado. */
    @Test
    fun `F active 503 falls back to local ML`() = runTest {
        val properties = FakePropertyRepo(Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1)))
        val logs = captureLogs {
            val vm = activeVm(
                CountingApi(recommendationsFailure = DomainException(DomainError.ServiceUnavailable)),
                FakePrefRepo(pref()), properties,
            )
            vm.load(); advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state is SmartMatchUiState.Content)
            assertEquals(com.thehouse.app.domain.smartmatch.SmartMatchSource.LOCAL_ESTIMATE,
                (state as SmartMatchUiState.Content).results.first().source)
        }
        assertTrue(logs.any { it == "fallback_start reason=503" })
        assertTrue(logs.any { it == "candidates_start" })
        assertTrue(logs.any { it == "ranking_start" })
        assertTrue(logs.any { it == "candidates_result count=1" })
    }

    /** G — Active + IOException/network: fallback ML local. */
    @Test
    fun `G active network failure falls back to local ML`() = runTest {
        val logs = captureLogs {
            val vm = activeVm(
                CountingApi(recommendationsFailure = java.io.IOException("offline")),
                FakePrefRepo(pref()),
                FakePropertyRepo(Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1))),
            )
            vm.load(); advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.Content)
        }
        assertTrue(logs.any { it == "fallback_start reason=network" })
        assertTrue(logs.any { it == "ranking_start" })
    }

    /** H — Active + 400/404: Error tipado, sin fallback. */
    @Test
    fun `H active 400 and 404 are typed errors without fallback`() = runTest {
        for (error in listOf(DomainError.Unknown, DomainError.NotFound)) {
            val logs = captureLogs {
                val vm = activeVm(CountingApi(recommendationsFailure = DomainException(error)))
                vm.load(); advanceUntilIdle()
                assertTrue(vm.uiState.value is SmartMatchUiState.Error)
            }
            assertTrue(logs.none { it.startsWith("fallback_start") })
            assertTrue(logs.none { it == "ranking_start" })
        }
    }

    /**
     * Sesión mutable (como `sessionStore.state` de la factory): la carga toma
     * el valor vigente en el momento de entrar, sin cachear un estado viejo.
     */
    @Test
    fun `session state is read at load time like the real factory provides`() = runTest {
        val liveSession = MutableStateFlow<SessionState>(SessionState.Anonymous)
        val api = CountingApi()
        val vm = SmartMatchViewModel(
            sessionState = liveSession,
            preferenceRepository = FakePrefRepo(null),
            smartMatchRepository = SmartMatchRepository(api),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(
                Result.success(PagedResult(emptyList(), 1, 50, 0, 0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        captureLogs {
            liveSession.value = SessionState.Suspended(UserSession("u", "n", "e@t.com"))
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.RequiresAuth)
        }
        assertEquals(0, api.queryCalls)
    }

    /** F — Suspended: RequiresAuth sin tocar preferencias ni red. */
    @Test
    fun `F suspended session requires auth`() = runTest {
        val suspended = SessionState.Suspended(UserSession("u1", "n", "e@t.com"))
        val api = CountingApi()
        val prefs = FakePrefRepo(pref())
        val logs = captureLogs {
            val vm = vm(suspended, prefs, FakePropertyRepo(
                Result.success(PagedResult(emptyList(), 1, 50, 0, 0))), api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.RequiresAuth)
        }
        assertTrue(logs.any { it == "session=Suspended" })
        assertTrue(logs.any { it == "state=RequiresAuth" })
        assertEquals(0, prefs.loadCalls)
        assertEquals(0, api.queryCalls)
    }
}
