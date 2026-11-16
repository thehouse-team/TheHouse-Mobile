package com.thehouse.app.presentation.notifications

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.NotificationException
import com.thehouse.app.domain.model.AppNotification
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationTarget
import com.thehouse.app.domain.model.UnreadCount
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.usecase.GetNotificationsUseCase
import com.thehouse.app.domain.usecase.GetUnreadNotificationsCountUseCase
import com.thehouse.app.domain.usecase.MarkAllNotificationsReadUseCase
import com.thehouse.app.domain.usecase.MarkNotificationReadUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEffect
import com.thehouse.app.presentation.common.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NotificationFilter(val label: String, val category: NotificationCategory?) {
    ALL("Todas", null),
    REQUESTS("Solicitudes", NotificationCategory.REQUESTS),
    VISITS("Visitas", NotificationCategory.VISITS),
    SYSTEM("Sistema", NotificationCategory.SYSTEM),
}

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isRefreshingUnread: Boolean = false,
    val items: List<AppNotification> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val page: Int = 1,
    val totalPages: Int = 0,
    val unreadCount: UnreadCount? = null,
    val errorMessage: String? = null,
    val markingId: String? = null,
    val markingAll: Boolean = false,
)

sealed interface NotificationsEvent : UiEvent {
    data object Load : NotificationsEvent
    data object Retry : NotificationsEvent
    data class FilterChanged(val filter: NotificationFilter) : NotificationsEvent
    data object LoadMore : NotificationsEvent
    data class OpenNotification(val notification: AppNotification) : NotificationsEvent
    data object MarkAllRead : NotificationsEvent
    data object RefreshUnread : NotificationsEvent
    data object StartPolling : NotificationsEvent
    data object StopPolling : NotificationsEvent
    data object ForegroundRefresh : NotificationsEvent
}

sealed interface NotificationsEffect : UiEffect {
    data class NavigateToTarget(val target: NotificationTarget) : NotificationsEffect
    data class TargetUnavailable(val message: String) : NotificationsEffect
}

class NotificationsViewModel(
    private val getNotifications: GetNotificationsUseCase,
    private val getUnreadCount: GetUnreadNotificationsCountUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    private val pollIntervalMs: Long = 30_000L,
) : BaseViewModel<NotificationsUiState, NotificationsEvent, NotificationsEffect>(
    NotificationsUiState(isLoading = true),
) {

    private var unreadEtag: String? = null
    private var pollingJob: Job? = null

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    override fun onEvent(event: NotificationsEvent) {
        when (event) {
            NotificationsEvent.Load,
            NotificationsEvent.Retry -> loadNotifications(reset = true)
            is NotificationsEvent.FilterChanged -> {
                if (uiState.value.filter != event.filter) {
                    updateState { it.copy(filter = event.filter) }
                    loadNotifications(reset = true)
                }
            }
            NotificationsEvent.LoadMore -> loadNotifications(reset = false)
            is NotificationsEvent.OpenNotification -> openNotification(event.notification)
            NotificationsEvent.MarkAllRead -> markAll()
            NotificationsEvent.RefreshUnread,
            NotificationsEvent.ForegroundRefresh -> refreshUnread()
            NotificationsEvent.StartPolling -> startPolling()
            NotificationsEvent.StopPolling -> stopPolling()
        }
    }

    private fun loadNotifications(reset: Boolean) {
        val current = uiState.value
        val nextPage = if (reset) 1 else current.page + 1
        if (!reset && (current.isLoading || (current.totalPages in 1..current.page))) return

        launch {
            updateState { it.copy(isLoading = true, errorMessage = if (reset) null else it.errorMessage) }
            val result = getNotifications(
                unread = null,
                category = current.filter.category,
                page = nextPage,
                pageSize = 20,
            )
            result.fold(
                onSuccess = { page ->
                    updateState { state ->
                        val merged = if (reset) page.items else state.items + page.items
                            .filter { item -> state.items.none { it.id == item.id } }
                        state.copy(
                            isLoading = false,
                            items = merged,
                            page = page.page,
                            totalPages = page.totalPages,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapError(throwable),
                        )
                    }
                },
            )
        }
    }

    private fun refreshUnread() {
        launch {
            try {
                val result = getUnreadCount(unreadEtag)
                result.fold(
                    onSuccess = { fetch ->
                        when (fetch) {
                            is UnreadCountFetch.NotModified -> Unit // NO modificar estado
                            is UnreadCountFetch.Modified -> {
                                unreadEtag = fetch.etag
                                updateState { it.copy(unreadCount = fetch.count) }
                            }
                        }
                    },
                    onFailure = { /* mantener estado actual en fallo de badge */ },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = launch {
            try {
                while (true) {
                    refreshUnread()
                    delay(pollIntervalMs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun openNotification(notification: AppNotification) {
        if (!notification.read) {
            markReadAndNavigate(notification)
        } else {
            navigateTo(notification)
        }
    }

    private fun markReadAndNavigate(notification: AppNotification) {
        if (uiState.value.markingId != null) return
        launch {
            updateState { it.copy(markingId = notification.id) }
            val result = markNotificationRead(notification.id)
            result.fold(
                onSuccess = {
                    updateState { state ->
                        state.copy(
                            markingId = null,
                            items = state.items.map {
                                if (it.id == notification.id) it.copy(read = true) else it
                            },
                            unreadCount = state.unreadCount?.let { count ->
                                val delta = if (notification.read) 0 else 1
                                val total = (count.unread - delta).coerceAtLeast(0)
                                when (notification.category) {
                                    NotificationCategory.REQUESTS -> count.copy(
                                        unread = total,
                                        requests = (count.requests - delta).coerceAtLeast(0),
                                    )
                                    NotificationCategory.VISITS -> count.copy(
                                        unread = total,
                                        visits = (count.visits - delta).coerceAtLeast(0),
                                    )
                                    NotificationCategory.SYSTEM -> count.copy(
                                        unread = total,
                                        system = (count.system - delta).coerceAtLeast(0),
                                    )
                                    null -> count.copy(unread = total)
                                }
                            },
                        )
                    }
                    refreshUnread()
                    navigateTo(notification)
                },
                onFailure = { throwable ->
                    updateState { it.copy(markingId = null, errorMessage = mapError(throwable)) }
                },
            )
        }
    }

    private fun navigateTo(notification: AppNotification) {
        val target = notification.target
        if (target == null) {
            sendEffect(NotificationsEffect.TargetUnavailable("Esta notificación no tiene un destino disponible."))
        } else {
            sendEffect(NotificationsEffect.NavigateToTarget(target))
        }
    }

    private fun markAll() {
        if (uiState.value.markingAll) return
        val category = uiState.value.filter.category
        launch {
            updateState { it.copy(markingAll = true) }
            val result = markAllNotificationsRead(category)
            result.fold(
                onSuccess = {
                    updateState { state ->
                        val updatedItems = state.items.map { item ->
                            if (category == null || item.category == category) item.copy(read = true) else item
                        }
                        val updatedCount = when (category) {
                            null -> UnreadCount(0, 0, 0, 0)
                            NotificationCategory.REQUESTS -> state.unreadCount?.copy(
                                unread = (state.unreadCount.unread - state.unreadCount.requests).coerceAtLeast(0),
                                requests = 0,
                            )
                            NotificationCategory.VISITS -> state.unreadCount?.copy(
                                unread = (state.unreadCount.unread - state.unreadCount.visits).coerceAtLeast(0),
                                visits = 0,
                            )
                            NotificationCategory.SYSTEM -> state.unreadCount?.copy(
                                unread = (state.unreadCount.unread - state.unreadCount.system).coerceAtLeast(0),
                                system = 0,
                            )
                        }
                        state.copy(markingAll = false, items = updatedItems, unreadCount = updatedCount)
                    }
                    refreshUnread()
                },
                onFailure = { throwable ->
                    updateState { it.copy(markingAll = false, errorMessage = mapError(throwable)) }
                },
            )
        }
    }

    private fun mapError(throwable: Throwable): String {
        val domain = (throwable as? NotificationException)?.domainError
        return when (domain) {
            DomainError.Network -> "No hay conexión. Revisa tu red e inténtalo de nuevo."
            DomainError.Timeout -> "La solicitud tardó demasiado. Inténtalo de nuevo."
            DomainError.Unauthorized -> "Tu sesión ya no es válida. Inicia sesión nuevamente."
            DomainError.NotFound -> "La notificación ya no está disponible."
            DomainError.RateLimited, DomainError.Server -> "El servicio no está disponible en este momento. Inténtalo más tarde."
            DomainError.Conflict, DomainError.Gone, DomainError.AccountSuspended,
            DomainError.Unknown, null -> "No se pudieron cargar las notificaciones. Inténtalo de nuevo."
        }
    }
}
