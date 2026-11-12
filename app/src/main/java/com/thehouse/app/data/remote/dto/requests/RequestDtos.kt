package com.thehouse.app.data.remote.dto.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRequestDto(
    val quickQuestions: List<String> = emptyList(),
    val message: String? = null,
)

@Serializable
data class RequestRoomDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val city: String,
)

@Serializable
data class RequestStudentDto(
    val name: String,
    val university: String? = null,
)

@Serializable
data class RequestLandlordDto(
    val name: String,
)

@Serializable
data class RequestVisitDto(
    val id: String,
    val scheduledAt: String,
    val status: String,
)

@Serializable
data class RequestRejectionDto(
    val reason: String,
    val note: String? = null,
)

@Serializable
data class RoomRequestDto(
    val id: String,
    val status: String,
    val room: RequestRoomDto,
    val quickQuestions: List<String> = emptyList(),
    val message: String? = null,
    val student: RequestStudentDto? = null,
    val landlord: RequestLandlordDto? = null,
    val visit: RequestVisitDto? = null,
    @SerialName("conversationId") val conversationId: String? = null,
    val rejection: RequestRejectionDto? = null,
    val roomAvailable: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class RequestsListResponseDto(
    val items: List<RoomRequestDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class RejectRequestDto(
    val reason: String,
    val note: String? = null,
)
