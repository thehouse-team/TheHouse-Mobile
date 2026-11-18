package com.thehouse.app.domain.account

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AccountRepository
import com.thehouse.app.domain.repository.LocalUserDataPurger
import com.thehouse.app.domain.usecase.AccountDeletionResult
import com.thehouse.app.domain.usecase.DeleteAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val activeSession = UserSession(
    userId = "u-15",
    displayName = "Estudiante",
    email = "estudiante@correo.mx",
)

private class FakeAccountRepository(
    override var isDeletionAvailable: Boolean,
    var result: Result<Unit> = Result.success(Unit),
) : AccountRepository {

    var deleteCalls = 0

    override suspend fun deleteAccount(): Result<Unit> {
        deleteCalls += 1
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

class DeleteAccountUseCaseTest {

    private fun sessionFlow(state: SessionState): MutableStateFlow<SessionState> =
        MutableStateFlow(state)

    @Test
    fun `unavailable contract never calls backend or cleans data`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = false)
        val purger = RecordingPurger()
        val session = sessionFlow(SessionState.Active(activeSession))
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.BackendUnavailable, result)
        assertEquals(0, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
        assertEquals(SessionState.Active(activeSession), session.value)
    }

    @Test
    fun `confirmed deletion purges the active user and reports success`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger()
        val session = sessionFlow(SessionState.Active(activeSession))
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.Deleted, result)
        assertEquals(1, repository.deleteCalls)
        assertEquals(listOf<String?>("u-15"), purger.userIds)
    }

    @Test
    fun `confirmed deletion purges null when identity is unknown`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger()
        val session = sessionFlow(SessionState.Anonymous)
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.Deleted, result)
        assertEquals(listOf<String?>(null), purger.userIds)
    }

    @Test
    fun `backend failure preserves local data and maps domain error`() = runTest {
        val repository = FakeAccountRepository(
            isDeletionAvailable = true,
            result = Result.failure(DomainException(DomainError.Network)),
        )
        val purger = RecordingPurger()
        val session = sessionFlow(SessionState.Active(activeSession))
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.Failed(DomainError.Network), result)
        assertEquals(1, repository.deleteCalls)
        assertTrue(purger.userIds.isEmpty())
        assertEquals(SessionState.Active(activeSession), session.value)
    }

    @Test
    fun `unexpected failure never leaks exception details`() = runTest {
        val repository = FakeAccountRepository(
            isDeletionAvailable = true,
            result = Result.failure(IllegalStateException("raw-body-secret")),
        )
        val purger = RecordingPurger()
        val session = sessionFlow(SessionState.Active(activeSession))
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.Failed(DomainError.Unknown), result)
        assertTrue(purger.userIds.isEmpty())
    }

    @Test
    fun `local purge failure does not hide confirmed backend deletion`() = runTest {
        val repository = FakeAccountRepository(isDeletionAvailable = true)
        val purger = RecordingPurger(onPurge = { error("disk unavailable") })
        val session = sessionFlow(SessionState.Active(activeSession))
        val useCase = DeleteAccountUseCase(repository, purger, session as StateFlow<SessionState>)

        val result = useCase()

        assertEquals(AccountDeletionResult.Deleted, result)
        assertEquals(1, repository.deleteCalls)
        assertEquals(listOf<String?>("u-15"), purger.userIds)
    }
}
