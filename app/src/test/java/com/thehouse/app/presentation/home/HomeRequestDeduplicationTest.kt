package com.thehouse.app.presentation.home

import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UnreadCount
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.model.UserSession
import com.thehouse.app.domain.repository.AuthRepository
import com.thehouse.app.domain.repository.NotificationsRepository
import com.thehouse.app.domain.repository.PropertyRepository
import com.thehouse.app.domain.repository.UniversityRepository
import com.thehouse.app.domain.usecase.GetPropertiesUseCase
import com.thehouse.app.domain.usecase.GetUniversitiesUseCase
import com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase
import com.thehouse.app.domain.usecase.LogoutUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Cuenta llamadas reales al "backend" para detectar duplicación de requests. */
private class CountingUniversityRepository : UniversityRepository {
    var calls = 0

    override suspend fun getUniversities(page: Int, pageSize: Int): Result<PagedResult<University>> {
        calls++
        return Result.success(PagedResult(emptyList(), page, pageSize, 0L, 0))
    }

    override suspend fun getUniversityById(id: String): Result<University> = error("no usado")
}

private class CountingPropertyRepository : PropertyRepository {
    var calls = 0

    override suspend fun getProperties(page: Int, pageSize: Int): Result<PagedResult<Property>> {
        calls++
        return Result.success(PagedResult(emptyList(), page, pageSize, 0L, 0))
    }

    override suspend fun getPropertyById(id: String): Result<Property> = error("no usado")
}

/**
 * Repositorio de unread count con "gate": la respuesta queda en vuelo hasta que
 * se completa, para reproducir solapamiento real de ON_START + ON_RESUME.
 */
private class CountingUnreadRepository(blocking: Boolean = false) : NotificationsRepository {
    var calls = 0
    private val gate: CompletableDeferred<Unit> =
        if (blocking) CompletableDeferred() else CompletableDeferred<Unit>().apply { complete(Unit) }

    suspend fun release() {
        gate.complete(Unit)
    }

    override suspend fun getUnreadCount(etag: String?): Result<UnreadCountFetch> {
        calls++
        gate.await()
        return Result.success(
            UnreadCountFetch.Modified(
                count = UnreadCount(unread = 3, requests = 1, visits = 1, system = 1),
                etag = "etag-1",
            ),
        )
    }

    override suspend fun getNotifications(
        unread: Boolean?,
        category: NotificationCategory?,
        page: Int,
        pageSize: Int,
    ): Result<NotificationsPage> = error("no usado")

    override suspend fun markRead(id: String): Result<Unit> = error("no usado")

    override suspend fun markAllRead(category: NotificationCategory?): Result<Unit> =
        error("no usado")
}

private class FakeAuthRepository : AuthRepository {
    private val state = MutableStateFlow<SessionState>(
        SessionState.Active(
            UserSession(userId = "u1", displayName = "Carlos", email = "c@correo.mx"),
        ),
    )

    override val session: StateFlow<SessionState> = state

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): Result<UserSession?> = Result.success(null)

    override suspend fun login(email: String, password: String): Result<UserSession?> =
        Result.success(null)

    override suspend fun logout(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshAccessToken(): Result<String?> = Result.success(null)

    override suspend fun getCurrentUser(): Result<UserSession?> = Result.success(null)

    override suspend fun restoreSession(): Result<UserSession?> = Result.success(null)
}

/**
 * Carga inicial / retry / unread (secciones 5 y 10 del hardening de runtime):
 * cada evento lógico genera UNA petición; los disparos de ciclo de vida que
 * llegan en la misma tanda no duplican el request de unread count.
 */
class HomeRequestDeduplicationTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun activeSession(): MutableStateFlow<SessionState> =
        MutableStateFlow(
            SessionState.Active(
                UserSession(userId = "u1", displayName = "Carlos", email = "c@correo.mx"),
            ),
        )

    @Test
    fun `carga inicial pide universidades y propiedades una sola vez`() = runTest {
        val universities = CountingUniversityRepository()
        val properties = CountingPropertyRepository()
        HomeViewModel(
            logoutUseCase = LogoutUseCase(FakeAuthRepository()),
            sessionState = activeSession(),
            getUniversities = GetUniversitiesUseCase(universities),
            getProperties = GetPropertiesUseCase(properties),
        )

        advanceUntilIdle()

        assertEquals(1, universities.calls)
        assertEquals(1, properties.calls)
    }

    @Test
    fun `retry explicito agrega exactamente una peticion`() = runTest {
        val universities = CountingUniversityRepository()
        val properties = CountingPropertyRepository()
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(FakeAuthRepository()),
            sessionState = activeSession(),
            getUniversities = GetUniversitiesUseCase(universities),
            getProperties = GetPropertiesUseCase(properties),
        )
        advanceUntilIdle()
        assertEquals(1, properties.calls)

        vm.onEvent(HomeEvent.RetryPropertiesClicked)
        advanceUntilIdle()

        assertEquals(2, properties.calls)
        assertEquals(1, universities.calls)
    }

    @Test
    fun `refresh de unread no duplica la peticion en la misma tanda`() = runTest {
        val universities = CountingUniversityRepository()
        val properties = CountingPropertyRepository()
        val unread = CountingUnreadRepository(blocking = true)
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(FakeAuthRepository()),
            sessionState = activeSession(),
            getUniversities = GetUniversitiesUseCase(universities),
            getProperties = GetPropertiesUseCase(properties),
            getUnreadNotificationsCount = GetUnreadNotificationsCountUseCase(unread),
        )

        // ON_START: arranca el polling y lanza la primera petición (queda en vuelo).
        vm.onEvent(HomeEvent.StartUnreadPolling)
        runCurrent()
        assertEquals(1, unread.calls)

        // ON_RESUME inmediato (misma tanda): no debe duplicar la llamada.
        vm.onEvent(HomeEvent.RefreshUnread)
        runCurrent()
        assertEquals(1, unread.calls)

        unread.release()
        vm.onEvent(HomeEvent.StopUnreadPolling)
        advanceUntilIdle()

        assertEquals(1, unread.calls)
        assertEquals(3, vm.uiState.value.unreadNotifications)
    }

    @Test
    fun `sesion anonima no consulta unread count`() = runTest {
        val unread = CountingUnreadRepository()
        val vm = HomeViewModel(
            logoutUseCase = LogoutUseCase(FakeAuthRepository()),
            sessionState = MutableStateFlow<SessionState>(SessionState.Anonymous),
            getUnreadNotificationsCount = GetUnreadNotificationsCountUseCase(unread),
        )

        vm.onEvent(HomeEvent.RefreshUnread)
        vm.onEvent(HomeEvent.StartUnreadPolling)
        advanceUntilIdle()

        assertEquals(0, unread.calls)
        assertEquals(0, vm.uiState.value.unreadNotifications)
    }
}
