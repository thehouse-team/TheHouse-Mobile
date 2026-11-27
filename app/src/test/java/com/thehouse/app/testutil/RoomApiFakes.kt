package com.thehouse.app.testutil

import com.thehouse.app.data.remote.api.RoomApi
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto

/**
 * Implementación de test SOLO para el método de búsqueda. NUNCA se usa en
 * producción: la prueba real de transporte usa Retrofit + MockWebServer.
 */
open class SearchStubRoomApi : RoomApi {
    override suspend fun getRoom(id: String): com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto =
        error("getRoom no implementado en este stub de búsqueda")

    override suspend fun searchRooms(
        universityId: String,
        minPrice: Int?,
        maxPrice: Int?,
        roomType: String?,
        bathroom: String?,
        buildingType: String?,
        allowedTenants: String?,
        services: String?,
        maxDistanceKm: Double?,
        sort: String?,
        page: Int,
        pageSize: Int,
    ): RoomSearchResponseDto = RoomSearchResponseDto()
}
