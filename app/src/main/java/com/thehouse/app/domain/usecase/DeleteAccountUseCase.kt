package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.repository.AccountRepository
import com.thehouse.app.domain.repository.LocalUserDataPurger
import kotlinx.coroutines.flow.StateFlow

/**
 * Resultado explícito de eliminación de cuenta.
 *
 * Evita fingir éxito: [BackendUnavailable] significa que no existe contrato
 * ejecutable y que NO se ejecutó ninguna request ni limpieza local.
 */
sealed interface AccountDeletionResult {

    /** El Backend confirmó la eliminación y se ejecutó la limpieza local. */
    data object Deleted : AccountDeletionResult

    /** No existe contrato Backend confirmado (`DELETE-ACCOUNT-BACKEND-BLOCKED`). */
    data object BackendUnavailable : AccountDeletionResult

    /** El Backend rechazó o falló la eliminación; la sesión local se conserva. */
    data class Failed(val error: DomainError) : AccountDeletionResult
}

/**
 * Caso de uso de eliminación de cuenta.
 *
 * Orden obligatorio:
 * 1. verificar contrato disponible;
 * 2. capturar el userId de la sesión activa;
 * 3. ejecutar la eliminación remota;
 * 4. solo si fue exitosa, purgar datos locales y sesión.
 *
 * Un fallo de red/servidor NO limpia la sesión ni simula la eliminación.
 */
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val localUserDataPurger: LocalUserDataPurger,
    private val sessionState: StateFlow<SessionState>,
) {

    val isAvailable: Boolean
        get() = accountRepository.isDeletionAvailable

    suspend operator fun invoke(): AccountDeletionResult {
        if (!accountRepository.isDeletionAvailable) {
            return AccountDeletionResult.BackendUnavailable
        }
        val userId = (sessionState.value as? SessionState.Active)?.session?.userId
        val deletion = accountRepository.deleteAccount()
        if (deletion.isSuccess) {
            // La purga limpia primero la sesión y no debe impedir reportar el
            // éxito remoto ya confirmado.
            runCatching { localUserDataPurger.purge(userId) }
            return AccountDeletionResult.Deleted
        }
        val error = (deletion.exceptionOrNull() as? DomainException)?.error
            ?: DomainError.Unknown
        return AccountDeletionResult.Failed(error)
    }
}
