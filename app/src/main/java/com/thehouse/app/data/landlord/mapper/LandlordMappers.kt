package com.thehouse.app.data.landlord.mapper

import com.thehouse.app.data.remote.dto.landlord.BuildingAddressDto
import com.thehouse.app.data.remote.dto.landlord.BuildingDto
import com.thehouse.app.data.remote.dto.landlord.BuildingLocationDto
import com.thehouse.app.data.remote.dto.landlord.DashboardSummaryDto
import com.thehouse.app.data.remote.dto.landlord.LandlordProfileDto
import com.thehouse.app.data.remote.dto.landlord.PublicationStatusDto
import com.thehouse.app.data.remote.dto.landlord.RoomDto
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.DashboardSummary
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.LandlordProfile
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.NearestUniversity
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.domain.model.RoomType
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
)

fun PublicationStatusDto.toDomain(): PublicationStatus = PublicationStatus(
    canPublish = canPublish,
    steps = steps.map { com.thehouse.app.domain.model.PublicationStep(id = it.id, done = it.done) },
)
