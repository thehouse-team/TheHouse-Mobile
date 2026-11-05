package com.thehouse.app.data.auth.mapper

import com.thehouse.app.data.remote.dto.auth.AuthResponseDto
import com.thehouse.app.data.remote.dto.auth.ProblemDetailsDto
import com.thehouse.app.domain.model.UserSession
import kotlinx.serialization.json.Json

/**
 * Mapeo DTO ↔ Domain. Solo transformaciones: no crea dependencia Domain → Data.
 */
object AuthMappers {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun AuthResponseDto.toDomain(): UserSession = UserSession(
        userId = user?.id ?: user?.userId ?: id ?: userId,
        displayName = user?.name ?: displayName ?: name?.ifBlank { null } ?: FALLBACK_NAME,
        email = user?.email ?: email.orEmpty(),
    )

    fun parseProblemDetails(errorBody: String?): ProblemDetailsDto? = runCatching {
        errorBody?.takeIf { it.isNotBlank() }?.let { json.decodeFromString<ProblemDetailsDto>(it) }
    }.getOrNull()

    fun ProblemDetailsDto.toDetailMessage(): String? =
        detail?.ifBlank { null } ?: title?.ifBlank { null }

    private const val FALLBACK_NAME = "Bienvenido"
}
