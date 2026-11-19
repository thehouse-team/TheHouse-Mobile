package com.thehouse.app.presentation.preferences

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.Budget
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.repository.PreferenceRepository
import com.thehouse.app.domain.usecase.GetPreferencesUseCase
import com.thehouse.app.domain.usecase.SavePreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load success populates editable fields`() = runTest {
        val preference = Preference(
            budget = Budget(min = 2000.0, max = 4500.0, currency = "MXN"),
            maxDistanceKm = 3.5,
            essentialServices = listOf("Internet", "Agua"),
            livingWith = "Roomies",
            placeType = "Cuarto",
            pets = "No",
            transport = "Caminar",
            propertyTypes = listOf("Cuarto", "Estudio"),
            currency = "MXN",
        )
        val vm = createViewModel(FakePreferenceRepository(preference))

        vm.onEvent(PreferencesEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.loadError)
        assertEquals("2000", state.budgetMin)
        assertEquals("4500", state.budgetMax)
        assertEquals("MXN", state.budgetCurrency)
        assertEquals("3.5", state.maxDistanceKm)
        assertEquals("Internet, Agua", state.essentialServices)
        assertEquals("Roomies", state.livingWith)
        assertEquals("Cuarto", state.placeType)
        assertEquals("No", state.pets)
        assertEquals("Caminar", state.transport)
        assertEquals("Cuarto, Estudio", state.propertyTypes)
    }

    @Test
    fun `load error sets safe error`() = runTest {
        val vm = createViewModel(
            FakePreferenceRepository(
                preference = null,
                loadError = DomainException(DomainError.Network),
            ),
        )

        vm.onEvent(PreferencesEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No hay conexión. Verifica tu red e intenta de nuevo.", state.loadError)
    }

    @Test
    fun `save builds preference from drafts and shows success`() = runTest {
        val repo = FakePreferenceRepository(preference = null)
        val vm = createViewModel(repo)
        vm.onEvent(PreferencesEvent.Load)
        advanceUntilIdle()

        vm.onEvent(PreferencesEvent.BudgetMinChanged("2500"))
        vm.onEvent(PreferencesEvent.BudgetMaxChanged("5000"))
        vm.onEvent(PreferencesEvent.BudgetCurrencyChanged("MXN"))
        vm.onEvent(PreferencesEvent.MaxDistanceKmChanged("4.2"))
        vm.onEvent(PreferencesEvent.EssentialServicesChanged("Internet, Agua, Luz"))
        vm.onEvent(PreferencesEvent.LivingWithChanged("Solo"))
        vm.onEvent(PreferencesEvent.PlaceTypeChanged("Departamento"))
        vm.onEvent(PreferencesEvent.PetsChanged("No"))
        vm.onEvent(PreferencesEvent.TransportChanged("Bici"))
        vm.onEvent(PreferencesEvent.PropertyTypesChanged("Departamento, Estudio"))
        vm.onEvent(PreferencesEvent.Save)
        advanceUntilIdle()

        val saved = repo.savedPreference
        assertNotNull(saved)
        assertEquals(2500.0, saved?.budget?.min)
        assertEquals(5000.0, saved?.budget?.max)
        assertEquals("MXN", saved?.budget?.currency)
        assertEquals(4.2, saved?.maxDistanceKm)
        assertEquals(listOf("Internet", "Agua", "Luz"), saved?.essentialServices)
        assertEquals("Solo", saved?.livingWith)
        assertEquals("Departamento", saved?.placeType)
        assertEquals("No", saved?.pets)
        assertEquals("Bici", saved?.transport)
        assertEquals(listOf("Departamento", "Estudio"), saved?.propertyTypes)
        assertEquals("Preferencias guardadas.", vm.uiState.value.message)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `save with invalid number blocks save and shows field error`() = runTest {
        val repo = FakePreferenceRepository(preference = null)
        val vm = createViewModel(repo)
        vm.onEvent(PreferencesEvent.Load)
        advanceUntilIdle()

        vm.onEvent(PreferencesEvent.BudgetMinChanged("abc"))
        vm.onEvent(PreferencesEvent.Save)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Ingresa un número válido.", state.budgetMinError)
        assertEquals("Revisa los campos marcados.", state.saveError)
        assertNull(repo.savedPreference)
    }

    @Test
    fun `save error shows safe message`() = runTest {
        val repo = FakePreferenceRepository(
            preference = null,
            saveError = DomainException(DomainError.Server),
        )
        val vm = createViewModel(repo)
        vm.onEvent(PreferencesEvent.Load)
        advanceUntilIdle()

        vm.onEvent(PreferencesEvent.Save)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertEquals("El servicio no está disponible ahora. Intenta más tarde.", state.saveError)
    }

    private fun createViewModel(repository: FakePreferenceRepository): PreferencesViewModel =
        PreferencesViewModel(
            preferenceRepository = repository,
            getPreferences = GetPreferencesUseCase(repository),
            savePreferences = SavePreferencesUseCase(repository),
        )
}

private class FakePreferenceRepository(
    private var preference: Preference?,
    private val loadError: Throwable? = null,
    private val saveError: Throwable? = null,
) : PreferenceRepository {
    private val state = MutableStateFlow(preference)
    var savedPreference: Preference? = null

    override val preferenceState: Flow<Preference?> = state.asStateFlow()

    override suspend fun loadPreferences() {
        loadError?.let { throw it }
        state.value = preference
    }

    override suspend fun savePreferences(preference: Preference) {
        saveError?.let { throw it }
        savedPreference = preference
        this.preference = preference
        state.value = preference
    }
}
