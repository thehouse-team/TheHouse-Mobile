package com.thehouse.app.data.agreements.mapper

import com.thehouse.app.data.remote.dto.agreements.AgreementBuildingSummaryDto
import com.thehouse.app.data.remote.dto.agreements.AgreementLandlordSummaryDto
import com.thehouse.app.data.remote.dto.agreements.AgreementPreviewResponseDto
import com.thehouse.app.data.remote.dto.agreements.AgreementRoomSummaryDto
import com.thehouse.app.data.remote.dto.agreements.AgreementStudentSummaryDto
import com.thehouse.app.data.remote.dto.agreements.AgreementTermsResponseDto
import com.thehouse.app.data.remote.dto.agreements.ClosingCodeResponseDto
import com.thehouse.app.data.remote.dto.agreements.LandlordAgreementResponseDto
import com.thehouse.app.data.remote.dto.agreements.RentalAddressDto
import com.thehouse.app.data.remote.dto.agreements.RentalLocationDto
import com.thehouse.app.data.remote.dto.agreements.RentalNextPaymentDto
import com.thehouse.app.data.remote.dto.agreements.RentalResponseDto
import com.thehouse.app.domain.model.AgreementBuildingSummary
import com.thehouse.app.domain.model.AgreementLandlordSummary
import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.model.AgreementRoomSummary
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.AgreementStudentSummary
import com.thehouse.app.domain.model.AgreementTerms
import com.thehouse.app.domain.model.ClosingCode
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.model.RentalAddress
import com.thehouse.app.domain.model.RentalLocation
import com.thehouse.app.domain.model.RentalNextPayment

fun AgreementRoomSummaryDto.toDomain(): AgreementRoomSummary = AgreementRoomSummary(
    id = id,
    buildingId = buildingId,
    title = title,
    city = city,
)

fun AgreementTermsResponseDto.toDomain(): AgreementTerms = AgreementTerms(
    monthlyRent = monthlyRent,
    deposit = deposit,
    startDate = startDate,
    durationMonths = durationMonths,
    endDate = endDate,
    paymentDay = paymentDay,
    currency = currency,
)

fun AgreementStudentSummaryDto.toDomain(): AgreementStudentSummary = AgreementStudentSummary(
    name = name,
    university = university,
)

fun ClosingCodeResponseDto.toDomain(): ClosingCode = ClosingCode(
    backupCode = backupCode,
    expiresAt = expiresAt,
)

fun LandlordAgreementResponseDto.toDomain(): LandlordAgreement = LandlordAgreement(
    id = id,
    room = room.toDomain(),
    student = student?.toDomain(),
    terms = terms.toDomain(),
    status = AgreementStatus.fromApi(status),
    closingCode = closingCode?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    activatedAt = activatedAt,
    endedAt = endedAt,
    cancelledAt = cancelledAt,
)

fun AgreementBuildingSummaryDto.toDomain(): AgreementBuildingSummary = AgreementBuildingSummary(
    name = name,
    rules = rules,
)

fun AgreementLandlordSummaryDto.toDomain(): AgreementLandlordSummary = AgreementLandlordSummary(
    name = name,
)

fun AgreementPreviewResponseDto.toDomain(): AgreementPreview = AgreementPreview(
    id = id,
    room = room.toDomain(),
    building = building.toDomain(),
    landlord = landlord.toDomain(),
    terms = terms.toDomain(),
    codeExpiresAt = codeExpiresAt,
)

fun RentalNextPaymentDto.toDomain(): RentalNextPayment = RentalNextPayment(
    dueDate = dueDate,
    amount = amount,
)

fun RentalAddressDto.toDomain(): RentalAddress = RentalAddress(
    street = street,
    number = number,
    neighborhood = neighborhood,
    city = city,
    state = state,
    postalCode = postalCode,
)

fun RentalLocationDto.toDomain(): RentalLocation = RentalLocation(
    lat = lat,
    lng = lng,
)

fun RentalResponseDto.toDomain(): MyRental = MyRental(
    agreementId = agreementId,
    room = room.toDomain(),
    building = building.toDomain(),
    terms = terms.toDomain(),
    nextPayment = nextPayment?.toDomain(),
    address = address.toDomain(),
    location = location?.toDomain(),
    landlord = landlord.toDomain(),
    conversationId = conversationId,
    activatedAt = activatedAt,
)
