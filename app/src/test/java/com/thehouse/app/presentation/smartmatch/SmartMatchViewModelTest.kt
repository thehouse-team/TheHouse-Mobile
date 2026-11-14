package com.thehouse.app.presentation.smartmatch

import com.thehouse.app.domain.common.DispatcherProvider
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
        override val main: CoroutineDispatcher = testDispatcher
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

    private val model = object : SmartMatchModel {
        override val modelVersion = "test"
        override val featureVersion = "test"
        override val artifactHash = "hash"
        override fun predictProbability(vector: FeatureVector): Double = vector.values.average().coerceIn(0.0, 1.0)
    }

    private fun property(id: String) = Property(id, "t$id", 3000.0, "MXN", "Puebla", null, 1, 1, 20.0, "2026-01-01T00:00:00Z", null, listOf("wifi"))
    private fun meaningfulPref() = Preference(Budget(2000.0, 4000.0, "MXN"), null, listOf("wifi"), null, null, null, null, null, "MXN")

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `no preferences state`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Active(com.thehouse.app.domain.model.AuthSession("e", "n", null))),
            preferenceRepository = FakePreferenceRepo(pref = null),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.NoPreferences)
    }

    @Test
    fun `empty candidates state`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Active(com.thehouse.app.domain.model.AuthSession("e", "n", null))),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Empty)
    }

    @Test
    fun `network error maps to error state`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Active(com.thehouse.app.domain.model.AuthSession("e", "n", null))),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.failure(DomainError.Network))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.Error)
    }

    @Test
    fun `content with ranked results`() = runTest {
        val props = listOf(property("1"), property("2"))
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Active(com.thehouse.app.domain.model.AuthSession("e", "n", null))),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(props,1,20,2,1)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s is SmartMatchUiState.Content)
        val results = (s as SmartMatchUiState.Content).results
        assertEquals(2, results.size)
        assertTrue(results.first().property.id.isNotBlank()) // integrable con detalle/favoritos por propertyId real
    }

    @Test
    fun `requires auth state`() = runTest {
        val vm = SmartMatchViewModel(
            sessionState = MutableStateFlow(SessionState.Anonymous),
            preferenceRepository = FakePreferenceRepo(pref = meaningfulPref()),
            getProperties = GetPropertiesUseCase(FakePropertyRepo(Result.success(PagedResult(emptyList(),1,20,0,0)))),
            runSmartMatch = RunSmartMatchUseCase(model),
            dispatchers = dispatchers,
        )
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value is SmartMatchUiState.RequiresAuth)
    }
}
