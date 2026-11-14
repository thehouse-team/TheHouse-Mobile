package com.thehouse.app.data.notifications.repository

import com.thehouse.app.data.notifications.mapper.toDomain
import com.thehouse.app.data.remote.api.NotificationsApi
import com.thehouse.app.data.remote.auth.AccountSuspendedIOException
import com.thehouse.app.data.remote.dto.notifications.ReadAllNotificationsDto
import com.thehouse.app.data.remote.readProblemCode
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.NotificationException
import com.thehouse.app.domain.model.NotificationCategory
import com.thehouse.app.domain.model.NotificationsPage
import com.thehouse.app.domain.model.UnreadCountFetch
import com.thehouse.app.domain.repository.NotificationsRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class NotificationsRepositoryImpl(
    private val api: NotificationsApi,
    private val json: Json,
) : NotificationsRepository {

    override suspend fun getNotifications(
        unread: Boolean?,
        category: NotificationCategory?,
        page: Int,
        pageSize: Int,
    ): Result<NotificationsPage> = safeCall {
        val response = api.getNotifications(
            unread = unread,
            category = category?.api,
            page = page,
            pageSize = pageSize,
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getUnreadCount(etag: String?): Result<UnreadCountFetch> = safeCall {
        val response = api.getUnreadCount(ifNoneMatch = etag?.takeIf { it.isNotBlank() })
        when {
            response.code() == 304 -> Result.success(UnreadCountFetch.NotModified)
            else -> {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(
                        UnreadCountFetch.Modified(
                            count = body.toDomain(),
                            etag = response.headers()["ETag"],
                        ),
                    )
                } else {
                    Result.failure(errorFor(response))
                }
            }
        }
    }

    override suspend fun markRead(id: String): Result<Unit> = safeCall {
        val response = api.markRead(id)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun markAllRead(category: NotificationCategory?): Result<Unit> = safeCall {
        val response = api.markAllRead(ReadAllNotificationsDto(category = category?.api))
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(errorFor(response))
        }
    }

    private fun errorFor(response: Response<*>): NotificationException {
        val rawProblemCode = runCatching { response.readProblemCode(json) }.getOrNull()
        val domainError = when {
            response.code() == 403 && rawProblemCode == "account_suspended" ->
                DomainError.AccountSuspended
            response.code() == 401 || response.code() == 403 -> DomainError.Unauthorized
            response.code() == 404 -> DomainError.NotFound
            response.code() == 409 -> DomainError.Conflict
            response.code() == 408 -> DomainError.Timeout
            response.code() == 429 -> DomainError.RateLimited
            response.code() in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        return NotificationException(domainError)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: AccountSuspendedIOException) {
        Result.failure(NotificationException(DomainError.AccountSuspended))
    } catch (_: SocketTimeoutException) {
        Result.failure(NotificationException(DomainError.Timeout))
    } catch (_: IOException) {
        Result.failure(NotificationException(DomainError.Network))
    } catch (_: Exception) {
        Result.failure(NotificationException(DomainError.Unknown))
    }
}
