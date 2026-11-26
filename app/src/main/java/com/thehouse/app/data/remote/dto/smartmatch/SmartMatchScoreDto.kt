package com.thehouse.app.data.remote.dto.smartmatch

import kotlinx.serialization.Serializable

/**
 * Request for POST /api/v1/smart-match/scores
 * 1-50 roomIds. Sin token: añade universityId y preferences.
 */
@Serializable
data class SmartMatchScoreRequest(
    val roomIds: List<String>,
    val universityId: String? = null,
    val preferences: SmartMatchPreferencesDto? = null
)

/**
 * DTO item dentro del response de scores.
 * Contract NV-23 v1.2.0: matchScore (nullable), breakdown, dismissed.
 */
@Serializable
data class SmartMatchScoreItem(
    val roomId: String,
    val matchScore: Double?,          // nullable: room no elegible devuelve null
    val breakdown: List<SmartMatchBreakdownItem>,  // vacía si no elegible
    val dismissed: Boolean,
    val excludedBy: List<String> = emptyList()    // múltiples motivos
)

/** Item de breakdown según contrato NV-23 */
@Serializable
data class SmartMatchBreakdownItem(
    val factor: String,
    val label: String,
    val points: Double,              // soporta decimal, NO redondear a entero
    val maxPoints: Double
)

/**
 * DTO preferences resumen para query pública y scores.
 */
@Serializable
data class SmartMatchPreferencesDto(
    val budget: SmartMatchBudgetDto?,
    val essentialServices: List<String>?,
    val livingWith: String?,
    val placeType: String?,
    @kotlinx.serialization.Transient val pets: String? = null,
    val transport: String?,
    val propertyTypes: List<String>?,
    @kotlinx.serialization.Transient val currency: String? = null,
)

@Serializable
data class SmartMatchBudgetDto(
    val min: Double?,
    val max: Double?,
    val currency: String?
)

/**
 * Request for POST /api/v1/smart-match/recommendations/query
 * Sin token. universityId y preferences.
 */
@Serializable
data class SmartMatchQueryRequest(
    val universityId: String?,
    val preferences: SmartMatchPreferencesDto?
)

/**
 * DTO recommendation según contrato NV-23.
 */
@Serializable
data class SmartMatchRecommendationDto(
    val roomId: String,
    val title: String?,
    val price: Double?,
    val currency: String?,
    val matchScore: Int?,             // contrato real: entero 0..100 (puede ser null si no elegible)
    val dismissed: Boolean,
    val excludedBy: List<String> = emptyList(),
    val breakdown: List<SmartMatchFactorDto> = emptyList(),
    val modelVersion: String = "",
)

/**
 * Response wrapping for recommendations list.
 */
@Serializable
data class SmartMatchRecommendationResponse(
    val items: List<SmartMatchRecommendationDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0
)

/**
 * RoomSearchItem del contrato NV-14 (Backend `SearchContracts.RoomSearchItem`),
 * anidado dentro de cada recomendación NV-23 v1.2.0. Sin dirección real:
 * `approxLocation` es la celda aproximada publicada por el Backend.
 */
@Serializable
data class SmartMatchRoomSearchDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val price: Double,
    val currency: String,
    val roomType: String? = null,
    val bathroom: String? = null,
    val buildingType: String,
    val allowedTenants: String,
    val services: List<String> = emptyList(),
    val distanceKm: Double,
    val walkingMinutes: Int,
    val approxLocation: SmartMatchApproxLocationDto? = null,
    val thumbnailUrl: String? = null,
    val demo: Boolean = false,
)

@Serializable
data class SmartMatchApproxLocationDto(
    val lat: Double,
    val lng: Double,
)

/** Factor del desglose expuesto por el Backend (sin valores de features). */
@Serializable
data class SmartMatchFactorDto(
    val factor: String,
    val label: String,
    val points: Double,
    val maxPoints: Double,
)

/**
 * Item real de `GET /api/v1/smart-match/recommendations` (NV-23 v1.2.0 + D16).
 * El Backend es autoridad de `matchScore`, `breakdown`, `dismissed` y orden.
 *
 * Contrato confirmado: `matchScore` SIEMPRE entero 0..100. El breakdown
 * (`points`/`maxPoints`) sigue siendo Double con decimales reales.
 */
@Serializable
data class SmartMatchRecommendationItemDto(
    val room: SmartMatchRoomSearchDto,
    val matchScore: Int,
    val breakdown: List<SmartMatchFactorDto> = emptyList(),
    val dismissed: Boolean = false,
    val modelVersion: String,
    val featureSchemaVersion: String,
)

/**
 * Response for GET /api/v1/smart-match/recommendations
 */
@Serializable
data class SmartMatchListResponse(
    val items: List<SmartMatchRecommendationItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

/** Error 422 perfil incompleto */
@Serializable
data class SmartMatchProfileIncompleteError(
    val code: String = "smart_match_profile_incomplete",
    val missing: List<String> = emptyList()  // ["university", "budget.max", "livingWith", "placeType"]
)

/** Error genérico para smart match */
@Serializable
data class SmartMatchError(
    val code: String?,
    val message: String?
)

/** Response for GET /api/v1/smart-match/explanations/{roomId} (AUTH) y POST /api/v1/smart-match/explanations/query (GUEST) */
@Serializable
data class SmartMatchExplanationResponse(
    val roomId: String,
    val score: Double,
    val summary: String,
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val generatedBy: String,         // "template" | "genai"
    val modelVersion: String,
    val explanationVersion: String
)

/** Request for POST /api/v1/smart-match/explanations/query (GUEST) */
@Serializable
data class SmartMatchExplanationQueryRequest(
    val roomId: String,
    val universityId: String?,
    val preferences: SmartMatchPreferencesDto?
)