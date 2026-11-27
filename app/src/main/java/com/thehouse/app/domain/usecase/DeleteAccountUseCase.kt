package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.DeletionPreview
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.AccountRepository
import com.thehouse.app.domain.repository.LocalUserDataPurger
import kotlinx.coroutines.flow.StateFlow

/** Resultado explícito de eliminación de cuenta (nunca finge éxito). */
sealed interface AccountDeletionResult {

    /** El Backend confirmó 204 y se ejecutó la limpieza local. */
    data object Deleted : AccountDeletionResult

    /** No existe contrato Backend ejecutable. */
    data object BackendUnavailable : AccountDeletionResult

    /** El Backend rechazó o falló; la sesión local se conserva. */
    data class Failed(val error: DomainError) : AccountDeletionResult
}

/**
 * D19: caso de uso de eliminación de cuenta con contrato Backend confirmado.
 * Orden: preview → password+"ELIMINAR" → purga local solo tras 204.
 */
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val localUserDataPurger: LocalUserDataPurger,
    private val sessionState: StateFlow<SessionState>,
) {

    val isAvailable: Boolean
        get() = accountRepository.isDeletionAvailable

    suspend fun preview(): Result<DeletionPreview> = accountRepository.getDeletionPreview()

    /** Legacy overload usado por tests existentes sin password. */
    suspend operator fun invoke(): AccountDeletionResult = deleteInternal(password = "")

    suspend operator fun invoke(password: String): AccountDeletionResult = deleteInternal(password)

    private suspend fun deleteInternal(password: String): AccountDeletionResult {
        if (!accountRepository.isDeletionAvailable) {
            return AccountDeletionResult.BackendUnavailable
        }
        val userId = (sessionState.value as? SessionState.Active)?.session?.userId
        val deletion = accountRepository.deleteAccount(password)
        if (deletion.isSuccess) {
            // La purga limpia primero la sesión; no impide reportar el éxito remoto.
            runCatching { localUserDataPurger.purge(userId) }
            return AccountDeletionResult.Deleted
        }
        val throwable = deletion.exceptionOrNull()
        val error = when (throwable) {
            is DomainException -> throwable.error
            is com.thehouse.app.domain.error.AccountDeletionException ->
                when (throwable.reason) {
                    com.thehouse.app.domain.error.AccountDeletionError.PasswordMismatch,
                    com.thehouse.app.domain.error.AccountDeletionError.DeletionBlocked,
                    -> DomainError.Conflict
                    com.thehouse.app.domain.error.AccountDeletionError.RateLimited -> DomainError.RateLimited
                    com.thehouse.app.domain.error.AccountDeletionError.Incomplete ->
                        DomainError.ServiceUnavailable
                    com.thehouse.app.domain.error.AccountDeletionError.LegalVersionOutdated ->
                        DomainError.Conflict
                    is com.thehouse.app.domain.error.AccountDeletionError.Generic ->
                        throwable.reason.domain
                }
            else -> DomainError.Unknown
        }
        return AccountDeletionResult.Failed(error)
    }
}
