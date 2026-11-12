package com.thehouse.app.data.requests.mapper

import com.thehouse.app.data.remote.dto.requests.RequestLandlordDto
import com.thehouse.app.data.remote.dto.requests.RequestRejectionDto
import com.thehouse.app.data.remote.dto.requests.RequestRoomDto
import com.thehouse.app.data.remote.dto.requests.RequestStudentDto
import com.thehouse.app.data.remote.dto.requests.RequestVisitDto
import com.thehouse.app.data.remote.dto.requests.RoomRequestDto
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.domain.model.RequestLandlord
import com.thehouse.app.domain.model.RequestRejection
import com.thehouse.app.domain.model.RequestRoom
import com.thehouse.app.domain.model.RequestStatus
import com.thehouse.app.domain.model.RequestStudent
import com.thehouse.app.domain.model.RequestVisit
import com.thehouse.app.domain.model.RoomRequest
import java.time.Instant
import java.time.format.DateTimeParseException

fun RoomRequestDto.toDomain(): RoomRequest = RoomRequest(
    id = id,
    status = RequestStatus.fromApi(status),
    room = room.toDomain(),
    quickQuestions = quickQuestions.mapNotNull { QuickQuestion.fromApi(it) },
    message = message,
    student = student?.toDomain(),
    landlord = landlord?.toDomain(),
    visit = visit?.toDomain(),
    conversationId = conversationId,
    rejection = rejection?.toDomain(),
    roomAvailable = roomAvailable,
    createdAt = createdAt?.toInstantOrNull(),
    updatedAt = updatedAt?.toInstantOrNull(),
)

fun RequestRoomDto.toDomain(): RequestRoom = RequestRoom(
    id = id,
    buildingId = buildingId,
    title = title,
    city = city,
)

fun RequestStudentDto.toDomain(): RequestStudent = RequestStudent(
    name = name,
    university = university,
)

fun RequestLandlordDto.toDomain(): RequestLandlord = RequestLandlord(name = name)

fun RequestVisitDto.toDomain(): RequestVisit = RequestVisit(
    id = id,
    scheduledAt = scheduledAt.toInstantOrNull() ?: Instant.EPOCH,
    status = status,
)

fun RequestRejectionDto.toDomain(): RequestRejection = RequestRejection(
    reason = reason,
    note = note,
)

internal fun String.toInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (_: DateTimeParseException) {
    try {
        Instant.parse(replace("+00:00", "Z").replace(" ", "T"))
    } catch (_: DateTimeParseException) {
        null
    }
}
