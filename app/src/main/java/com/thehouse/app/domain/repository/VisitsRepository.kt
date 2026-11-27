package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitStatus

interface VisitsRepository {
    /** Flujo nuevo D20: SOLO roomId (XOR), nunca propertyId. */
    suspend fun createRoomVisit(
        roomId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> = Result.failure(
        com.thehouse.app.domain.error.VisitException(
            com.thehouse.app.domain.error.DomainError.Unknown,
        ),
    )

    /** Compatibilidad legacy (rentas/visitas históricas). */
    suspend fun createVisit(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation>

    suspend fun getVisits(
        status: VisitStatus? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<Visit>>

    suspend fun getVisit(id: String): Result<Visit>

    suspend fun cancelVisit(id: String, reason: String? = null): Result<Visit>
}
