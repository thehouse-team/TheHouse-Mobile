package com.thehouse.app.data.landlord.mapper

import com.thehouse.app.data.remote.dto.landlord.BuildingAddressDto
import com.thehouse.app.data.remote.dto.landlord.BuildingDocumentDto
import com.thehouse.app.data.remote.dto.landlord.BuildingDocumentsResponseDto
import com.thehouse.app.data.remote.dto.landlord.BuildingDto
import com.thehouse.app.data.remote.dto.landlord.BuildingLocationDto
import com.thehouse.app.data.remote.dto.landlord.DashboardSummaryDto
import com.thehouse.app.data.remote.dto.landlord.LandlordProfileDto
import com.thehouse.app.data.remote.dto.landlord.MediaItemDto
import com.thehouse.app.data.remote.dto.landlord.LandlordRatingDto
import com.thehouse.app.data.remote.dto.landlord.LandlordRoomStatsDto
import com.thehouse.app.data.remote.dto.landlord.PublicationStatusDto
import com.thehouse.app.data.remote.dto.landlord.RoomDto
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingDocument
import com.thehouse.app.domain.model.BuildingDocuments
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.DashboardSummary
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.LandlordProfile
import com.thehouse.app.domain.model.MediaItem
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.NearestUniversity
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.domain.model.RoomType
import com.thehouse.app.domain.model.LandlordRating
import com.thehouse.app.domain.model.LandlordRoomStats
import com.thehouse.app.domain.model.Service

/** Mapeo DTO → dominio. Enums EXACTOS; valor desconocido cae al default documentado. */
fun LandlordProfileDto.toDomain(): LandlordProfile = LandlordProfile(
    fullName = fullName,
    phone = phone,
    rentalMode = if (rentalMode == RentalMode.AGENCY.api) RentalMode.AGENCY else RentalMode.INDIVIDUAL,
    rfc = rfc,
    acceptedTermsVersion = acceptedTermsVersion,
    createdAt = createdAt,
)

fun MediaItemDto.toDomain(): MediaItem = MediaItem(
    id = id,
    url = url,
    thumbUrl = thumbUrl,
    width = width,
    height = height,
    bytes = bytes,
    createdAt = createdAt,
)

fun BuildingDto.toDomain(): Building = Building(
    id = id,
    type = BuildingType.fromApi(type),
    name = name,
    address = address.toDomain(),
    location = location.toDomain(),
    nearestUniversity = NearestUniversity(
        id = nearestUniversity.id,
        code = nearestUniversity.code,
        name = nearestUniversity.name,
    ),
    distanceToUniversityKm = distanceToUniversityKm,
    walkingMinutes = walkingMinutes,
    allowedTenants = AllowedTenants.fromApi(allowedTenants),
    services = services.mapNotNull(Service::fromApi),
    rules = rules,
    status = BuildingStatus.fromApi(status),
    roomsTotal = roomsTotal,
    roomsAvailable = roomsAvailable,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
    photo = photo?.toDomain(),
)

fun DashboardSummaryDto.toDomain(): DashboardSummary = DashboardSummary(
    buildings = buildings,
    rooms = rooms,
    available = available,
    occupancyPercent = occupancyPercent,
    monthlyIncome = monthlyIncome,
)

fun BuildingAddressDto.toDomain(): BuildingAddress = BuildingAddress(
    street = street,
    number = number,
    neighborhood = neighborhood,
    city = city,
    state = state,
    postalCode = postalCode,
)

fun BuildingLocationDto.toDomain(): BuildingLocation = BuildingLocation(lat = lat, lng = lng)

fun RoomDto.toDomain(): Room = Room(
    id = id,
    buildingId = buildingId,
    number = number,
    floor = floor,
    price = price,
    currency = currency,
    sizeM2 = sizeM2,
    type = RoomType.fromApi(type),
    bathroom = BathroomType.fromApi(bathroom),
    furniture = furniture.mapNotNull(Furniture::fromApi),
    availableFrom = availableFrom,
    minContract = MinContract.fromApi(minContract),
    status = RoomStatus.fromApi(status),
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
    photos = photos.map { it.toDomain() },
)

fun BuildingDocumentDto.toDomain(): BuildingDocument = BuildingDocument(
    type = type,
    id = id,
    fileName = fileName,
    status = status,
    createdAt = createdAt,
)

fun BuildingDocumentsResponseDto.toDomain(): BuildingDocuments {
    val list = items.map { it.toDomain() }
    val ineDoc = ine?.toDomain() ?: list.firstOrNull { it.type.equals("ine", ignoreCase = true) }
    val proofDoc = propertyProof?.toDomain() ?: list.firstOrNull {
        it.type.equals("property_proof", ignoreCase = true)
    }
    return BuildingDocuments(ine = ineDoc, propertyProof = proofDoc, raw = list)
}

fun PublicationStatusDto.toDomain(): PublicationStatus = PublicationStatus(
    canPublish = canPublish,
    steps = steps.map { com.thehouse.app.domain.model.PublicationStep(id = it.id, done = it.done) },
)

fun LandlordRoomStatsDto.toDomain(): LandlordRoomStats = LandlordRoomStats(
    roomId = roomId,
    buildingId = buildingId,
    buildingName = buildingName,
    number = number,
    status = status,
    price = price,
    views = views,
    favorites = favorites,
    requests = requests,
    visits = visits,
    agreements = agreements,
    conversion = conversion,
    rating = rating?.toDomain(),
)

fun LandlordRatingDto.toDomain(): LandlordRating = LandlordRating(
    average = average,
    count = count,
)
