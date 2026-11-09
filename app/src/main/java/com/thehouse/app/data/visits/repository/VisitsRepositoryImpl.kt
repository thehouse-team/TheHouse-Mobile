package com.thehouse.app.data.visits.repository

import com.thehouse.app.data.remote.api.VisitsApi
import com.thehouse.app.data.remote.dto.visits.CancelVisitRequestDto
import com.thehouse.app.data.remote.dto.visits.CreateVisitRequestDto
import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import com.thehouse.app.data.visits.mapper.toDomain
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.VisitErrorCode
import com.thehouse.app.domain.error.VisitException
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.repository.VisitsRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class VisitsRepositoryImpl(
    private val api: VisitsApi,
    private val json: Json,
) : VisitsRepository {

    override suspend fun createVisit(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> = safeCall {
        val response = api.createVisit(
            idempotencyKey = idempotencyKey,
            body = CreateVisitRequestDto(
                propertyId = propertyId,
                scheduledAt = scheduledAt,
                message = message,
            ),
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(
                VisitCreation(
                    visit = body.toDomain(),
                    replayed = response.headers()["Idempotent-Replayed"]
                        ?.equals("true", ignoreCase = true) == true,
                ),
            )
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getVisits(
        status: VisitStatus?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<Visit>> = safeCall {
        val response = api.getVisits(
            status = status?.name?.lowercase(),
            page = page,
            pageSize = pageSize,
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(
                PagedResult(
                    items = body.items.map { it.toDomain() },
                    page = body.page,
                    pageSize = body.pageSize,
                    totalItems = body.totalItems,
                    totalPages = body.totalPages,
                ),
            )
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getVisit(id: String): Result<Visit> = safeCall {
        val response = api.getVisit(id)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun cancelVisit(id: String, reason: String?): Result<Visit> = safeCall {
        val response = api.cancelVisit(
            id = id,
            body = CancelVisitRequestDto(reason = reason?.takeIf { it.isNotBlank() }),
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    private fun errorFor(response: Response<*>): VisitException {
        val code = parseCode(response)
        val domainError = when (response.code()) {
            401, 403 -> DomainError.Unauthorized
            404 -> DomainError.NotFound
            409 -> DomainError.Conflict
            408 -> DomainError.Timeout
            429 -> DomainError.RateLimited
            in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        return VisitException(domainError, code, retryAfter)
    }

    private fun parseCode(response: Response<*>): VisitErrorCode {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull() ?: return VisitErrorCode.UNKNOWN
        val problem = runCatching { json.decodeFromString<ProblemDetailsDto>(raw) }.getOrNull()
        return VisitErrorCode.fromApi(problem?.code)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (timeout: SocketTimeoutException) {
        Result.failure(VisitException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(VisitException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(VisitException(DomainError.Unknown))
    }
}