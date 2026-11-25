package com.thehouse.app.data.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.dto.smartmatch.*
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import retrofit2.HttpException

class SmartMatchRepository(
    private val api: SmartMatchApi
) {

    /** Calcula scores para una lista de roomIds */
    suspend fun calculateScores(
        roomIds: List<String>,
        universityId: String? = null,
        preferences: Preference? = null
    ): List<SmartMatchScoreItem> {
        val request = SmartMatchScoreRequest(
            roomIds = roomIds,
            universityId = universityId,
            preferences = null
        )
        val response = api.calculateScores(request)
        return response.items
    }

    /** Obtiene recomendaciones autenticadas con paginación */
    suspend fun getRecommendations(page: Int = 1, pageSize: Int = 20): SmartMatchListResponse {
        return api.getRecommendations(page, pageSize)
    }

    /** Query pública de recomendaciones (sin token) */
    suspend fun queryRecommendations(request: SmartMatchQueryRequest): SmartMatchRecommendationResponse {
        return api.queryRecommendations(request)
    }

    /** Descarta una recomendación */
    suspend fun dismissRecommendation(roomId: String): Boolean {
        val response = api.dismissRecommendation(SmartMatchDismissalRequest(roomId))
        return response.dismissed
    }

    /** Restaura una recomendación descartada */
    suspend fun restoreRecommendation(roomId: String): Boolean {
        val response = api.restoreRecommendation(roomId)
        return response.dismissed
    }

    /** AUTH: obtiene explicación para un roomId */
    suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse {
        return api.getExplanation(roomId)
    }

    /** GUEST: obtiene explicación por query (sin token) */
    suspend fun queryExplanation(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse {
        return api.queryExplanations(request)
    }

    /** Verifica si el perfil es completo (error 422) */
    data class ProfileCheckResult(
        val isComplete: Boolean,
        val missing: List<String>
    )

    fun checkProfileCompleteness(
        response: retrofit2.Response<*>
    ): ProfileCheckResult {
        if (response.code() == 422) {
            val errorBody = response.errorBody()
            // Parsear error 422 smart_match_profile_incomplete
            // Por ahora retornamos estructura básica
            return ProfileCheckResult(
                isComplete = false,
                missing = listOf("university", "budget.max", "livingWith", "placeType")
            )
        }
        return ProfileCheckResult(isComplete = true, missing = emptyList())
    }
}