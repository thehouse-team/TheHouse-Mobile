package com.thehouse.app.presentation.notifications

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.NotificationException
import com.thehouse.app.domain.model.AppNotification
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.NotificationTargetKind
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.UnreadCount
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.repository.NotificationsRepository
import com.thehouse.app.domain.usecase.GetNotificationsUseCase
import com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase
import com.thehouse.app.domain.usecase.MarkAllNotificationsReadUseCase
import com.thehouse.app.domain.usecase.MarkNotificationReadUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(StandardTestDispatcher())

    private fun notification(
        id: String,
        read: Boolean = false,
        category: NotificationCategory = NotificationCategory.REQUESTS,
        kind: NotificationTargetKind = NotificationTargetKind.REQUEST,
        targetId: String = "t-$id",
        count: Int = 1,
    ) = AppNotification(
        id = id,
        type = "test",
        category = category,
        title = "Title $id",
        body = "Body $id",
        target = NotificationTarget(kind, targetId),
        count = count,
        read = read,
        createdAt = Instant.parse("2026-11-12T12:00:00Z"),
        updatedAt = Instant.parse("2026-11-12T12:00:00Z"),
    )

    private class FakeNotificationsRepository : NotificationsRepository {
        val unreadResults = ArrayDeque<Result<UnreadCountFetch>>()
        val listResults = ArrayDeque<Result<NotificationsPage>>()
        val readCalls = mutableListOf<String>()
        val readAllCalls = mutableListOf<NotificationCategory?>()
        var readResult: Result<Unit> = Result.success(Unit)
        var readAllResult: Result<Unit> = Result.success(Unit)
        val unreadEtags = mutableListOf<String?>()

        override suspend fun getNotifications(
            unread: Boolean?,
            category: NotificationCategory?,
            page: Int,
            pageSize: Int,
        ): Result<NotificationsPage> = listResults.removeFirstOrNull()
            ?: Result.success(NotificationsPage(emptyList(), page, pageSize, 0, 0))

        override suspend fun getUnreadCount(etag: String?): Result<UnreadCountFetch> {
            unreadEtags.add(etag)
            return unreadResults.removeFirstOrNull()
                ?: Result.success(UnreadCountFetch.NotModified)
        }

        override suspend fun markRead(id: String): Result<Unit> {
            readCalls.add(id)
            return readResult
        }

        override suspend fun markAllRead(category: NotificationCategory?): Result<Unit> {
            readAllCalls.add(category)
            return readAllResult
        }
    }

    private fun vm(repo: FakeNotificationsRepository, pollMs: Long = 30_000L) =
        NotificationsViewModel(
            getNotifications = GetNotificationsUseCase(repo),
            getUnreadCount = GetUnreadNotificationsCountUseCase(repo),
            markNotificationRead = MarkNotificationReadUseCase(repo),
            markAllNotificationsRead = MarkAllNotificationsReadUseCase(repo),
            pollIntervalMs = pollMs,
        )

    @Test
    fun `load fetches notifications`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            listResults.add(
                Result.success(
                    NotificationsPage(
                        items = listOf(notification("n-1", count = 2)),
                        page = 1,
                        pageSize = 20,
                        totalItems = 1,
                        totalPages = 1,
                    ),
                ),
            )
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.Load)
        runCurrent()

        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(2, vm.uiState.value.items.first().count)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `unread count 200 updates state and stores etag`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            unreadResults.add(
                Result.success(
                    UnreadCountFetch.Modified(
                        UnreadCount(unread = 5, requests = 2, visits = 1, system = 2),
                        etag = "\"e1\"",
                    ),
                ),
            )
            unreadResults.add(Result.success(UnreadCountFetch.NotModified))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.RefreshUnread)
        runCurrent()
        assertEquals(5, vm.uiState.value.unreadCount?.unread)

        vm.onEvent(NotificationsEvent.RefreshUnread)
        runCurrent()
        // 304 mantiene estado
        assertEquals(5, vm.uiState.value.unreadCount?.unread)
        assertEquals("\"e1\"", repo.unreadEtags.last())
    }

    @Test
    fun `304 keeps previous unread count`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            unreadResults.add(
                Result.success(
                    UnreadCountFetch.Modified(UnreadCount(3, 1, 1, 1), "\"e1\""),
                ),
            )
            unreadResults.add(Result.success(UnreadCountFetch.NotModified))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.RefreshUnread)
        runCurrent()
        vm.onEvent(NotificationsEvent.RefreshUnread)
        runCurrent()
        assertEquals(3, vm.uiState.value.unreadCount?.unread)
    }

    @Test
    fun `polling runs every interval and stops`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            unreadResults.add(Result.success(UnreadCountFetch.Modified(UnreadCount(1, 1, 0, 0), "e1")))
            unreadResults.add(Result.success(UnreadCountFetch.NotModified))
            unreadResults.add(Result.success(UnreadCountFetch.NotModified))
        }
        val vm = vm(repo, pollMs = 30_000L)
        vm.onEvent(NotificationsEvent.StartPolling)
        runCurrent() // immediate refresh
        assertEquals(1, repo.unreadEtags.size)

        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(2, repo.unreadEtags.size)

        vm.onEvent(NotificationsEvent.StopPolling)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(2, repo.unreadEtags.size)
    }

    @Test
    fun `foreground refresh triggers unread fetch`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            unreadResults.add(Result.success(UnreadCountFetch.Modified(UnreadCount(4, 4, 0, 0), "e1")))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.ForegroundRefresh)
        runCurrent()
        assertEquals(4, vm.uiState.value.unreadCount?.unread)
    }

    @Test
    fun `open unread notification marks read then navigates`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            listResults.add(
                Result.success(
                    NotificationsPage(
                        listOf(notification("n-1", read = false, kind = NotificationTargetKind.REQUEST, targetId = "req-9")),
                        1, 20, 1, 1,
                    ),
                ),
            )
            unreadResults.add(Result.success(UnreadCountFetch.Modified(UnreadCount(1, 1, 0, 0), "e2")))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.Load)
        runCurrent()

        val effects = mutableListOf<NotificationsEffect>()
        val job = backgroundScope.launch {
            vm.effects.collect { effects.add(it) }
        }
        vm.onEvent(NotificationsEvent.OpenNotification(vm.uiState.value.items.first()))
        runCurrent()
        job.cancel()

        assertEquals(listOf("n-1"), repo.readCalls)
        assertTrue(vm.uiState.value.items.first().read)
        assertTrue(effects.any { it is NotificationsEffect.NavigateToTarget && it.target.id == "req-9" })
    }

    @Test
    fun `mark all read by current category calls category`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            listResults.add(Result.success(NotificationsPage(emptyList(), 1, 20, 0, 0)))
            unreadResults.add(Result.success(UnreadCountFetch.Modified(UnreadCount(0, 0, 0, 0), "e3")))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.FilterChanged(NotificationFilter.VISITS))
        runCurrent()
        vm.onEvent(NotificationsEvent.MarkAllRead)
        runCurrent()
        assertEquals(listOf(NotificationCategory.VISITS), repo.readAllCalls)
    }

    @Test
    fun `target kinds are preserved for navigation`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            listResults.add(
                Result.success(
                    NotificationsPage(
                        listOf(
                            notification("n1", read = true, kind = NotificationTargetKind.CONVERSATION, targetId = "c1"),
                            notification("n2", read = true, kind = NotificationTargetKind.VISIT, targetId = "v1"),
                            notification("n3", read = true, kind = NotificationTargetKind.AGREEMENT, targetId = "a1"),
                            notification("n4", read = true, kind = NotificationTargetKind.RENTAL, targetId = "r1"),
                            notification("n5", read = true, kind = NotificationTargetKind.BUILDING, targetId = "b1"),
                        ),
                        1, 20, 5, 1,
                    ),
                ),
            )
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.Load)
        runCurrent()

        val effects = mutableListOf<NotificationsEffect>()
        val job = backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        vm.uiState.value.items.forEach { vm.onEvent(NotificationsEvent.OpenNotification(it)) }
        runCurrent()
        job.cancel()

        val targets = effects.filterIsInstance<NotificationsEffect.NavigateToTarget>().map { it.target.kind }
        assertTrue(targets.contains(NotificationTargetKind.CONVERSATION))
        assertTrue(targets.contains(NotificationTargetKind.VISIT))
        assertTrue(targets.contains(NotificationTargetKind.AGREEMENT))
        assertTrue(targets.contains(NotificationTargetKind.RENTAL))
        assertTrue(targets.contains(NotificationTargetKind.BUILDING))
    }

    @Test
    fun `mark read 404 surfaces friendly error`() = runTest {
        val repo = FakeNotificationsRepository().apply {
            listResults.add(
                Result.success(NotificationsPage(listOf(notification("n-404", read = false)), 1, 20, 1, 1)),
            )
            readResult = Result.failure(NotificationException(DomainError.NotFound))
        }
        val vm = vm(repo)
        vm.onEvent(NotificationsEvent.Load)
        runCurrent()
        vm.onEvent(NotificationsEvent.OpenNotification(vm.uiState.value.items.first()))
        runCurrent()
        assertTrue(vm.uiState.value.errorMessage?.contains("ya no está disponible") == true)
    }
}
