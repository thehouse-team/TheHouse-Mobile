package com.thehouse.app.domain.model

/** Modelo de dominio del preview de eliminación (sin infraestructura). */
data class DeletionPreview(
    val canDelete: Boolean,
    val blockers: List<DeletionBlocker>,
    val effects: DeletionEffects,
) {
    val summary: String
        get() = buildString {
            if (!canDelete) append("No es posible eliminar la cuenta en este momento. ")
            if (blockers.isNotEmpty()) {
                append("Bloqueos: ").append(
                    blockers.joinToString { "${it.code} (${it.count})" }
                ).append(". ")
            }
            append("Se eliminarían ").append(effects.rooms).append(" cuartos, ")
                .append(effects.favorites).append(" favoritos, ")
                .append(effects.conversations).append(" conversaciones y ")
                .append(effects.requests).append(" solicitudes.")
        }
}

data class DeletionBlocker(val code: String, val count: Int)

data class DeletionEffects(
    val buildings: Int,
    val rooms: Int,
    val upcomingVisits: Int,
    val openRequests: Int,
    val pendingAgreements: Int,
    val conversations: Int,
    val reviews: Int,
    val favorites: Int,
    val roommateProfile: Boolean,
) {
    /** Alias legible usado en el resumen. */
    val requests: Int get() = openRequests
}
