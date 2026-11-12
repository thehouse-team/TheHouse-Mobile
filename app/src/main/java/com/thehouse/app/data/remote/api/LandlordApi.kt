package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.landlord.BuildingDto
import com.thehouse.app.data.remote.dto.landlord.BuildingsListResponseDto
import com.thehouse.app.data.remote.dto.landlord.CopyFromRoomDto
import com.thehouse.app.data.remote.dto.landlord.CreateBuildingDto
import com.thehouse.app.data.remote.dto.landlord.CreateLandlordProfileDto
import com.thehouse.app.data.remote.dto.landlord.GenerateRoomsDto
import com.thehouse.app.data.remote.dto.landlord.LandlordProfileDto
import com.thehouse.app.data.remote.dto.landlord.PublicationStatusDto
import com.thehouse.app.data.remote.dto.landlord.RoomDto
import com.thehouse.app.data.remote.dto.landlord.RoomsListResponseDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Api REAL de NV-12 (arrendador/edificios/cuartos). Contrato Backend §6.1. */
interface LandlordApi {

    @POST("api/v1/landlord/profile")
    suspend fun createProfile(@Body body: CreateLandlordProfileDto): Response<LandlordProfileDto>

    @GET("api/v1/landlord/profile")
    suspend fun getProfile(): Response<LandlordProfileDto>

    @POST("api/v1/landlord/buildings")
    suspend fun createBuilding(@Body body: CreateBuildingDto): Response<BuildingDto>

    @GET("api/v1/landlord/buildings")
    suspend fun getBuildings(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<BuildingsListResponseDto>

    @GET("api/v1/landlord/buildings/{id}")
    suspend fun getBuilding(@Path("id") id: String): Response<BuildingDto>

    /** PATCH parcial: el cuerpo se construye campo a campo en el repositorio. */
    @PATCH("api/v1/landlord/buildings/{id}")
    suspend fun updateBuilding(@Path("id") id: String, @Body body: JsonObject): Response<BuildingDto>

    @DELETE("api/v1/landlord/buildings/{id}")
    suspend fun deleteBuilding(@Path("id") id: String, @Query("version") version: Int): Response<Unit>

    @POST("api/v1/landlord/buildings/{id}/rooms/generate")
    suspend fun generateRooms(@Path("id") id: String, @Body body: GenerateRoomsDto): Response<RoomsListResponseDto>

    @GET("api/v1/landlord/buildings/{id}/rooms")
    suspend fun getRooms(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): Response<RoomsListResponseDto>

    @PATCH("api/v1/landlord/rooms/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body body: JsonObject): Response<RoomDto>

    @DELETE("api/v1/landlord/rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String, @Query("version") version: Int): Response<Unit>

    @POST("api/v1/landlord/rooms/{id}/duplicate")
    suspend fun duplicateRoom(@Path("id") id: String): Response<RoomDto>

    @POST("api/v1/landlord/rooms/{id}/copy-from")
    suspend fun copyFromRoom(@Path("id") id: String, @Body body: CopyFromRoomDto): Response<RoomDto>

    @POST("api/v1/landlord/rooms/{id}/pause")
    suspend fun pauseRoom(@Path("id") id: String): Response<RoomDto>

    @POST("api/v1/landlord/rooms/{id}/resume")
    suspend fun resumeRoom(@Path("id") id: String): Response<RoomDto>

    @GET("api/v1/landlord/buildings/{id}/publication")
    suspend fun getPublication(@Path("id") id: String): Response<PublicationStatusDto>

    @POST("api/v1/landlord/buildings/{id}/publish")
    suspend fun publishBuilding(@Path("id") id: String): Response<BuildingDto>
}
