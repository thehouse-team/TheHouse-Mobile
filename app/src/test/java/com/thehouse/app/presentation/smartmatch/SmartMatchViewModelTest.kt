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

@OptIn(ExperimentalCoroutinesApi::class)
class SmartMatchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private class FakePreferenceRepo(var pref: Preference?, var fail: Throwable? = null) : PreferenceRepository {
        private val state = MutableStateFlow<Preference?>(pref)
        override val preferenceState: Flow<Preference?> = state
        override suspend fun loadPreferences() {
            fail?.let { throw it }
            state.value = pref
        }
        override suspend fun savePreferences(preference: Preference) { pref = preference; state.value = preference }
    }

    private class FakePropertyRepo(private val result: Result<PagedResult<Property>>) : com.thehouse.app.domain.repository.PropertyRepository {
        override suspend fun getProperties(page: Int, pageSize: Int): Result<PagedResult<Property>> = result
        override suspend fun getPropertyById(id: String): Result<Property> = Result.failure(UnsupportedOperationException())
    }

    /** Fake del contrato NV-23 v1.2.0 para el endpoint guest (query pública). */
    private class FakeSmartMatchApi(
        private val queryResponse: SmartMatchRecommendationResponse = SmartMatchRecommendationResponse(),
        private val recommendations: SmartMatchListResponse = SmartMatchListResponse(),
        private val recommendationsFailure: Throwable? = null,
    ) : SmartMatchApi {
        var queryCalls: Int = 0
            private set
        var recommendationCalls: Int = 0
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
            return queryResponse
        }

        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()

        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()

        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse = explanation()

        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse =
            explanation()

        private fun explanation() = SmartMatchExplanationResponse(
            roomId = "room-1",
            score = 90.0,
            summary = "resumen",
            generatedBy = "template",
            modelVersion = "v1",
            explanationVersion = "v1",
        )
    }

    private fun guestSmartMatchRepository(api: FakeSmartMatchApi = FakeSmartMatchApi()): SmartMatchRepository =
        SmartMatchRepository(api)

    private val model = object : SmartMatchModel {
        override val modelVersion = "test"
        override val featureVersion = "test"
        override val artifactHash = "hash"
        override fun predictProbability(vector: FeatureVector): Double = vector.values.average().coerceIn(0.0, 1.0)
    }

    private fun activeSession() = SessionState.Active(
        UserSession(userId = "u1", displayName = "n", email = "e@t.com"),
    )

    private fun property(id: String) = Property(id, "t$id", 3000.0, "MXN", "Puebla", null, 1, 1, 20.0, "2026-01-01T00:00:00Z", null, listOf("wifi"))
    private fun meaningfulPref() = Preference(Budget(2000.0, 4000.0, "MXN"), null, listOf("wifi"), null, null, null, null, null, "MXN")

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `no preferences state`() = runTest {
        // AUTH: el Backend reporta 422 smart_match_profile_incomplete con missing.
        val incomplete = com.thehouse.app.domain.error.SmartMatchProfileIncompleteException(
            listOf("university", "budget.max", "livingWith", "placeType"),
        )
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(activeSession()),
            preferenceRepository = FakePreferenceRepo(pref = null),
            smartMatchRepository = SmartMatchRepository(FakeSmartMatchApi(recommendationsFailure = incomplete)),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
        assertEquals(
            listOf("university", "budget.max", "livingWith", "placeType"),
            vm.profileMissing.value,
        )
    }

    @Test
    fun `empty candidates state`() = runTest {
        // AUTH: 200 con items vacíos → Empty (sin preferencias locales ni catálogo).
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(activeSession()),
            preferenceRepository = FakePreferenceRepo(pref = null),
            smartMatchRepository = guestSmartMatchRepository(),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Empty)
    }

    @Test
    fun `network error maps to error state`() = runTest {
        // AUTH: 429 NO es elegible a fallback; se mantiene como Error tipado.
        val rateLimited = DomainException(DomainError.RateLimited)
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(activeSession()),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            smartMatchRepository = SmartMatchRepository(FakeSmartMatchApi(recommendationsFailure = rateLimited)),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.failure(DomainException(DomainError.Network)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is SmartMatchUiState.Error)
        assertEquals(com.thehouse.app.presentation.common.UiError.RateLimited, (state as SmartMatchUiState.Error).error)
    }

    @Test
    fun `content with ranked results`() = runTest {
        // AUTH: el Backend devuelve items ya ordenados/scoreados; Mobile no reordena.
        val recommendations = SmartMatchListResponse(
            items = listOf(backendItem("2", 70.0), backendItem("1", 90.0)),
        )
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(activeSession()),
            preferenceRepository = FakePreferenceRepo(pref = null),
            smartMatchRepository = SmartMatchRepository(FakeSmartMatchApi(recommendations = recommendations)),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s is SmartMatchUiState.Content)
        val results = (s as SmartMatchUiState.Content).results
        assertEquals(2, results.size)
        assertEquals(listOf("2", "1"), results.map { it.property.id })
        assertEquals(listOf(70.0, 90.0), results.map { it.matchScore })
    }

    private fun backendItem(id: String, score: Double) =
        com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationItemDto(
            room = com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRoomSearchDto(
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
            breakdown = listOf(
                com.thehouse.app.data.remote.dto.smartmatch.SmartMatchFactorDto(
                    factor = "budget", label = "Presupuesto", points = 30.0, maxPoints = 30.0,
                ),
            ),
            dismissed = false,
            modelVersion = "logreg-1.0.0",
            featureSchemaVersion = "2.0.0",
        )

    @Test
    fun `suspended session requires auth`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Suspended(UserSession(userId = "u1", displayName = "n", email = "e@t.com"))),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            smartMatchRepository = guestSmartMatchRepository(),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.RequiresAuth)
    }

    @Test
    fun `guest without preferences shows no preferences state`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Anonymous),
            preferenceRepository = FakePreferenceRepo(pref = null),
            smartMatchRepository = guestSmartMatchRepository(),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        // Preferencias ausentes/incompletas: estado tipado, sin query inválida.
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
    }

    @Test
    fun `guest with preferences queries public recommendations endpoint`() = runTest {
        val api = FakeSmartMatchApi(SmartMatchRecommendationResponse(items = emptyList()))
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Anonymous),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            smartMatchRepository = guestSmartMatchRepository(api),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Empty)
        assertEquals(1, api.queryCalls)
    }

    /** D16: GUEST 200 usa el score/orden del Backend, sin re-score local. */
    @Test
    fun `guest with preferences and candidates returns backend content`() = runTest {
        val api = FakeSmartMatchApi(
            SmartMatchRecommendationResponse(
                items = listOf(
                    SmartMatchRecommendationDto(
                        roomId = "1",
                        title = "t1",
                        price = 3000.0,
                        currency = "MXN",
                        matchScore = 80.0,
                        dismissed = false,
                    ),
                ),
            ),
        )
        val props = listOf(property("1"), property("2"))
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Anonymous),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            smartMatchRepository = guestSmartMatchRepository(api),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(props,1,20,2,1)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s is SmartMatchUiState.Content)
        val results = (s as SmartMatchUiState.Content).results
        assertEquals(1, results.size)
        assertEquals(80.0, results.first().matchScore, 0.0)
        assertEquals(com.thehouse.app.domain.smartmatch.SmartMatchSource.BACKEND, results.first().source)
        assertEquals(1, api.queryCalls)
    }
}
