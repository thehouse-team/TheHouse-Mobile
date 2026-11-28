package com.thehouse.app.presentation.quicksetup

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.repository.ProfileRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.domain.usecase.GetProfileUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.SelectUserUniversityUseCase
import com.thehouse.app.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickSetupViewModelTest {

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
    fun `load shows incomplete state when quick setup not completed`() = runTest {
        val repo = FakeQuickSetupProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
        )
        val vm = createViewModel(repo, FakeUniversityRepository(emptyList()))

        vm.onEvent(QuickSetupEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCompleted)
        assertEquals("Ana", state.nameDraft)
    }

    @Test
    fun `load shows completed state when quick setup completed`() = runTest {
        val repo = FakeQuickSetupProfileRepository(
            profile = UserProfile(
                name = "Ana",
                university = University("u1", "BUAP", "BUAP", "Puebla", "MX", ""),
                quickSetupCompleted = true,
            ),
        )
        val vm = createViewModel(repo, FakeUniversityRepository(emptyList()))

        vm.onEvent(QuickSetupEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isCompleted)
        assertEquals("u1", state.selectedUniversityId)
    }

    @Test
    fun `load error sets safe message`() = runTest {
        val repo = FakeQuickSetupProfileRepository(
            profile = null,
            loadError = DomainException(DomainError.Network),
        )
        val vm = createViewModel(repo, FakeUniversityRepository(emptyList()))

        vm.onEvent(QuickSetupEvent.Load)
        advanceUntilIdle()

        assertEquals("No hay conexión. Verifica tu red e intenta de nuevo.", vm.uiState.value.loadError)
    }

    @Test
    fun `save updates name and university and reflects completion`() = runTest {
        val completedProfile = UserProfile(
            name = "Ana Maria",
            university = University("u2", "UDLAP", "UDLAP", "Cholula", "MX", ""),
            quickSetupCompleted = true,
        )
        val repo = FakeQuickSetupProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
            profileAfterSave = completedProfile,
        )
        val universities = listOf(University("u2", "UDLAP", "UDLAP", "Cholula", "MX", ""))
        val vm = createViewModel(repo, FakeUniversityRepository(universities))
        vm.onEvent(QuickSetupEvent.Load)
        advanceUntilIdle()

        vm.onEvent(QuickSetupEvent.NameChanged("Ana Maria"))
        vm.onEvent(QuickSetupEvent.UniversitySelected("u2"))
        vm.onEvent(QuickSetupEvent.Save)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.isCompleted)
        assertEquals("Ana Maria", state.profile?.name)
        assertEquals("u2", state.profile?.university?.id)
        assertEquals("Configuración inicial completa.", state.message)
        assertEquals("Ana Maria", repo.lastUpdatedName)
        assertEquals("u2", repo.lastSelectedUniversityId)
    }

    @Test
    fun `save error shows action error`() = runTest {
        val repo = FakeQuickSetupProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
            updateError = DomainException(DomainError.Server),
        )
        val vm = createViewModel(repo, FakeUniversityRepository(emptyList()))
        vm.onEvent(QuickSetupEvent.Load)
        advanceUntilIdle()

        vm.onEvent(QuickSetupEvent.NameChanged("Nuevo"))
        vm.onEvent(QuickSetupEvent.Save)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertEquals("El servicio no está disponible ahora. Intenta más tarde.", state.actionError)
    }

    private fun createViewModel(
        profileRepository: FakeQuickSetupProfileRepository,
        universityRepository: FakeUniversityRepository,
    ): QuickSetupViewModel = QuickSetupViewModel(
        profileRepository = profileRepository,
        getProfile = GetProfileUseCase(profileRepository),
        updateProfile = UpdateProfileUseCase(profileRepository),
        selectUserUniversity = SelectUserUniversityUseCase(profileRepository),
        getUniversities = GetUniversitiesUseCase(universityRepository),
    )
}

private class FakeQuickSetupProfileRepository(
    private var profile: UserProfile?,
    private val profileAfterSave: UserProfile? = null,
    private val loadError: Throwable? = null,
    private val updateError: Throwable? = null,
    private val selectError: Throwable? = null,
) : ProfileRepository {
    private val profileFlow = MutableStateFlow(profile)
    private val universityFlow = MutableStateFlow(profile?.university)
    private val quickSetupFlow = MutableStateFlow(profile?.quickSetupCompleted == true)

    var lastUpdatedName: String? = null
    var lastSelectedUniversityId: String? = null

    override val profileState: Flow<UserProfile?> = profileFlow.asStateFlow()
    override val universityState: Flow<University?> = universityFlow.asStateFlow()
    override val quickSetupState: StateFlow<Boolean> = quickSetupFlow.asStateFlow()

    override suspend fun loadProfile() {
        loadError?.let { throw it }
        publish(profile)
    }

    override suspend fun updateProfile(name: String?) {
        updateError?.let { throw it }
        lastUpdatedName = name
        profile = (profileAfterSave ?: profile)?.copy(name = name)
            ?: UserProfile(name = name, university = null, quickSetupCompleted = false)
        publish(profile)
    }

    override suspend fun selectUniversity(universityId: String?) {
        selectError?.let { throw it }
        lastSelectedUniversityId = universityId
        if (profileAfterSave != null) {
            profile = profileAfterSave
        }
        publish(profile)
    }

    private fun publish(value: UserProfile?) {
        profile = value
        profileFlow.value = value
        universityFlow.value = value?.university
        quickSetupFlow.value = value?.quickSetupCompleted == true
    }
}

private class FakeUniversityRepository(
    private val items: List<University>,
) : UniversityRepository {
    override suspend fun getUniversities(page: Int, pageSize: Int): Result<PagedResult<University>> =
        Result.success(
            PagedResult(
                items = items,
                page = page,
                pageSize = pageSize,
                totalItems = items.size.toLong(),
                totalPages = if (items.isEmpty()) 0 else 1,
            ),
        )

    override suspend fun getUniversityById(id: String): Result<University> =
        items.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(DomainException(DomainError.NotFound))

    override suspend fun searchNearbyUniversities(lat: Double, lng: Double) =
        error("no usado en QuickSetupViewModelTest")
}
