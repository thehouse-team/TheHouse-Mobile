package com.thehouse.app.data.requests.repository

import com.thehouse.app.data.remote.api.RequestsApi
import com.thehouse.app.data.remote.dto.requests.CreateRequestDto
import com.thehouse.app.data.remote.readProblemCode
import com.thehouse.app.data.remote.auth.AccountSuspendedIOException
import com.thehouse.app.data.remote.dto.requests.RejectRequestDto
import com.thehouse.app.data.remote.dto.requests.RequestsListResponseDto
import com.thehouse.app.data.remote.dto.requests.RoomRequestDto
import com.thehouse.app.data.requests.mapper.toDomain
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.RequestErrorCode
import com.thehouse.app.domain.error.RequestException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RoomRequest
import com.thehouse.app.domain.repository.RequestsRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class RequestsRepositoryImpl(
    private val api: RequestsApi,
    private val json: Json,
) : RequestsRepository {

    override suspend fun createRequest(
        roomId: String,
        quickQuestions: List<QuickQuestion>,
        message: String?,
    ): Result<RoomRequest> = safeCall {
        val response = api.createRequest(
            roomId = roomId,
            body = CreateRequestDto(
                quickQuestions = quickQuestions.map { it.api },
                message = message?.takeIf { it.isNotBlank() },
            ),
        )
        finish(response)
    }

    override suspend fun getRequests(
        status: RequestStatus?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomRequest>> = safeCall {
        val response = api.getUserRequests(
            status = status?.name?.lowercase(),
            page = page,
            pageSize = pageSize,
        )
        response.toPageResult()
    }

    override suspend fun getRequest(id: String): Result<RoomRequest> = safeCall {
        finish(api.getUserRequest(id))
    }

    override suspend fun cancelRequest(id: String): Result<RoomRequest> = safeCall {
        finish(api.cancelRequest(id))
    }

    override suspend fun getLandlordRequests(
        status: RequestStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoomRequest>> = safeCall {
        api.getLandlordRequests(
            status = status?.name?.lowercase(),
            roomId = roomId?.takeIf { it.isNotBlank() },
            page = page,
            pageSize = pageSize,
        ).toPageResult()
    }

    override suspend fun getLandlordRequest(id: String): Result<RoomRequest> = safeCall {
        finish(api.getLandlordRequest(id))
    }

    override suspend fun acceptRequest(id: String): Result<RoomRequest> = safeCall {
        finish(api.acceptRequest(id))
    }

    override suspend fun rejectRequest(id: String, reason: String, note: String?): Result<RoomRequest> =
        safeCall {
            finish(
                api.rejectRequest(
                    id = id,
                    body = RejectRequestDto(reason = reason, note = note?.takeIf { it.isNotBlank() }),
                ),
            )
        }

    private suspend fun finish(response: Response<RoomRequestDto>): Result<RoomRequest> {
        val body = response.body()
        if (response.isSuccessful && body != null) {
            return Result.success(body.toDomain())
        }
        return Result.failure(errorFor(response))
    }

    private suspend fun Response<RequestsListResponseDto>.toPageResult(): Result<PagedResult<RoomRequest>> {
        val body = this.body()
        if (isSuccessful && body != null) {
            return Result.success(
                PagedResult(
                    items = body.items.map { it.toDomain() },
                    page = body.page,
                    pageSize = body.pageSize,
                    totalItems = body.totalItems,
                    totalPages = body.totalPages,
                ),
            )
        }
        return Result.failure(errorFor(this))
    }

    private fun errorFor(response: Response<*>): RequestException {
        val rawProblemCode = response.readProblemCode(json)
        val code = RequestErrorCode.fromApi(rawProblemCode)
        val domainError = when {
            response.code() == 403 && rawProblemCode == "account_suspended" ->
                DomainError.AccountSuspended
            response.code() == 401 || response.code() == 403 -> DomainError.Unauthorized
            response.code() == 404 -> DomainError.NotFound
            response.code() == 409 -> DomainError.Conflict
            response.code() == 422 -> DomainError.Unknown
            response.code() == 408 -> DomainError.Timeout
            response.code() == 429 -> DomainError.RateLimited
            response.code() in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        return RequestException(domainError, code, retryAfter)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (suspended: AccountSuspendedIOException) {
        Result.failure(RequestException(DomainError.AccountSuspended))
    } catch (timeout: SocketTimeoutException) {
        Result.failure(RequestException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(RequestException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(RequestException(DomainError.Unknown))
    }
}
