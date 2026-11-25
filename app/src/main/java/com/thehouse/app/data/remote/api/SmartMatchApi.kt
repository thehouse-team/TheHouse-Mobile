package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.smartmatch.*
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.*;

/** Contrato NV-23 v1.2.0 para Smart Match */
interface SmartMatchApi {

    /** POST /api/v1/smart-match/scores */
    @POST("api/v1/smart-match/scores")
    suspend fun calculateScores(
        @Body request: SmartMatchScoreRequest
    ): SmartMatchScoreResponse

    /** GET /api/v1/smart-match/recommendations?page=1&pageSize=20 */
    @GET("api/v1/smart-match/recommendations")
    suspend fun getRecommendations(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): SmartMatchListResponse

    /** POST /api/v1/smart-match/recommendations/query?page=1&pageSize=20 (pública, sin token) */
    @POST("api/v1/smart-match/recommendations/query")
    suspend fun queryRecommendations(
        @Body request: SmartMatchQueryRequest
    ): SmartMatchRecommendationResponse

    /** POST /api/v1/smart-match/dismissals */
    @POST("api/v1/smart-match/dismissals")
    suspend fun dismissRecommendation(
        @Body request: SmartMatchDismissalRequest
    ): SmartMatchDismissalResponse

    /** DELETE /api/v1/smart-match/dismissals/{roomId} */
    @DELETE("api/v1/smart-match/dismissals/{roomId}")
    suspend fun restoreRecommendation(
        @Path("roomId") roomId: String
    ): SmartMatchDismissalResponse

    /** AUTH: GET /api/v1/smart-match/explanations/{roomId} */
    @GET("api/v1/smart-match/explanations/{roomId}")
    suspend fun getExplanation(
        @Path("roomId") roomId: String
    ): SmartMatchExplanationResponse

    /** GUEST: POST /api/v1/smart-match/explanations/query */
    @POST("api/v1/smart-match/explanations/query")
    suspend fun queryExplanations(
        @Body request: SmartMatchExplanationQueryRequest
    ): SmartMatchExplanationResponse
}

/** Request for dismissal */
data class SmartMatchDismissalRequest(
    val roomId: String
)

/** Response for dismissal */
data class SmartMatchDismissalResponse(
    val dismissed: Boolean = true
)

/** Response for scores */
data class SmartMatchScoreResponse(
    val items: List<SmartMatchScoreItem> = emptyList(),
    val total: Int = 0
)