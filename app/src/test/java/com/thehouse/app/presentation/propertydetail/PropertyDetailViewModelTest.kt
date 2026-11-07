package com.thehouse.app.presentation.propertydetail

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.usecase.GetPropertyByIdUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val detailProperty = Property(
    id = "prop-1",
    title = "Estudio Centro",
    price = 3200.0,
    currency = "MXN",
    city = "Tulancingo",
    universityId = null,
    bedrooms = 1,
    bathrooms = 1,
    areaM2 = 28.5,
    createdAt = "2026-11-05T00:00:00Z",
    description = "Cerca del campus",
    amenities = listOf("wifi"),
)

private class FakeDetailRepository(
    private val result: Result<Property>,
) : PropertyRepository {
    override suspend fun getProperties(page: Int, pageSize: Int) =
        Result.success(PagedResult<Property>(emptyList(), 1, 20, 0, 0))

    override suspend fun getPropertyById(id: String): Result<Property> = result
}

class PropertyDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `detail 200 muestra content`() = runTest {
        val repo = FakeDetailRepository(Result.success(detailProperty))
        val vm = PropertyDetailViewModel(
            propertyId = "prop-1",
            getPropertyById = GetPropertyByIdUseCase(repo),
            sessionState = MutableStateFlow(com.thehouse.app.domain.model.SessionState.Anonymous),
        )
        runCurrent()
        val state = vm.uiState.value as PropertyDetailUiState.Content
        assertEquals("Estudio Centro", state.property.title)
    }

    @Test
    fun `detail 404 muestra NotFound`() = runTest {
        val repo = FakeDetailRepository(Result.failure(DomainException(DomainError.NotFound)))
        val vm = PropertyDetailViewModel(
            propertyId = "missing",
            getPropertyById = GetPropertyByIdUseCase(repo),
            sessionState = MutableStateFlow(com.thehouse.app.domain.model.SessionState.Anonymous),
        )
        runCurrent()
        assertTrue(vm.uiState.value is PropertyDetailUiState.NotFound)
    }

    @Test
    fun `network error muestra Error y retry recarga`() = runTest {
        var calls = 0
        val repo = object : PropertyRepository {
            override suspend fun getProperties(page: Int, pageSize: Int) =
                Result.success(PagedResult<Property>(emptyList(), 1, 20, 0, 0))

            override suspend fun getPropertyById(id: String): Result<Property> {
                calls++
                return if (calls == 1) {
                    Result.failure(DomainException(DomainError.Network))
                } else {
                    Result.success(detailProperty)
                }
            }
        }
        val vm = PropertyDetailViewModel(
            propertyId = "prop-1",
            getPropertyById = GetPropertyByIdUseCase(repo),
            sessionState = MutableStateFlow(com.thehouse.app.domain.model.SessionState.Anonymous),
        )
        runCurrent()
        assertTrue(vm.uiState.value is PropertyDetailUiState.Error)
        vm.onEvent(PropertyDetailEvent.Retry)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is PropertyDetailUiState.Content)
    }
}
