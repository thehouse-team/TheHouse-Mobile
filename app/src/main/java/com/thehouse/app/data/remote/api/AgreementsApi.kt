package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.agreements.AcceptAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.AgreementPreviewResponseDto
import com.thehouse.app.data.remote.dto.agreements.ClosingCodeRequestDto
import com.thehouse.app.data.remote.dto.agreements.CreateAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.EndAgreementRequestDto
import com.thehouse.app.data.remote.dto.agreements.LandlordAgreementListResponseDto
import com.thehouse.app.data.remote.dto.agreements.LandlordAgreementResponseDto
import com.thehouse.app.data.remote.dto.agreements.RentalResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AgreementsApi {

    @POST("api/v1/landlord/agreements")
    suspend fun createAgreement(
        @Body body: CreateAgreementRequestDto,
    ): Response<LandlordAgreementResponseDto>

    @GET("api/v1/landlord/agreements")
    suspend fun getLandlordAgreements(
        @Query("status") status: String? = null,
        @Query("roomId") roomId: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<LandlordAgreementListResponseDto>

    @GET("api/v1/landlord/agreements/{id}")
    suspend fun getLandlordAgreement(
        @Path("id") id: String,
    ): Response<LandlordAgreementResponseDto>

    @POST("api/v1/landlord/agreements/{id}/code")
    suspend fun issueAgreementCode(
        @Path("id") id: String,
    ): Response<LandlordAgreementResponseDto>

    @POST("api/v1/landlord/agreements/{id}/cancel")
    suspend fun cancelAgreement(
        @Path("id") id: String,
    ): Response<LandlordAgreementResponseDto>

    @POST("api/v1/landlord/agreements/{id}/end")
    suspend fun endAgreement(
        @Path("id") id: String,
        @Body body: EndAgreementRequestDto? = null,
    ): Response<LandlordAgreementResponseDto>

    @POST("api/v1/agreements/preview")
    suspend fun previewAgreement(
        @Body body: ClosingCodeRequestDto,
    ): Response<AgreementPreviewResponseDto>

    @POST("api/v1/agreements/accept")
    suspend fun acceptAgreement(
        @Body body: AcceptAgreementRequestDto,
    ): Response<RentalResponseDto>

    @GET("api/v1/users/me/rental")
    suspend fun getMyRental(): Response<RentalResponseDto>
}
