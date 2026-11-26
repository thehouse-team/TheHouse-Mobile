package com.thehouse.app.data.account.repository

import com.thehouse.app.data.error.DataError
import com.thehouse.app.data.error.toDomainError
import com.thehouse.app.data.remote.api.AccountApi
import com.thehouse.app.data.remote.dto.account.AccountDeletionRequestDto
import com.thehouse.app.data.remote.readProblemCode
import com.thehouse.app.data.serialization.JsonProvider
import com.thehouse.app.domain.error.AccountDeletionError
import com.thehouse.app.domain.error.AccountDeletionException
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.DeletionBlocker
import com.thehouse.app.domain.model.DeletionEffects
import com.thehouse.app.domain.model.DeletionPreview
import com.thehouse.app.domain.repository.AccountRepository
import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * Implementación real del contrato `GET|POST /api/v1/users/me/deletion`
 * confirmado en Backend `origin/main` @ 780b7be.
 *
 - `ELIMINAR` se envía literalmente (case-sensitive, lo valida el Backend).
 - 204 = cuenta eliminada; el caller ejecuta la purga local.
 - Errores Problem Details tipados, sin exponer mensajes crudos.
 */
class AccountRepositoryImpl(
    private val api: AccountApi,
    private val json: Json = JsonProvider.create(),
) : AccountRepository {

    override val isDeletionAvailable: Boolean = true

    override suspend fun getDeletionPreview(): Result<DeletionPreview> = try {
        val response = api.getDeletionPreview()
        if (response.isSuccessful && response.body() != null) {
            val dto = response.body()!!
            Result.success(
                DeletionPreview(
                    canDelete = dto.canDelete,
                    blockers = dto.blockers.map { DeletionBlocker(it.code, it.count) },
                    effects = DeletionEffects(
                        buildings = dto.effects.buildings,
                        rooms = dto.effects.rooms,
                        upcomingVisits = dto.effects.upcomingVisits,
                        openRequests = dto.effects.openRequests,
                        pendingAgreements = dto.effects.pendingAgreements,
                        conversations = dto.effects.conversations,
                        reviews = dto.effects.reviews,
                        favorites = dto.effects.favorites,
                        roommateProfile = dto.effects.roommateProfile,
                    ),
                ),
            )
        } else {
            Result.failure(errorFor(response))
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    override suspend fun deleteAccount(password: String): Result<Unit> = try {
        val response = api.deleteAccount(
            AccountDeletionRequestDto(password = password, confirmation = CONFIRMATION),
        )
        if (response.code() == 204 || response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(errorFor(response))
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    private fun errorFor(response: Response<*>): Throwable {
        val code = runCatching { response.readProblemCode(json) }.getOrNull()
        val reason = when {
            response.code() == 403 && code == "password_mismatch" ->
                AccountDeletionError.PasswordMismatch
            response.code() == 409 && code == "deletion_blocked" ->
                AccountDeletionError.DeletionBlocked
            response.code() == 409 && code == "legal_version_outdated" ->
                AccountDeletionError.LegalVersionOutdated
            response.code() == 429 -> AccountDeletionError.RateLimited
            response.code() == 503 -> AccountDeletionError.Incomplete
            else -> AccountDeletionError.Generic(
                DataError.Http(response.code()).toDomainError(),
            )
        }
        return if (reason is AccountDeletionError.Generic) {
            DomainException(reason.domain)
        } else {
            AccountDeletionException(reason)
        }
    }

    private companion object {
        const val CONFIRMATION = "ELIMINAR"
    }
}
