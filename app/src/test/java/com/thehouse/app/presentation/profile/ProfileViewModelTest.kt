package com.thehouse.app.presentation.profile

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import com.thehouse.app.domain.model.UserSession
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
class ProfileViewModelTest {

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
    fun `load success exposes profile email and universities`() = runTest {
        val profile = UserProfile(
            name = "Ana",
            university = University("u1", "BUAP", "BUAP", "Puebla", "MX", ""),
            quickSetupCompleted = true,
        )
        val profileRepo = FakeProfileRepository(profile = profile)
        val universitiesRepo = FakeUniversityRepository(
            listOf(
                University("u1", "BUAP", "BUAP", "Puebla", "MX", ""),
                University("u2", "UDLAP", "UDLAP", "Cholula", "MX", ""),
            ),
        )
        val vm = createViewModel(profileRepo, universitiesRepo)

        vm.onEvent(ProfileEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Ana", state.profile?.name)
        assertEquals("ana@test.com", state.email)
        assertEquals(2, state.universities.size)
        assertTrue(state.quickSetupCompleted)
    }

    @Test
    fun `load error sets safe error state`() = runTest {
        val profileRepo = FakeProfileRepository(
            profile = null,
            loadError = DomainException(DomainError.Network),
        )
        val vm = createViewModel(profileRepo, FakeUniversityRepository(emptyList()))

        vm.onEvent(ProfileEvent.Load)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No hay conexión. Verifica tu red e intenta de nuevo.", state.error)
        assertNull(state.profile)
    }

    @Test
    fun `save name updates profile and shows success message`() = runTest {
        val profileRepo = FakeProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
        )
        val vm = createViewModel(profileRepo, FakeUniversityRepository(emptyList()))
        vm.onEvent(ProfileEvent.Load)
        advanceUntilIdle()

        vm.onEvent(ProfileEvent.EditName)
        vm.onEvent(ProfileEvent.NameChanged("  Ana Maria  "))
        vm.onEvent(ProfileEvent.SaveName)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSavingName)
        assertFalse(state.isEditingName)
        assertEquals("Ana Maria", state.profile?.name)
        assertEquals("Perfil actualizado.", state.message)
        assertEquals("Ana Maria", profileRepo.lastUpdatedName)
    }

    @Test
    fun `save name error keeps edit state and shows action error`() = runTest {
        val profileRepo = FakeProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
            updateError = DomainException(DomainError.Server),
        )
        val vm = createViewModel(profileRepo, FakeUniversityRepository(emptyList()))
        vm.onEvent(ProfileEvent.Load)
        advanceUntilIdle()

        vm.onEvent(ProfileEvent.EditName)
        vm.onEvent(ProfileEvent.NameChanged("Nuevo"))
        vm.onEvent(ProfileEvent.SaveName)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSavingName)
        assertTrue(state.isEditingName)
        assertEquals("El servicio no está disponible ahora. Intenta más tarde.", state.actionError)
    }

    @Test
    fun `select university updates profile and closes picker`() = runTest {
        val updatedUniversity = University("u2", "UDLAP", "UDLAP", "Cholula", "MX", "")
        val profileRepo = FakeProfileRepository(
            profile = UserProfile(name = "Ana", university = null, quickSetupCompleted = false),
            profileAfterUniversitySelection = UserProfile(
                name = "Ana",
                university = updatedUniversity,
                quickSetupCompleted = true,
            ),
        )
        val vm = createViewModel(profileRepo, FakeUniversityRepository(listOf(updatedUniversity)))
        vm.onEvent(ProfileEvent.Load)
        advanceUntilIdle()

        vm.onEvent(ProfileEvent.OpenUniversityPicker)
        vm.onEvent(ProfileEvent.SelectUniversity("u2"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.showUniversityPicker)
        assertEquals("u2", state.profile?.university?.id)
        assertEquals("Universidad actualizada.", state.message)
    }

    private fun createViewModel(
        profileRepository: FakeProfileRepository,
        universityRepository: FakeUniversityRepository,
    ): ProfileViewModel {
        val session = MutableStateFlow<SessionState>(
            SessionState.Active(
                UserSession(userId = "u1", displayName = "Ana", email = "ana@test.com"),
            ),
        )
        return ProfileViewModel(
            profileRepository = profileRepository,
            getProfile = GetProfileUseCase(profileRepository),
            updateProfile = UpdateProfileUseCase(profileRepository),
            selectUserUniversity = SelectUserUniversityUseCase(profileRepository),
            getUniversities = GetUniversitiesUseCase(universityRepository),
            sessionState = session,
        )
    }
}

private class FakeProfileRepository(
    private var profile: UserProfile?,
    private val profileAfterUniversitySelection: UserProfile? = null,
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
        profile = profile?.copy(name = name) ?: UserProfile(name = name, university = null, quickSetupCompleted = false)
        publish(profile)
    }

    override suspend fun selectUniversity(universityId: String?) {
        selectError?.let { throw it }
        lastSelectedUniversityId = universityId
        profile = profileAfterUniversitySelection ?: profile?.copy(
            university = profile?.university?.copy(id = universityId.orEmpty()) ?: profile?.university,
        )
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
    private val failure: Throwable? = null,
) : UniversityRepository {
    override suspend fun getUniversities(page: Int, pageSize: Int): Result<PagedResult<University>> {
        failure?.let { return Result.failure(it) }
        return Result.success(
            PagedResult(
                items = items,
                page = page,
                pageSize = pageSize,
                totalItems = items.size.toLong(),
                totalPages = if (items.isEmpty()) 0 else 1,
            ),
        )
    }

    override suspend fun getUniversityById(id: String): Result<University> =
        items.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(DomainException(DomainError.NotFound))

    override suspend fun searchNearbyUniversities(lat: Double, lng: Double) =
        error("no usado en ProfileViewModelTest")
}
