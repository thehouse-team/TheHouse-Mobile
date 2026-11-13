package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.ReportCreated
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.model.VerificationInfo
import com.thehouse.app.domain.repository.ReportsRepository

class CreateReportUseCase(private val repository: ReportsRepository) {
    /** POST /api/v1/reports. El usuario viene del JWT; nunca se envía userId. */
    suspend operator fun invoke(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        details: String?,
    ): Result<ReportCreated> = repository.createReport(targetType, targetId, reason, details)
}

class GetBuildingVerificationUseCase(private val repository: ReportsRepository) {
    /** GET /api/v1/landlord/buildings/{id}/verification (dueño). */
    suspend operator fun invoke(buildingId: String): Result<VerificationInfo> =
        repository.getBuildingVerification(buildingId)
}
