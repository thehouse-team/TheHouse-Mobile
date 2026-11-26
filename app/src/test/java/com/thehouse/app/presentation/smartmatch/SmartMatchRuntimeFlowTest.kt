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
    ) : SmartMatchApi {
        var queryCalls = 0
            private set

        override suspend fun calculateScores(request: SmartMatchScoreRequest): SmartMatchScoreResponse =
            SmartMatchScoreResponse()

        override suspend fun getRecommendations(page: Int, pageSize: Int): SmartMatchListResponse =
            SmartMatchListResponse()

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
        assertTrue(logs.any { it == "ranking_start" })
        assertTrue(logs.any { it == "candidates_result count=1" })
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

    /** C — fetchCandidates lanza: Error controlado + causa observable en el log. */
    @Test
    fun `C fetchCandidates exception is controlled and logged`() = runTest {
        val boom = IllegalStateException("simulated")
        val api = CountingApi(
            SmartMatchRecommendationResponse(items = listOf(
                SmartMatchRecommendationDto("1", "t1", 3000.0, "MXN", 80.0, false),
            )),
        )
        val logs = captureLogs {
            val vm = vm(
                SessionState.Anonymous,
                FakePrefRepo(pref()),
                FakePropertyRepo(Result.failure(boom)),
                api,
            )
            vm.load()
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state is SmartMatchUiState.Error)
        }
        assertTrue(logs.any { it.startsWith("exception=IllegalStateException operation=") })
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

    /** D — Retry dispara exactamente una carga adicional. */
    @Test
    fun `D retry triggers exactly one additional attempt`() = runTest {
        val api = CountingApi(
            SmartMatchRecommendationResponse(items = listOf(
                SmartMatchRecommendationDto("1", "t1", 3000.0, "MXN", 80.0, false),
            )),
        )
        val prefs = FakePrefRepo(pref())
        val properties = FakePropertyRepo(Result.failure(IllegalStateException("x")))
        val vm = vm(SessionState.Anonymous, prefs, properties, api)
        captureLogs {
            vm.load()
            advanceUntilIdle()
            assertEquals(1, prefs.loadCalls)
            assertEquals(1, properties.calls)
            vm.load()
            advanceUntilIdle()
            assertEquals(2, prefs.loadCalls)
            assertEquals(2, properties.calls)
        }
        // Cada carga guest intenta el endpoint una vez; retry añade exactamente una.
        assertEquals(2, api.queryCalls)
    }

    /** E — Active: flujo autenticado intacto (preferencias + candidatos + ranking). */
    @Test
    fun `E active session keeps authenticated flow`() = runTest {
        val active = SessionState.Active(UserSession("u1", "n", "e@t.com"))
        val api = CountingApi()
        val logs = captureLogs {
            val vm = vm(active, FakePrefRepo(pref()), FakePropertyRepo(
                Result.success(PagedResult(listOf(property("1")), 1, 50, 1, 1))), api)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is SmartMatchUiState.Content)
        }
        assertTrue(logs.any { it == "session=Active" })
        assertTrue(logs.any { it == "auth_query_start" })
        assertEquals(0, api.queryCalls) // El flujo autenticado usa ML on-device, no el endpoint guest.
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
