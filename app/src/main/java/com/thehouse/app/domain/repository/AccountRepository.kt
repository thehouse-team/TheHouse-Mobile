package com.thehouse.app.domain.repository

/**
 * Contrato de eliminación de la cuenta propia.
 *
 * [isDeletionAvailable] es la única fuente de verdad para habilitar la acción.
 * Si es false, la UI no debe abrir una confirmación que simule una eliminación
 * remota. La implementación actual es false porque no existe contrato Backend
 * confirmado (`DELETE-ACCOUNT-BACKEND-BLOCKED`).
 */
interface AccountRepository {

    val isDeletionAvailable: Boolean

    /**
     * Elimina la cuenta en el Backend.
     *
     * Solo puede ejecutarse cuando [isDeletionAvailable] es true. Una
     * implementación sin contrato no debe realizar ninguna request HTTP.
     */
    suspend fun deleteAccount(): Result<Unit>
}
