package com.thehouse.app.data.remote.dto.reports

import kotlinx.serialization.Serializable

/** DTOs EXACTOS del contrato NV-21 §6/§6.1 del Backend. No se agregan campos. */

@Serializable
data class CreateReportDto(
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String? = null,
)

@Serializable
data class ReportCreatedDto(
    val id: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class VerificationDecisionDto(
    val decision: String,
    val reason: String? = null,
    val decidedAt: String,
)

@Serializable
data class VerificationStatusDto(
    val status: String,
    val lastDecision: VerificationDecisionDto? = null,
)

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
)
