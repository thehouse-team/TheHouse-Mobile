package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.visits.CancelVisitRequestDto
import com.thehouse.app.data.remote.dto.visits.CreateVisitRequestDto
import com.thehouse.app.data.remote.dto.visits.VisitDto
import com.thehouse.app.data.remote.dto.visits.VisitsListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VisitsApi {

    @POST("api/v1/visits")
    suspend fun createVisit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateVisitRequestDto,
    ): Response<VisitDto>

    @GET("api/v1/users/me/visits")
    suspend fun getVisits(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<VisitsListResponseDto>

    @GET("api/v1/visits/{id}")
    suspend fun getVisit(
        @Path("id") id: String,
    ): Response<VisitDto>

    @POST("api/v1/visits/{id}/cancel")
    suspend fun cancelVisit(
        @Path("id") id: String,
        @Body body: CancelVisitRequestDto = CancelVisitRequestDto(),
    ): Response<VisitDto>
}