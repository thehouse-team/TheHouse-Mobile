package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.room.RoomDetailResponseDto
import com.thehouse.app.data.remote.dto.room.RoomSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato real del Backend (`SearchEndpoint`):
 * - `GET /api/v1/rooms/{id}` (auth requerida, token vía interceptor).
 * - `GET /api/v1/rooms/search` (público; parámetros contractuales).
 */
interface RoomApi {
    @GET("api/v1/rooms/{id}")
    suspend fun getRoom(@Path("id") id: String): RoomDetailResponseDto

    @GET("api/v1/rooms/search")
    suspend fun searchRooms(
        @Query("universityId") universityId: String,
        @Query("minPrice") minPrice: Int? = null,
        @Query("maxPrice") maxPrice: Int? = null,
        @Query("roomType") roomType: String? = null,
        @Query("bathroom") bathroom: String? = null,
        @Query("buildingType") buildingType: String? = null,
        @Query("allowedTenants") allowedTenants: String? = null,
        @Query("services") services: String? = null,
        @Query("maxDistanceKm") maxDistanceKm: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): RoomSearchResponseDto
}
