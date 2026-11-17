package com.thehouse.app.data.account.repository

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.repository.AccountRepository

/**
 * `DELETE-ACCOUNT-BACKEND-BLOCKED` (2026-11-17).
 *
 * El Backend disponible NO expone un contrato de eliminación de cuenta:
 * - `origin/main` no define `DELETE /api/v1/users/me` ni una ruta equivalente;
 * - la spec Backend `nv-10-account-session/spec.md` difiere explícitamente la
 *   eliminación de cuenta a una versión posterior.
 *
 * Por eso esta implementación:
 * - declara [isDeletionAvailable] = false;
 * - NO define ruta, DTO, body, status ni query inventados;
 * - NO ejecuta ninguna request HTTP.
 *
 * Cuando exista contrato real, se reemplaza por una implementación Retrofit
 * reutilizando el cliente/auth existentes.
 */
class AccountRepositoryImpl : AccountRepository {

    override val isDeletionAvailable: Boolean = false

    override suspend fun deleteAccount(): Result<Unit> =
        Result.failure(DomainException(DomainError.Unknown))
}
