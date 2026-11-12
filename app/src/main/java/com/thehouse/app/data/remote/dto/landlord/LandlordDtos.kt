package com.thehouse.app.data.remote.dto.landlord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTOs EXACTOS del contrato NV-12 §6/§6.1 del Backend. No se agregan campos. */

@Serializable
data class LandlordProfileDto(
    val fullName: String,
    val phone: String,
    val rentalMode: String,
    val rfc: String? = null,
    val acceptedTermsVersion: String,
    val createdAt: String? = null,
)

@Serializable
data class CreateLandlordProfileDto(
    val fullName: String,
    val phone: String,
    val rentalMode: String,
    val rfc: String? = null,
    val acceptedTermsVersion: String,
)

@Serializable
data class BuildingAddressDto(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
)

@Serializable
data class BuildingLocationDto(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class NearestUniversityDto(
    val id: String,
    val code: String,
    val name: String,
)

@Serializable
data class BuildingDto(
    val id: String,
    val type: String,
    val name: String,
    val address: BuildingAddressDto,
    val location: BuildingLocationDto,
    val nearestUniversity: NearestUniversityDto,
    val distanceToUniversityKm: Double,
    val walkingMinutes: Int,
    val allowedTenants: String,
    val services: List<String>,
    val rules: String? = null,
    val status: String,
    val roomsTotal: Int = 0,
    val roomsAvailable: Int = 0,
    val version: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateBuildingDto(
    val type: String,
    val name: String,
    val address: BuildingAddressDto,
    val location: BuildingLocationDto,
    val nearestUniversityId: String,
    val allowedTenants: String,
    val services: List<String>,
    val rules: String? = null,
)

@Serializable
data class DashboardSummaryDto(
    val buildings: Int = 0,
    val rooms: Int = 0,
    val available: Int = 0,
    val occupancyPercent: Double = 0.0,
    val monthlyIncome: Double = 0.0,
)

@Serializable
data class BuildingsListResponseDto(
    val items: List<BuildingDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
    val summary: DashboardSummaryDto? = null,
)

@Serializable
data class RoomDto(
    val id: String,
    val buildingId: String,
    val number: String,
    val floor: Int,
    val price: Int,
    val currency: String,
    val sizeM2: Int,
    val type: String,
    val bathroom: String,
    val furniture: List<String>,
    val availableFrom: String,
    val minContract: String,
    val status: String,
    val version: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class GenerateRoomsDto(
    val floors: Int,
    @SerialName("roomsPerFloor") val roomsPerFloor: List<Int>,
    val numbering: String,
    val base: RoomBaseDto,
)

@Serializable
data class RoomBaseDto(
    val price: Int? = null,
    val sizeM2: Int? = null,
    val type: String? = null,
    val bathroom: String? = null,
    val furniture: List<String>? = null,
    val availableFrom: String? = null,
    val minContract: String? = null,
)

@Serializable
data class RoomsListResponseDto(
    val items: List<RoomDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class CopyFromRoomDto(
    val sourceRoomId: String,
    val version: Int,
)

@Serializable
data class PublicationStepDto(
    val id: String,
    val done: Boolean,
)

@Serializable
data class PublicationStatusDto(
    val canPublish: Boolean,
    val steps: List<PublicationStepDto> = emptyList(),
)

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
)
