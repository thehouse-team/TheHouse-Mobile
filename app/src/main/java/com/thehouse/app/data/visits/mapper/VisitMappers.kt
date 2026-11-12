package com.thehouse.app.data.visits.mapper

import com.thehouse.app.data.remote.dto.visits.VisitDto
import com.thehouse.app.domain.model.Visit
import com.thehouse.app.domain.model.VisitProperty
import com.thehouse.app.domain.model.VisitStatus
import java.time.Instant
import java.time.OffsetDateTime

fun VisitDto.toDomain(): Visit = Visit(
    id = id,
    property = VisitProperty(
        id = property.id,
        title = property.title,
        city = property.city,
    ),
    scheduledAt = parseInstant(scheduledAt) ?: Instant.EPOCH,
    timeZone = timeZone,
    status = VisitStatus.fromApi(status),
    message = message,
    createdAt = createdAt?.let(::parseInstant),
    updatedAt = updatedAt?.let(::parseInstant),
    cancelledAt = cancelledAt?.let(::parseInstant),
)

private fun parseInstant(value: String): Instant? = runCatching {
    OffsetDateTime.parse(value).toInstant()
}.recoverCatching {
    Instant.parse(value)
}.getOrNull()