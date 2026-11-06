package com.thehouse.app.presentation.catalog

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.SelectedUniversityRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.domain.usecase.SearchPropertiesUseCase
import com.thehouse.app.domain.usecase.SelectUniversityUseCase
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.presentation.explore.ExploreEvent
import com.thehouse.app.presentation.explore.ExploreViewModel
import com.thehouse.app.presentation.home.HomeEvent
import com.thehouse.app.presentation.home.HomeViewModel
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val university = University(
    id = "uni-1",
    code = "UTTT",
    name = "Universidad Tecnologica de Tulancingo",
    city = "Tulancingo",
    country = "MX",
    createdAt = "2026-11-05T00:00:00Z",
)

private val property = Property(
    id = "prop-1",
    title = "Estudio Centro",
    price = 3200.0,
    currency = "MXN",
    city = "Tulancingo",
    universityId = "uni-1",
    bedrooms = 1,
    bathrooms = 1,
    areaM2 = 28.5,
    createdAt = "2026-11-05T00:00:00Z",
)

private class FakeUniversityRepository(
    private val result: Result<PagedResult<University>> =
        Result.success(PagedResult(listOf(university), 1, 20, 1, 1)),
) : UniversityRepository {
    override suspend fun getUniversities(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<University>> = result

    override suspend fun getUniversityById(id: String): Result<University> =
        Result.success(university)
}

private class FakePropertyRepository(
    private val result: Result<PagedResult<Property>> =
        Result.success(PagedResult(listOf(property), 1, 20, 1, 1)),
) : PropertyRepository {
    override suspend fun getProperties(
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<Property>> = result

    override suspend fun getPropertyById(id: String): Result<Property> =
        Result.success(property)
}

private class FakeSelectedRepository : SelectedUniversityRepository {
    private val state = MutableStateFlow<String?>(null)
    override val selectedUniversityId: StateFlow<String?> = state

    override suspend fun loadSelected() {}
    override suspend fun select(universityId: String) {
        state.value = universityId
    }

    override suspend fun clear() {
        state.value = null
    }
}

class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `home carga universidad y propiedad reales con primera destacada`() = runTest {
        val selected = FakeSelectedRepository()
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(com.thehouse.app.presentation.catalog.FakeCatalogAuth()),
            sessionState = MutableStateFlow(SessionState.Anonymous),
            getUniversities = GetUniversitiesUseCase(FakeUniversityRepository()),
            getProperties = GetPropertiesUseCase(FakePropertyRepository()),
            selectedRepository = selected,
            selectUniversity = SelectUniversityUseCase(selected),
        )
        runCurrent()
        assertTrue(vm.uiState.value.universities is CatalogLoadState.Success)
        assertTrue(vm.uiState.value.properties is CatalogLoadState.Success)
        assertEquals("Estudio Centro", vm.uiState.value.featuredProperty?.title)
    }

    @Test
    fun `home seleccion persiste via repositorio`() = runTest {
        val selected = FakeSelectedRepository()
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(com.thehouse.app.presentation.catalog.FakeCatalogAuth()),
            sessionState = MutableStateFlow(SessionState.Anonymous),
            getUniversities = GetUniversitiesUseCase(FakeUniversityRepository()),
            getProperties = GetPropertiesUseCase(FakePropertyRepository()),
            selectedRepository = selected,
            selectUniversity = SelectUniversityUseCase(selected),
        )
        runCurrent()
        vm.onEvent(HomeEvent.UniversitySelected("uni-1"))
        advanceUntilIdle()
        assertEquals("uni-1", vm.uiState.value.selectedUniversityId)
        assertEquals("uni-1", selected.selectedUniversityId.value)
    }

    @Test
    fun `explore real y busqueda local sobre titulo ciudad`() = runTest {
        val vm = ExploreViewModel(
            getProperties = GetPropertiesUseCase(FakePropertyRepository()),
            searchProperties = SearchPropertiesUseCase(),
        )
        runCurrent()
        assertTrue(vm.uiState.value.properties is CatalogLoadState.Success)
        vm.onEvent(ExploreEvent.QueryChanged("tulancingo"))
        runCurrent()
        assertTrue(vm.uiState.value.properties is CatalogLoadState.Success)
        vm.onEvent(ExploreEvent.QueryChanged("sin-resultados"))
        runCurrent()
        assertTrue(vm.uiState.value.properties is CatalogLoadState.Empty)
    }

    @Test
    fun `explore empty sin datos`() = runTest {
        val emptyRepo = FakePropertyRepository(
            Result.success(PagedResult(emptyList(), 1, 20, 0, 0)),
        )
        val vm = ExploreViewModel(
            getProperties = GetPropertiesUseCase(emptyRepo),
            searchProperties = SearchPropertiesUseCase(),
        )
        runCurrent()
        assertTrue(vm.uiState.value.properties is CatalogLoadState.Empty)
    }
}

/** Fake mínimo de AuthRepository para HomeViewModel (sin red). */
class FakeCatalogAuth : com.thehouse.app.domain.repository.AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Anonymous)
    override val session: StateFlow<SessionState> = state

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<com.thehouse.app.domain.model.UserSession?> = Result.success(null)

    override suspend fun login(
        email: String,
        password: String,
    ): Result<com.thehouse.app.domain.model.UserSession?> = Result.success(null)

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
    override suspend fun refreshAccessToken(): Result<String?> = Result.success(null)
    override suspend fun getCurrentUser(): Result<com.thehouse.app.domain.model.UserSession?> =
        Result.success(null)

    override suspend fun restoreSession(): Result<com.thehouse.app.domain.model.UserSession?> =
        Result.success(null)
}
