package com.thehouse.app.data.remote.dto.profile

import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile
import kotlinx.serialization.Serializable

/**
 * DTO de GET /api/v1/users/me/profile. Solo mapeo, sin lógica de negocio.
 *
 * D19.1: espejo exacto de `ProfileResponse` del Backend. `UpdatedAt` es
 * `DateTimeOffset?`, por lo que puede ser `null` o estar ausente. Declararlo
 * como `String` no nullable provocaba `SerializationException` en cada 200
 * con `updatedAt: null`, antes de llegar al mapper (error genérico en la UI).
 */
@Serializable
data class ProfileDto(
    val name: String?,
    val university: UniversityDto?,
    val quickSetupCompleted: Boolean = false,
    val updatedAt: String? = null,
)

@Serializable
data class UniversityDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val city: String? = null,
)

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    name = name,
    university = university?.toDomain(),
    quickSetupCompleted = quickSetupCompleted,
)

fun UniversityDto.toDomain(): University = University(
    id = id ?: "",
    code = code ?: "",
    name = name ?: "",
    city = city ?: "",
    country = "",
    createdAt = "",
)
