package com.thehouse.app.data.remote.dto.visits

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisitPropertyDto(
    val id: String,
    val title: String,
    val city: String,
)

@Serializable
data class VisitDto(
    val id: String,
    val property: VisitPropertyDto,
    val scheduledAt: String,
    val timeZone: String,
    val status: String,
    val message: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val cancelledAt: String? = null,
)

@Serializable
data class CreateVisitRequestDto(
    val propertyId: String,
    val scheduledAt: String,
    val message: String? = null,
)

@Serializable
data class CancelVisitRequestDto(
    val reason: String? = null,
)

@Serializable
data class VisitsListResponseDto(
    val items: List<VisitDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    @SerialName("code") val code: String? = null,
)
