package com.thehouse.app.data.remote.dto.room

import kotlinx.serialization.Serializable

/**
 * Espejo exacto de `TheHouse.Application.Contracts.Search.RoomSearchResponse`
 * y `RoomSearchItem` (Backend origin/main, auditado en Fase B).
 *
 * Campos nullable según contrato: `roomType`, `bathroom`, `thumbnailUrl`.
 * `price` es Int (MXN). `approxLocation` es el centro de una celda 0.003°.
 */
@Serializable
data class RoomSearchResponseDto(
    val items: List<RoomSearchItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class RoomSearchItemDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val price: Int,
    val currency: String,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String,
    val allowedTenants: String,
    val services: List<String> = emptyList(),
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: ApproxLocationDto,
    val thumbnailUrl: String? = null,
    val demo: Boolean = false,
)
