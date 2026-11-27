package com.thehouse.app.domain.model

import java.time.Instant

enum class VisitStatus {
    REQUESTED,
    CONFIRMED,
    CANCELLED;

    companion object {
        fun fromApi(value: String): VisitStatus = when (value.lowercase()) {
            "cancelled" -> CANCELLED
            "confirmed" -> CONFIRMED
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
    /** roomId real (D20) o propertyId legacy cuando no hay room. */
    val roomId: String = "",
    val property: VisitProperty,
    val scheduledAt: Instant,
    val timeZone: String,
    val status: VisitStatus,
    val message: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val cancelledAt: Instant?,
    /** Solo presente si el Backend ya reveló la dirección (confirmada). */
    val hasAddress: Boolean = false,
)

data class VisitCreation(
    val visit: Visit,
    /** true en replay 200 con la misma Idempotency-Key. */
    val replayed: Boolean,
)
