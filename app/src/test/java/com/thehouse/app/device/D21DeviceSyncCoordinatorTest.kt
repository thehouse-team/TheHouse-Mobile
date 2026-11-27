package com.thehouse.app.device

import com.thehouse.app.domain.connectivity.ConnectivityObserver
import com.thehouse.app.domain.device.DeviceError
import com.thehouse.app.domain.device.DeviceRegistrationResult
import com.thehouse.app.domain.device.DeviceSyncCoordinator
import com.thehouse.app.domain.device.DeviceSyncStatus
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.device.UnregisterOutcome
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.DeviceException
import com.thehouse.app.domain.repository.DeviceRepository
import com.thehouse.app.domain.repository.InstallationIdStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class D21DeviceSyncCoordinatorTest {

    private val id = InstallationId("6aef1234-1111-2222-3333-444455556666")
    private val session = UserSession("user-A", "A", "a@test.mx")

    private class FakeIdStore(private val id: InstallationId) : InstallationIdStore {
        override suspend fun getOrCreate(): InstallationId = id
    }

    private class FakeConnectivity : ConnectivityObserver {
        override val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
    }

    private class FakeRepository(
        var registerResult: Result<DeviceRegistrationResult> = Result.success(DeviceRegistrationResult.Created),
        var unregisterResult: Result<Unit> = Result.success(Unit),
        val calls: MutableList<String> = mutableListOf(),
    ) : DeviceRepository {
        override suspend fun register(
            installationId: InstallationId, fcmToken: String, platform: String,
            appVersion: String?, locale: String?,
        ): Result<DeviceRegistrationResult> {
            calls.add("register:${installationId.value}:$fcmToken")
            return registerResult
        }

        override suspend fun unregister(installationId: InstallationId): Result<Unit> {
            calls.add("unregister:${installationId.value}")
            return unregisterResult
        }

        override suspend fun list() = Result.success(emptyList<com.thehouse.app.domain.device.RegisteredDevice>())
    }

    private fun coordinator(
        repository: FakeRepository,
        state: SessionState = SessionState.Active(session),
        token: String? = "fake-token-for-http-test-0123456789",
    ): Pair<DeviceSyncCoordinator, () -> String?> {
        var currentToken: String? = token
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        val coordinator = DeviceSyncCoordinator(
            repository = repository,
            installationIdStore = FakeIdStore(id),
            sessionState = MutableStateFlow(state),
            connectivity = FakeConnectivity(),
            appScope = scope,
            tokenProvider = { currentToken },
            tokenClear = { currentToken = null },
        )
        return coordinator to { currentToken }
    }

    @Test
    fun guest_isNeverRegistered() = runTest {
        val repo = FakeRepository()
        val (coordinator, _) = coordinator(repo, state = SessionState.Anonymous)
        assertEquals(DeviceSyncStatus.SkippedGuest, coordinator.syncIfPossible())
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun noToken_isNeverRegistered() = runTest {
        val repo = FakeRepository()
        val (coordinator, _) = coordinator(repo, token = null)
        assertEquals(DeviceSyncStatus.SkippedNoToken, coordinator.syncIfPossible())
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun repeatedSyncForSameAccountAndToken_isDeduplicated() = runTest {
        val repo = FakeRepository()
        val (coordinator, _) = coordinator(repo)
        coordinator.syncIfPossible()
        coordinator.syncIfPossible()
        assertEquals(1, repo.calls.size)
    }

    @Test
    fun newToken_triggersNewRegistration() = runTest {
        val repo = FakeRepository()
        var currentToken: String? = "fake-token-for-http-test-AAA"
        val coordinator = DeviceSyncCoordinator(
            repository = repo,
            installationIdStore = FakeIdStore(id),
            sessionState = MutableStateFlow(SessionState.Active(session)),
            connectivity = FakeConnectivity(),
            appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
            tokenProvider = { currentToken },
            tokenClear = { currentToken = null },
        )
        coordinator.syncIfPossible()
        currentToken = "fake-token-for-http-test-BBB"
        coordinator.syncIfPossible()
        assertEquals(2, repo.calls.size)
    }

    @Test
    fun logout_204_mapsToRemoteDeleted() = runTest {
        val repo = FakeRepository(unregisterResult = Result.success(Unit))
        val (coordinator, _) = coordinator(repo)
        assertEquals(UnregisterOutcome.RemoteDeleted, coordinator.unregisterOnLogout())
        assertEquals(1, repo.calls.size)
    }

    @Test
    fun logout_404_mapsToAlreadyAbsent() = runTest {
        val repo = FakeRepository(
            unregisterResult = Result.failure(DeviceException(DeviceError.NotFound)),
        )
        val (coordinator, _) = coordinator(repo)
        assertEquals(UnregisterOutcome.AlreadyAbsent, coordinator.unregisterOnLogout())
    }

    @Test
    fun logout_networkFailure_mapsToRemoteFailure_notDeleted() = runTest {
        val repo = FakeRepository(
            unregisterResult = Result.failure(DeviceException(DeviceError.Network)),
        )
        val (coordinator, _) = coordinator(repo)
        assertEquals(UnregisterOutcome.RemoteFailure, coordinator.unregisterOnLogout())
    }

    @Test
    fun logout_onGuest_isLocalOnly() = runTest {
        val repo = FakeRepository()
        val (coordinator, _) = coordinator(repo, state = SessionState.Anonymous)
        assertEquals(UnregisterOutcome.LocalOnly, coordinator.unregisterOnLogout())
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun accountSwitch_clearsTokenAndPendingWork() = runTest {
        val repo = FakeRepository()
        val (coordinator, readToken) = coordinator(repo)
        coordinator.syncIfPossible()
        coordinator.clearForAccountSwitch()
        assertNull(readToken())
        assertNull(coordinator.currentAccount())
    }

    @Test
    fun networkFailure_doesNotReportRegistered() = runTest {
        val repo = FakeRepository(
            registerResult = Result.failure(DeviceException(DeviceError.Network)),
        )
        val (coordinator, _) = coordinator(repo)
        coordinator.syncIfPossible()
        assertEquals(1, repo.calls.size)
    }
}
