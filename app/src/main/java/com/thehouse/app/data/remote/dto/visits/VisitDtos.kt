package com.thehouse.app.data.remote.dto.visits

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisitPropertyDto(
    val id: String,
    val title: String,
    val city: String,
)

/** Resumen del cuarto real (`VisitResponse.room`, no-null en el contrato). */
@Serializable
data class VisitRoomSummaryDto(
    val id: String,
    val buildingId: String? = null,
    val title: String,
    val city: String? = null,
)

@Serializable
data class VisitDto(
    val id: String,
    /** Contrato real: `room` siempre presente. Legacy: `property` puede estar. */
    val room: VisitRoomSummaryDto? = null,
    val property: VisitPropertyDto? = null,
    val scheduledAt: String,
    val timeZone: String,
    val status: String,
    val message: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val cancelledAt: String? = null,
    /** Dirección exacta: solo presente cuando la visita está confirmada. */
    val address: VisitAddressDto? = null,
    val location: VisitLocationDto? = null,
) {
    /** Identidad de la visita: roomId real cuando existe, legacy propertyId. */
    val effectiveRoomId: String get() = room?.id ?: property?.id.orEmpty()
    val effectiveTitle: String get() = room?.title ?: property?.title.orEmpty()
    val effectiveCity: String get() = room?.city ?: property?.city.orEmpty()
}

@Serializable
data class VisitAddressDto(
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postalCode") val postalCode: String? = null,
)

@Serializable
data class VisitLocationDto(val lat: Double, val lng: Double)

/** Body con XOR roomId/propertyId. Los flujos D20 envían SOLO roomId. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateVisitRequestDto(
    /** XOR contractual: el campo ausente NO se envía, ni siquiera como null. */
    @EncodeDefault(Mode.NEVER) val roomId: String? = null,
    @EncodeDefault(Mode.NEVER) val propertyId: String? = null,
    val scheduledAt: String,
    @EncodeDefault(Mode.NEVER) val message: String? = null,
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
