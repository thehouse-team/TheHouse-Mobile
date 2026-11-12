package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.requests.CreateRequestDto
import com.thehouse.app.data.remote.dto.requests.RejectRequestDto
import com.thehouse.app.data.remote.dto.requests.RoomRequestDto
import com.thehouse.app.data.remote.dto.requests.RequestsListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RequestsApi {

    @POST("api/v1/rooms/{roomId}/requests")
    suspend fun createRequest(
        @Path("roomId") roomId: String,
        @Body body: CreateRequestDto,
    ): Response<RoomRequestDto>

    @GET("api/v1/users/me/requests")
    suspend fun getUserRequests(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<RequestsListResponseDto>

    @GET("api/v1/users/me/requests/{id}")
    suspend fun getUserRequest(@Path("id") id: String): Response<RoomRequestDto>

    @POST("api/v1/users/me/requests/{id}/cancel")
    suspend fun cancelRequest(@Path("id") id: String): Response<RoomRequestDto>

    @GET("api/v1/landlord/requests")
    suspend fun getLandlordRequests(
        @Query("status") status: String? = null,
        @Query("roomId") roomId: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<RequestsListResponseDto>

    @GET("api/v1/landlord/requests/{id}")
    suspend fun getLandlordRequest(@Path("id") id: String): Response<RoomRequestDto>

    @POST("api/v1/landlord/requests/{id}/accept")
    suspend fun acceptRequest(@Path("id") id: String): Response<RoomRequestDto>

    @POST("api/v1/landlord/requests/{id}/reject")
    suspend fun rejectRequest(
        @Path("id") id: String,
        @Body body: RejectRequestDto,
    ): Response<RoomRequestDto>
}
