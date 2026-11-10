package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.search.PlaceSearchResponseDto
import com.thehouse.app.data.remote.dto.search.RoomDetailDto
import com.thehouse.app.data.remote.dto.search.RoomMapResponseDto
import com.thehouse.app.data.remote.dto.search.RoomSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Búsqueda real de cuartos y colonias (NV-14, D10).
 *
 * Público: /rooms/search, /rooms/map, /places/search.
 * Autenticado: /rooms/{id} (Bearer existente de D3).
 * Solo se declaran parámetros confirmados; no hay `q` para /rooms/search.
 */
interface SearchApi {

    @GET("api/v1/rooms/search")
    suspend fun searchRooms(
        @Query("universityId") universityId: String,
        @Query("minPrice") minPrice: Int?,
        @Query("maxPrice") maxPrice: Int?,
        @Query("roomType") roomType: String?,
        @Query("bathroom") bathroom: String?,
        @Query("buildingType") buildingType: String?,
        @Query("allowedTenants") allowedTenants: String?,
        @Query("services") services: String?,
        @Query("maxDistanceKm") maxDistanceKm: Double?,
        @Query("sort") sort: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<RoomSearchResponseDto>

    @GET("api/v1/rooms/map")
    suspend fun getRoomMap(
        @Query("universityId") universityId: String,
        @Query("minPrice") minPrice: Int?,
        @Query("maxPrice") maxPrice: Int?,
        @Query("roomType") roomType: String?,
        @Query("bathroom") bathroom: String?,
        @Query("buildingType") buildingType: String?,
        @Query("allowedTenants") allowedTenants: String?,
        @Query("services") services: String?,
        @Query("maxDistanceKm") maxDistanceKm: Double?,
        @Query("bbox") bbox: String,
    ): Response<RoomMapResponseDto>

    @GET("api/v1/places/search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("universityId") universityId: String,
    ): Response<PlaceSearchResponseDto>

    @GET("api/v1/rooms/{id}")
    suspend fun getRoomById(
        @Path("id") id: String,
    ): Response<RoomDetailDto>
}
