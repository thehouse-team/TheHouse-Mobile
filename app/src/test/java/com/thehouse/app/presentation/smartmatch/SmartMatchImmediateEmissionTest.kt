package com.thehouse.app.presentation.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.api.SmartMatchDismissalResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchScoreRequest
import com.thehouse.app.data.remote.api.SmartMatchScoreResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchListResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchRecommendationResponse
import com.thehouse.app.data.smartmatch.SmartMatchRepository
import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.smartmatch.FeatureVector
import com.thehouse.app.domain.smartmatch.SmartMatchModel
import com.thehouse.app.domain.usecase.FavoriteStateStore
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.RunSmartMatchUseCase
import com.thehouse.app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D20 hotfix: reproduce el FATAL EXCEPTION del collector de favoritos.
 * Con UnconfinedTestDispatcher el collector emite INMEDIATAMENTE durante la
 * construcción del ViewModel. Si `_uiState` se declara después del `init`,
 * falla con NPE (stacktrace reportado en SmartMatchViewModel.kt:63).
 */
class SmartMatchImmediateEmissionTest {

    private class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val io: CoroutineDispatcher get() = dispatcher
        override val default: CoroutineDispatcher get() = dispatcher
    }

    private class EmptyApi : SmartMatchApi {
        override suspend fun calculateScores(request: SmartMatchScoreRequest) = SmartMatchScoreResponse()
        override suspend fun getRecommendations(page: Int, pageSize: Int) = SmartMatchListResponse()
        override suspend fun queryRecommendations(request: SmartMatchQueryRequest) = SmartMatchRecommendationResponse()
        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest) = SmartMatchDismissalResponse()
        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse = SmartMatchDismissalResponse()
        private fun explanation() = SmartMatchExplanationResponse(
            roomId = "room-1", score = 90.0, summary = "resumen",
            generatedBy = "template", modelVersion = "v1", explanationVersion = "v1",
        )
        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse = explanation()
        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse = explanation()
    }

    private class EmptyPreferences : PreferenceRepository {
        private val state = MutableStateFlow<com.thehouse.app.domain.model.Preference?>(null)
        override val preferenceState: StateFlow<com.thehouse.app.domain.model.Preference?> = state
        override suspend fun loadPreferences() = Unit
        override suspend fun savePreferences(preference: com.thehouse.app.domain.model.Preference) = Unit
    }

    private class EmptyProperties : PropertyRepository {
        override suspend fun getProperties(page: Int, pageSize: Int): Result<PagedResult<Property>> =
            Result.success(PagedResult(emptyList(), 1, 20, 0, 0))
        override suspend fun getPropertyById(id: String): Result<Property> =
            Result.failure(UnsupportedOperationException())
    }

    private fun build(session: SessionState, store: FavoriteStateStore): SmartMatchViewModel {
        val dispatcher = UnconfinedTestDispatcher()
        val model = object : SmartMatchModel {
            override val modelVersion = "test"
            override val featureVersion = "test"
            override val artifactHash = "hash"
            override fun predictProbability(vector: FeatureVector): Double = 0.5
        }
        return SmartMatchViewModel(
            sessionState = MutableStateFlow(session),
            preferenceRepository = EmptyPreferences(),
            smartMatchRepository = SmartMatchRepository(EmptyApi()),
            getProperties = GetPropertiesUseCase(EmptyProperties()),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = TestDispatchers(dispatcher),
            favoriteStateStore = store,
            toggleFavorite = ToggleFavoriteUseCase(
                repository = object : com.thehouse.app.domain.repository.FavoritesRepository {
                    override suspend fun getFavorites(page: Int, pageSize: Int): Result<PagedResult<com.thehouse.app.domain.model.Favorite>> =
                        Result.success(PagedResult(emptyList(), 1, 20, 0, 0))
                    override suspend fun getFavoriteIds(): Result<Set<String>> = Result.success(emptySet())
                    override suspend fun addFavorite(roomId: String): Result<Unit> = Result.success(Unit)
                    override suspend fun removeFavorite(roomId: String): Result<Unit> = Result.success(Unit)
                },
                store = store,
                isAuthenticated = { true },
                inFlight = mutableSetOf(),
            ),
        )
    }

    @Test
    fun `guest con store ya emitido no crashea al construir el ViewModel`() = runTest {
        val store = FavoriteStateStore().also { it.setAll(setOf("room-1")) }
        val vm = build(SessionState.Anonymous, store)
        assertTrue(vm.uiState.value is SmartMatchUiState.Loading || vm.uiState.value is SmartMatchUiState.Content)
    }

    @Test
    fun `sesion Active con store ya emitido no crashea al construir el ViewModel`() = runTest {
        val store = FavoriteStateStore().also { it.setAll(setOf("room-1")) }
        val vm = build(SessionState.Active(UserSession("u1", "Ana", "a@x.mx")), store)
        assertTrue(vm.uiState.value is SmartMatchUiState.Loading || vm.uiState.value is SmartMatchUiState.Content)
    }
}
