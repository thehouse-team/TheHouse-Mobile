package com.thehouse.app.data.legal.repository

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.AcceptLegalRequestDto
import com.thehouse.app.data.remote.api.LegalApi
import com.thehouse.app.data.remote.dto.legal.LegalResponseDto
import com.thehouse.app.data.remote.readProblemCode
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.AccountDeletionError
import com.thehouse.app.domain.error.AccountDeletionException
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.LegalInfo
import com.thehouse.app.domain.repository.LegalRepository
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Implementación real de `GET /legal` y `PUT /users/me/legal` (Backend 780b7be). */
class LegalRepositoryImpl(
    private val api: LegalApi,
    private val json: Json = JsonProvider.create(),
) : LegalRepository {

    override suspend fun getLegal(): Result<LegalInfo> = try {
        val response = api.getLegal()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.toDomain())
        } else {
            Result.failure(DomainException(DataError.Http(response.code()).toDomainError()))
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    override suspend fun acceptLegal(termsVersion: String, privacyVersion: String): Result<Unit> = try {
        val response = api.acceptLegal(AcceptLegalRequestDto(termsVersion, privacyVersion))
        when {
            response.code() in 200..299 -> Result.success(Unit)
            response.code() == 409 &&
                runCatching { response.readProblemCode(json) }.getOrNull() == "legal_version_outdated" ->
                Result.failure(AccountDeletionException(AccountDeletionError.LegalVersionOutdated))
            else -> Result.failure(
                DomainException(DataError.Http(response.code()).toDomainError()),
            )
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    private fun LegalResponseDto.toDomain() = LegalInfo(
        termsVersion = termsVersion,
        privacyVersion = privacyVersion,
        landlordTermsVersion = landlordTermsVersion,
        termsUrl = termsUrl,
        privacyUrl = privacyUrl,
        accountDeletionUrl = accountDeletionUrl,
        updatedAt = updatedAt,
    )
}
