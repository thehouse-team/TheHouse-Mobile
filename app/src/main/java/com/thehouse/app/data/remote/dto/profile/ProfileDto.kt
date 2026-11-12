package com.thehouse.app.data.remote.dto.profile

import com.thehouse.app.domain.model.University
import com.thehouse.app.domain.model.UserProfile

/**
 * DTO de GET /api/v1/users/me/profile. Solo mapeo, sin lógica de negocio.
 */
data class ProfileDto(
    val name: String?,
    val university: UniversityDto?,
    val quickSetupCompleted: Boolean,
    val updatedAt: String
)

data class UniversityDto(
    val id: String?,
    val code: String?,
    val name: String?,
    val city: String?
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
