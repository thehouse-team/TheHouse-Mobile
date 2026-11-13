package com.thehouse.app.data.agreements.repository

import com.thehouse.app.data.agreements.mapper.toDomain
import com.thehouse.app.data.remote.api.AgreementsApi
import com.thehouse.app.data.remote.auth.AccountSuspendedIOException
import com.thehouse.app.data.remote.dto.agreements.AcceptAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.ClosingCodeRequestDto
import com.thehouse.app.data.remote.dto.agreements.CreateAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.EndAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.LandlordAgreementListResponseDto
import com.thehouse.app.data.remote.dto.agreements.LandlordAgreementResponseDto
import com.thehouse.app.data.remote.readProblemCode
import com.thehouse.app.domain.error.AgreementErrorCode
import com.thehouse.app.domain.error.AgreementException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.repository.AgreementsRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class AgreementsRepositoryImpl(
    private val api: AgreementsApi,
    private val json: Json,
) : AgreementsRepository {

    override suspend fun createAgreement(
        requestId: String,
        monthlyRent: Int,
        deposit: Int,
        startDate: String,
        durationMonths: Int,
        paymentDay: Int,
    ): Result<LandlordAgreement> = safeCall {
        finish(
            api.createAgreement(
                CreateAgreementRequestDto(
                    requestId = requestId,
                    monthlyRent = monthlyRent,
                    deposit = deposit,
                    startDate = startDate,
                    durationMonths = durationMonths,
                    paymentDay = paymentDay,
                ),
            ),
        )
    }

    override suspend fun getLandlordAgreements(
        status: AgreementStatus?,
        roomId: String?,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<LandlordAgreement>> = safeCall {
        api.getLandlordAgreements(
            status = status?.api,
            roomId = roomId?.takeIf { it.isNotBlank() },
            page = page,
            pageSize = pageSize,
        ).toPageResult()
    }

    override suspend fun getLandlordAgreement(id: String): Result<LandlordAgreement> = safeCall {
        finish(api.getLandlordAgreement(id))
    }

    override suspend fun issueAgreementCode(id: String): Result<LandlordAgreement> = safeCall {
        finish(api.issueAgreementCode(id))
    }

    override suspend fun cancelAgreement(id: String): Result<LandlordAgreement> = safeCall {
        finish(api.cancelAgreement(id))
    }

    override suspend fun endAgreement(
        id: String,
        reason: String?,
        note: String?,
    ): Result<LandlordAgreement> = safeCall {
        val body = if (reason == null && note.isNullOrBlank()) {
            null
        } else {
            EndAgreementRequestDto(
                reason = reason?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
            )
        }
        finish(api.endAgreement(id, body))
    }

    override suspend fun previewAgreement(
        code: String?,
        qrToken: String?,
    ): Result<AgreementPreview> = safeCall {
        val response = api.previewAgreement(ClosingCodeRequestDto(code = code, qrToken = qrToken))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun acceptAgreement(
        code: String?,
        qrToken: String?,
        acceptedTerms: Boolean,
    ): Result<MyRental> = safeCall {
        val response = api.acceptAgreement(
            AcceptAgreementRequestDto(code = code, qrToken = qrToken, acceptedTerms = acceptedTerms),
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getMyRental(): Result<MyRental> = safeCall {
        val response = api.getMyRental()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.toDomain())
        } else {
            Result.failure(errorFor(response))
        }
    }

    private suspend fun finish(response: Response<LandlordAgreementResponseDto>): Result<LandlordAgreement> {
        val body = response.body()
        if (response.isSuccessful && body != null) {
            return Result.success(body.toDomain())
        }
        return Result.failure(errorFor(response))
    }

    private suspend fun Response<LandlordAgreementListResponseDto>.toPageResult(): Result<PagedResult<LandlordAgreement>> {
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

    private fun errorFor(response: Response<*>): AgreementException {
        val rawProblemCode = response.readProblemCode(json)
        val code = AgreementErrorCode.fromApi(rawProblemCode)
        val domainError = when {
            response.code() == 403 && rawProblemCode == "account_suspended" -> DomainError.AccountSuspended
            response.code() == 401 || response.code() == 403 -> DomainError.Unauthorized
            response.code() == 404 -> DomainError.NotFound
            response.code() == 409 -> DomainError.Conflict
            response.code() == 410 -> DomainError.Gone
            response.code() == 422 -> DomainError.Unknown
            response.code() == 408 -> DomainError.Timeout
            response.code() == 429 -> DomainError.RateLimited
            response.code() in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        return AgreementException(domainError, code, retryAfter)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (suspended: AccountSuspendedIOException) {
        Result.failure(AgreementException(DomainError.AccountSuspended))
    } catch (timeout: SocketTimeoutException) {
        Result.failure(AgreementException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(AgreementException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(AgreementException(DomainError.Unknown))
    }
}
