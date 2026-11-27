package com.thehouse.app.data.remote.dto.account

import kotlinx.serialization.Serializable

/** Espejo de `DeletionPreviewResponse` del Backend confirmado. */
@Serializable
data class DeletionPreviewDto(
    val canDelete: Boolean,
    val blockers: List<DeletionBlockerDto> = emptyList(),
    val effects: DeletionEffectsDto,
)

@Serializable
data class DeletionBlockerDto(
    val code: String,
    val count: Int,
)

@Serializable
data class DeletionEffectsDto(
    val buildings: Int = 0,
    val rooms: Int = 0,
    val upcomingVisits: Int = 0,
    val openRequests: Int = 0,
    val pendingAgreements: Int = 0,
    val conversations: Int = 0,
    val reviews: Int = 0,
    val favorites: Int = 0,
    val roommateProfile: Boolean = false,
)

/** Cuerpo exacto del POST: "ELIMINAR" es case-sensitive (lo valida el Backend). */
@Serializable
data class AccountDeletionRequestDto(
    val password: String,
    val confirmation: String,
)
