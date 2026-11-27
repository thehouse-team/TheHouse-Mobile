package com.thehouse.app.domain.error

/**
 * D19: errores específicos de eliminación de cuenta, derivados del
 * Problem Details del Backend (`code`). No contienen infraestructura.
 */
sealed interface AccountDeletionError {
    data object PasswordMismatch : AccountDeletionError       // 403 password_mismatch
    data object DeletionBlocked : AccountDeletionError        // 409 deletion_blocked
    data object LegalVersionOutdated : AccountDeletionError   // 409 legal_version_outdated
    data object RateLimited : AccountDeletionError            // 429
    data object Incomplete : AccountDeletionError             // 503 deletion_incomplete
    data class Generic(val domain: DomainError) : AccountDeletionError
}

class AccountDeletionException(val reason: AccountDeletionError) : Exception()
