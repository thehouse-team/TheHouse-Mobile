package com.thehouse.app.domain.model

import java.time.Instant

/** Quick questions confirmadas por el contrato de solicitudes (0–5 valores por solicitud). */
enum class QuickQuestion(val api: String) {
    AVAILABLE_NOW("available_now"),
    SERVICES_INCLUDED("services_included"),
    DEPOSIT("deposit"),
    MINIMUM_CONTRACT("minimum_contract"),
    VISIT_POSSIBLE("visit_possible"),
    PETS_ALLOWED("pets_allowed"),
    ROOMMATES("roommates");

    companion object {
        fun fromApi(value: String): QuickQuestion? =
            entries.firstOrNull { it.api == value.lowercase() }
    }
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED;

    companion object {
        fun fromApi(value: String): RequestStatus = when (value.lowercase()) {
            "accepted" -> ACCEPTED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            else -> PENDING
        }
    }
}

data class RequestRoom(
    val id: String,
    val buildingId: String,
    val title: String,
    val city: String,
)

data class RequestStudent(
    val name: String,
    val university: String?,
)

data class RequestLandlord(
    val name: String,
)

data class RequestVisit(
    val id: String,
    val scheduledAt: Instant,
    val status: String,
)

data class RequestRejection(
    val reason: String,
    val note: String?,
)

data class RoomRequest(
    val id: String,
    val status: RequestStatus,
    val room: RequestRoom,
    val quickQuestions: List<QuickQuestion>,
    val message: String?,
    val student: RequestStudent?,
    val landlord: RequestLandlord?,
    val visit: RequestVisit?,
    val conversationId: String?,
    val rejection: RequestRejection?,
    val roomAvailable: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
