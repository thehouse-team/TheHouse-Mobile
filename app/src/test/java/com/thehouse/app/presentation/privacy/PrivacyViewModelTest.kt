package com.thehouse.app.presentation.privacy

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AccountRepository
import com.thehouse.app.domain.repository.LocalUserDataPurger
import com.thehouse.app.domain.usecase.DeleteAccountUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val activeUser = UserSession(
    userId = "u-15",
    displayName = "Estudiante",
    email = "estudiante@correo.mx",
)

private class FakeAccountRepository(
    override var isDeletionAvailable: Boolean,
    var result: Result<Unit> = Result.success(Unit),
    private val beforeDelete: suspend () -> Unit = {},
) : AccountRepository {

    var deleteCalls = 0

    override suspend fun deleteAccount(): Result<Unit> {
        deleteCalls += 1
        beforeDelete()
        return result
    }
}

private class RecordingPurger(
    private val onPurge: (String?) -> Unit = {},
) : LocalUserDataPurger {

    val userIds = mutableListOf<String?>()

    override suspend fun purge(userId: String?) {
        userIds += userId
        onPurge(userId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun TestScope.viewModel(
        privacyPolicyUrl: String? = null,
        session: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.Active(activeUser)),
        accountRepository: FakeAccountRepository = FakeAccountRepository(isDeletionAvailable = false),
        purger: RecordingPurger = RecordingPurger(),
    ): PrivacyViewModel = PrivacyViewModel(
        privacyPolicyUrl = privacyPolicyUrl,
        sessionState = session,
        deleteAccount = DeleteAccountUseCase(accountRepository, purger, session),
    )

    /** D19: rellena password y confirmación explícita antes de confirmar. */
    private fun fillDeleteForm(viewModel: PrivacyViewModel) {
        viewModel.onEvent(PrivacyEvent.DeletePasswordChanged("clave-correcta"))
        viewModel.onEvent(PrivacyEvent.DeleteConfirmationChanged(PrivacyViewModel.CONFIRMATION))
    }

    private fun TestScope.recordEffects(viewModel: PrivacyViewModel): Pair<MutableList<PrivacyEffect>, Job> {
        val effects = mutableListOf<PrivacyEffect>()
        val job = backgroundScope.launch {
            viewModel.effects.collect { effects += it }
        }
        return effects to job
    }

    @Test
    fun `missing url shows blocked notice without opening anything`() = runTest {
        val viewModel = viewModel(privacyPolicyUrl = null)
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.OpenPrivacyPolicyClicked)
        runCurrent()

        assertNull(viewModel.uiState.value.privacyPolicyUrl)
        assertEquals(PrivacyViewModel.MESSAGE_PRIVACY_BLOCKED, viewModel.uiState.value.info)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `invalid url is not exposed and never opens`() = runTest {
        val viewModel = viewModel(privacyPolicyUrl = "http://example.com/privacidad")
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.OpenPrivacyPolicyClicked)
        runCurrent()

        assertNull(viewModel.uiState.value.privacyPolicyUrl)
        assertEquals(PrivacyViewModel.MESSAGE_PRIVACY_BLOCKED, viewModel.uiState.value.info)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `valid url is normalized and opened through an effect`() = runTest {
        val viewModel = viewModel(privacyPolicyUrl = "  https://example.com/privacidad  ")
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.OpenPrivacyPolicyClicked)
        runCurrent()

        assertEquals("https://example.com/privacidad", viewModel.uiState.value.privacyPolicyUrl)
        assertEquals(
            listOf<PrivacyEffect>(PrivacyEffect.OpenPrivacyPolicy("https://example.com/privacidad")),
            effects,
        )
    }

    @Test
    fun `guest deletion requires login and opens no confirmation`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Anonymous)
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val viewModel = viewModel(
            session = session,
            accountRepository = repository,
        )
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(listOf<PrivacyEffect>(PrivacyEffect.RequireLogin), effects)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun `blocked deletion explains the state and never opens a fake dialog`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = false)
        val purger = RecordingPurger()
        val viewModel = viewModel(accountRepository = repository, purger = purger)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(PrivacyViewModel.MESSAGE_DELETE_BLOCKED, viewModel.uiState.value.info)
        assertEquals(0, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
    }

    @Test
    fun `cancel closes confirmation without calling backend`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger()
        val viewModel = viewModel(accountRepository = repository, purger = purger)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.onEvent(PrivacyEvent.CancelDeleteClicked)
        runCurrent()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(0, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
    }

    @Test
    fun `loading prevents double submit and sends exactly one request`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAccountRepository(
            isDeletionAvailable = true,
            beforeDelete = { gate.await() },
        )
        val purger = RecordingPurger()
        val viewModel = viewModel(accountRepository = repository, purger = purger)
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        fillDeleteForm(viewModel)
        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()

        assertTrue(viewModel.uiState.value.isDeleting)
        assertEquals(1, repository.deleteCalls)

        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()
        assertEquals(1, repository.deleteCalls)

        gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals(listOf<String?>("u-15"), purger.userIds)
        assertEquals(listOf<PrivacyEffect>(PrivacyEffect.AccountDeleted), effects)
    }

    @Test
    fun `success clears the local session and navigates out`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Active(activeUser))
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger(onPurge = { session.value = SessionState.Anonymous })
        val viewModel = viewModel(
            session = session,
            accountRepository = repository,
            purger = purger,
        )
        val (effects, _) = recordEffects(viewModel)
        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        fillDeleteForm(viewModel)
        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()

        assertEquals(SessionState.Anonymous, session.value)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(listOf<String?>("u-15"), purger.userIds)
        assertEquals(listOf<PrivacyEffect>(PrivacyEffect.AccountDeleted), effects)
    }

    @Test
    fun `backend failure preserves session and local data`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Active(activeUser))
        val repository = FakeAccountRepository(
            isDeletionAvailable = true,
            result = Result.failure(DomainException(DomainError.Network)),
        )
        val purger = RecordingPurger()
        val viewModel = viewModel(
            session = session,
            accountRepository = repository,
            purger = purger,
        )
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        fillDeleteForm(viewModel)
        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()

        assertEquals(SessionState.Active(activeUser), session.value)
        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(
            "No hay conexión. Verifica tu red e intenta de nuevo.",
            viewModel.uiState.value.error,
        )
        assertEquals(1, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `backend unavailable after opening the dialog deletes nothing`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger()
        val viewModel = viewModel(accountRepository = repository, purger = purger)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        repository.isDeletionAvailable = false
        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()

        assertEquals(PrivacyViewModel.MESSAGE_DELETE_BLOCKED, viewModel.uiState.value.info)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(0, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
    }

    @Test
    fun `unauthorized error is safe and keeps the session`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Active(activeUser))
        val repository = FakeAccountRepository(
            isDeletionAvailable = true,
            result = Result.failure(DomainException(DomainError.Unauthorized)),
        )
        val purger = RecordingPurger()
        val viewModel = viewModel(
            session = session,
            accountRepository = repository,
            purger = purger,
        )
        val (effects, _) = recordEffects(viewModel)

        viewModel.onEvent(PrivacyEvent.DeleteAccountClicked)
        runCurrent()
        fillDeleteForm(viewModel)
        viewModel.onEvent(PrivacyEvent.ConfirmDeleteClicked)
        runCurrent()

        assertEquals(
            "Tu sesión expiró. Inicia sesión de nuevo.",
            viewModel.uiState.value.error,
        )
        assertEquals(SessionState.Active(activeUser), session.value)
        assertTrue(effects.isEmpty())
        assertTrue(purger.userIds.isEmpty())
    }
}
