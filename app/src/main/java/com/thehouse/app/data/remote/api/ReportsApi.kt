package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.reports.CreateReportDto
import com.thehouse.app.data.remote.dto.reports.ReportCreatedDto
import com.thehouse.app.data.remote.dto.reports.VerificationStatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Api REAL de NV-21 (reportes y verificación del dueño). Contrato Backend §6.1. */
interface ReportsApi {

    @POST("api/v1/reports")
    suspend fun createReport(@Body body: CreateReportDto): Response<ReportCreatedDto>

    @GET("api/v1/landlord/buildings/{id}/verification")
    suspend fun getBuildingVerification(@Path("id") id: String): Response<VerificationStatusDto>
}
