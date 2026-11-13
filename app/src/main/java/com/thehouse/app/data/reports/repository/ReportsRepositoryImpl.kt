package com.thehouse.app.data.reports.repository

import com.thehouse.app.data.remote.api.ReportsApi
import com.thehouse.app.data.remote.dto.reports.CreateReportDto
import com.thehouse.app.data.remote.dto.reports.ProblemDetailsDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.ReportErrorCode
import com.thehouse.app.domain.error.ReportException
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.ReportCreated
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.model.VerificationDecision
import com.thehouse.app.domain.model.VerificationInfo
import com.thehouse.app.domain.model.VerificationLastDecision
import com.thehouse.app.domain.repository.ReportsRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class ReportsRepositoryImpl(
    private val api: ReportsApi,
    private val json: Json,
) : ReportsRepository {

    override suspend fun createReport(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        details: String?,
    ): Result<ReportCreated> = safeCall {
        val response = api.createReport(
            CreateReportDto(
                targetType = targetType.api,
                targetId = targetId,
                reason = reason.api,
                details = details?.takeIf { it.isNotBlank() },
            ),
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(ReportCreated(id = body.id, status = body.status, createdAt = body.createdAt))
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getBuildingVerification(buildingId: String): Result<VerificationInfo> =
        safeCall {
            val response = api.getBuildingVerification(buildingId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    VerificationInfo(
                        status = BuildingStatus.fromApi(body.status),
                        lastDecision = body.lastDecision?.let { decision ->
                            VerificationLastDecision(
                                decision = VerificationDecision.fromApi(decision.decision),
                                reason = decision.reason,
                                decidedAt = decision.decidedAt,
                            )
                        },
                    ),
                )
            } else {
                Result.failure(errorFor(response))
            }
        }

    private fun errorFor(response: Response<*>): ReportException {
        val code = parseCode(response)
        val businessCode = when (response.code()) {
            400 -> ReportErrorCode.VALIDATION
            403 -> code
            else -> code
        }
        val domainError = when {
            response.code() == 403 && code == ReportErrorCode.ACCOUNT_SUSPENDED ->
                DomainError.AccountSuspended
            response.code() == 401 -> DomainError.Unauthorized
            response.code() == 403 -> DomainError.Unauthorized
            response.code() == 404 -> DomainError.NotFound
            response.code() == 409 -> DomainError.Conflict
            response.code() == 408 -> DomainError.Timeout
            response.code() == 429 -> DomainError.RateLimited
            response.code() in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        return ReportException(domainError, businessCode, retryAfter)
    }

    private fun parseCode(response: Response<*>): ReportErrorCode {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
            ?: return ReportErrorCode.UNKNOWN
        val problem = runCatching { json.decodeFromString<ProblemDetailsDto>(raw) }.getOrNull()
        return ReportErrorCode.fromApi(problem?.code)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (timeout: SocketTimeoutException) {
        Result.failure(ReportException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(ReportException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(ReportException(DomainError.Unknown))
    }
}
