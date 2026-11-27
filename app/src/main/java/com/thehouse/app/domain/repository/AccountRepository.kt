package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.DeletionBlocker
import com.thehouse.app.domain.model.DeletionEffects
import com.thehouse.app.domain.model.DeletionPreview

/**
 * Contrato de eliminación de la cuenta propia (Backend confirmado 780b7be).
 *
 * [isDeletionAvailable] es true cuando existe el contrato ejecutable.
 * [getDeletionPreview] muestra blockers/effects antes de pedir la confirmación.
 * [deleteAccount] ejecuta la eliminación con password y "ELIMINAR".
 */
interface AccountRepository {

    val isDeletionAvailable: Boolean

    suspend fun getDeletionPreview(): Result<DeletionPreview> =
        Result.failure(UnsupportedOperationException("preview not available"))

    suspend fun deleteAccount(password: String): Result<Unit> = deleteAccount()

    /** Legacy overload retained for existing test fakes. */
    suspend fun deleteAccount(): Result<Unit> = Result.failure(UnsupportedOperationException())
}
