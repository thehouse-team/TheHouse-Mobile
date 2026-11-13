package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.ReportCreated
import com.thehouse.app.domain.model.ReportReason
import com.thehouse.app.domain.model.ReportTargetType
import com.thehouse.app.domain.model.VerificationInfo

/** Contrato de dominio D13. La Data lo implementa. */
interface ReportsRepository {

    /** POST /api/v1/reports. El usuario viene del JWT; nunca se envía userId. */
    suspend fun createReport(
        targetType: ReportTargetType,
        targetId: String,
        reason: ReportReason,
        details: String?,
    ): Result<ReportCreated>

    /** GET /api/v1/landlord/buildings/{id}/verification (dueño). */
    suspend fun getBuildingVerification(buildingId: String): Result<VerificationInfo>
}
