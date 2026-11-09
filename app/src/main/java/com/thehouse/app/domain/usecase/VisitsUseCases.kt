package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitCreation
import com.thehouse.app.domain.model.VisitStatus
import com.thehouse.app.domain.repository.VisitsRepository

class CreateVisitUseCase(private val repository: VisitsRepository) {
    suspend operator fun invoke(
        propertyId: String,
        scheduledAt: String,
        message: String?,
        idempotencyKey: String,
    ): Result<VisitCreation> = repository.createVisit(propertyId, scheduledAt, message, idempotencyKey)
}

class GetVisitsUseCase(private val repository: VisitsRepository) {
    suspend operator fun invoke(
        status: VisitStatus? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<Visit>> = repository.getVisits(status, page, pageSize)
}

class GetVisitUseCase(private val repository: VisitsRepository) {
    suspend operator fun invoke(id: String): Result<Visit> = repository.getVisit(id)
}

class CancelVisitUseCase(private val repository: VisitsRepository) {
    suspend operator fun invoke(id: String, reason: String? = null): Result<Visit> =
        repository.cancelVisit(id, reason)
}