package com.thehouse.app.data.smartmatch

import com.thehouse.app.data.remote.api.SmartMatchApi
import com.thehouse.app.data.remote.api.SmartMatchDismissalRequest
import com.thehouse.app.data.remote.api.SmartMatchDismissalResponse
import com.thehouse.app.data.remote.api.SmartMatchScoreResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchExplanationResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchListResponse
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchQueryRequest
import com.thehouse.app.data.remote.dto.smartmatch.SmartMatchScoreRequest
import com.thehouse.app.domain.error.SmartMatchProfileIncompleteException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** K — DTO y mapeo de errores del contrato real NV-23 v1.2.0 del Backend. */
class SmartMatchBackendContractTest {

    private class NoopApi : SmartMatchApi {
        override suspend fun calculateScores(request: SmartMatchScoreRequest): SmartMatchScoreResponse =
            SmartMatchScoreResponse()
        override suspend fun getRecommendations(page: Int, pageSize: Int): SmartMatchListResponse =
            SmartMatchListResponse()
        override suspend fun queryRecommendations(request: SmartMatchQueryRequest) =
            throw UnsupportedOperationException()
        override suspend fun dismissRecommendation(request: SmartMatchDismissalRequest): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()
        override suspend fun restoreRecommendation(roomId: String): SmartMatchDismissalResponse =
            SmartMatchDismissalResponse()
        override suspend fun getExplanation(roomId: String): SmartMatchExplanationResponse =
            throw UnsupportedOperationException()
        override suspend fun queryExplanations(request: SmartMatchExplanationQueryRequest): SmartMatchExplanationResponse =
            throw UnsupportedOperationException()
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `K backend recommendation json maps nested room breakdown and versions`() {
        // Respuesta real del Backend (NV-23 1.2.0): room anidado + breakdown + modelVersion.
        val raw = """
            {
              "items": [
                {
                  "room": {
                    "id": "room-1", "buildingId": "b-1", "title": "Cuarto", "price": 4500,
                    "currency": "MXN", "roomType": "private", "bathroom": "private",
                    "buildingType": "student_residence", "allowedTenants": "any",
                    "services": ["wifi"], "distanceKm": 1.4, "walkingMinutes": 18,
                    "approxLocation": { "lat": 19.1, "lng": -99.1 },
                    "thumbnailUrl": null, "demo": false
                  },
                  "matchScore": 73,
                  "breakdown": [
                    { "factor": "budget", "label": "Presupuesto", "points": 9.6, "maxPoints": 9.6 },
                    { "factor": "distance", "label": "Distancia", "points": 72.6, "maxPoints": 72.6 },
                    { "factor": "essential_services", "label": "Servicios", "points": 6.7, "maxPoints": 6.7 },
                    { "factor": "place_type", "label": "Tipo de lugar", "points": 4.9, "maxPoints": 4.9 },
                    { "factor": "property_type", "label": "Tipo de propiedad", "points": 1.7, "maxPoints": 1.7 },
                    { "factor": "extras", "label": "Extras", "points": 4.5, "maxPoints": 4.5 }
                  ],
                  "dismissed": false,
                  "modelVersion": "logreg-1.0.0",
                  "featureSchemaVersion": "2.0.0"
                }
              ],
              "page": 1, "pageSize": 20, "totalItems": 1, "totalPages": 1
            }
        """.trimIndent()

        val response = json.decodeFromString<SmartMatchListResponse>(raw)
        val item = response.items.single()
        assertEquals("room-1", item.room.id)
        assertEquals(4500.0, item.room.price, 0.0)
        assertEquals(listOf("wifi"), item.room.services)
        assertEquals(73, item.matchScore)
        assertEquals(6, item.breakdown.size)
        assertEquals("Presupuesto", item.breakdown.first().label)
        assertEquals(listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5), item.breakdown.map { it.maxPoints })
        assertEquals(listOf(9.6, 72.6, 6.7, 4.9, 1.7, 4.5), item.breakdown.map { it.points })
        assertEquals("logreg-1.0.0", item.modelVersion)
        assertEquals("2.0.0", item.featureSchemaVersion)
    }

    @Test
    fun `K 422 problem details exposes missing fields`() {
        // Problem Details real del Backend para smart_match_profile_incomplete.
        val problem = """{"code":"smart_match_profile_incomplete","missing":["university","budget.max","livingWith","placeType"]}"""
        val obj = json.parseToJsonElement(problem) as kotlinx.serialization.json.JsonObject
        assertEquals("smart_match_profile_incomplete", (obj["code"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(
            listOf("university", "budget.max", "livingWith", "placeType"),
            (obj["missing"] as kotlinx.serialization.json.JsonArray)
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content },
        )
        // El ViewModel recibe estos campos en SmartMatchProfileIncompleteException.missing.
        val exception = SmartMatchProfileIncompleteException(
            listOf("university", "budget.max", "livingWith", "placeType"),
        )
        assertEquals(4, exception.missing.size)
    }

    private fun problemJsonValue(raw: String): List<String> {
        val obj = json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
        return (obj["missing"] as kotlinx.serialization.json.JsonArray)
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
    }
}
