package com.thehouse.app.domain.model

import java.time.Instant

enum class VisitStatus {
    REQUESTED,
    CANCELLED;

    companion object {
        fun fromApi(value: String): VisitStatus = when (value.lowercase()) {
            "cancelled" -> CANCELLED
            else -> REQUESTED
        }
    }
}

data class VisitProperty(
    val id: String,
    val title: String,
    val city: String,
)

data class Visit(
    val id: String,
    val property: VisitProperty,
    val scheduledAt: Instant,
    val timeZone: String,
    val status: VisitStatus,
    val message: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val cancelledAt: Instant?,
)

data class VisitCreation(
    val visit: Visit,
    val replayed: Boolean,
)