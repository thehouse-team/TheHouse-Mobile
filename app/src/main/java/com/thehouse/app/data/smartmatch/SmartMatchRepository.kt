package com.thehouse.app.data.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.dto.smartmatch.*
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.DomainException
import com.thehouse.app.domain.error.SmartMatchProfileIncompleteException
import com.thehouse.app.domain.model.Preference
import com.thehouse.app.domain.model.Property
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class SmartMatchRepository(
    private val api: SmartMatchApi,
    private val problemJson: Json = Json { ignoreUnknownKeys = true },
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

    /**
     * AUTH: GET /api/v1/smart-match/recommendations.
     *
     * El Backend resuelve perfil, preferencias, universidad, scoring, ranking,
     * breakdown y descartes. Mobile NO reordena ni re-scorea una respuesta 200.
     *
     * Errores mapeados a tipos de dominio (sin exponer Retrofit a Presentation):
     * - 422 `smart_match_profile_incomplete` → [SmartMatchProfileIncompleteException] con `missing`
     * - 401 → DomainException(Unauthorized)
     * - 429 → DomainException(RateLimited)
     * - 400/404/otros → DomainException(tipado)
     * - IOException/timeout/503 → DomainException(Network|Timeout|Server) (elegible a fallback ML)
     */
    suspend fun getRecommendations(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<SmartMatchListResponse> = try {
        Result.success(api.getRecommendations(page, pageSize))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Throwable) {
        Result.failure(mapAuthError(e))
    }

    /**
     * Query pública de recomendaciones (sin token).
     * D16: los errores se mapean a tipos de dominio; 503/red/timeout son
     * elegibles a fallback etiquetado, 400/401/404/422/429 NO.
     */
    suspend fun queryRecommendations(request: SmartMatchQueryRequest): SmartMatchRecommendationResponse {
        return try {
            api.queryRecommendations(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            throw mapAuthError(e)
        }
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
        response: Response<*>
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

    private fun mapAuthError(e: Throwable): Throwable {
        // Errores ya tipados por dominio/callback no se re-empaquetan como Unknown.
        if (e is SmartMatchProfileIncompleteException || e is DomainException) return e
        if (e is HttpException) {
            val code = e.code()
            if (code == 422) {
                val problem = readProblem(e)
                if (problem.first == "smart_match_profile_incomplete") {
                    return SmartMatchProfileIncompleteException(problem.second)
                }
            }
            val error = when (code) {
                400 -> DomainError.ValidationError
                401, 403 -> DomainError.Unauthorized
                404 -> DomainError.NotFound
                408 -> DomainError.Timeout
                422 -> DomainError.ValidationError
                429 -> DomainError.RateLimited
                503 -> DomainError.ServiceUnavailable
                in 500..599 -> DomainError.Server
                else -> DomainError.Unknown
            }
            return DomainException(error)
        }
        return when (e) {
            is SocketTimeoutException -> DomainException(DomainError.Timeout)
            is IOException -> DomainException(DomainError.Network)
            else -> DomainException(DomainError.Unknown)
        }
    }

    /** Lee `code` y `missing` del Problem Details sin registrar PII ni bodies. */
    private fun readProblem(e: HttpException): Pair<String?, List<String>> {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: return null to emptyList()
        val json = runCatching { problemJson.parseToJsonElement(raw) }.getOrNull()
            ?: return null to emptyList()
        val obj = json as? kotlinx.serialization.json.JsonObject ?: return null to emptyList()
        val code = (obj["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val missing = (obj["missing"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ?: emptyList()
        return code to missing
    }
}
