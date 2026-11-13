package com.thehouse.app.data.remote.dto.agreements

import kotlinx.serialization.Serializable

@Serializable
data class CreateAgreementRequestDto(
    val requestId: String,
    val monthlyRent: Int,
    val deposit: Int,
    val startDate: String,
    val durationMonths: Int,
    val paymentDay: Int,
)

@Serializable
data class EndAgreementRequestDto(
    val reason: String? = null,
    val note: String? = null,
)

@Serializable
data class ClosingCodeRequestDto(
    val code: String? = null,
    val qrToken: String? = null,
)

@Serializable
data class AcceptAgreementRequestDto(
    val code: String? = null,
    val qrToken: String? = null,
    val acceptedTerms: Boolean,
)

@Serializable
data class AgreementRoomSummaryDto(
    val id: String,
    val buildingId: String,
    val title: String,
    val city: String,
)

@Serializable
data class AgreementTermsResponseDto(
    val monthlyRent: Int,
    val deposit: Int,
    val startDate: String,
    val durationMonths: Int,
    val endDate: String,
    val paymentDay: Int,
    val currency: String,
)

@Serializable
data class AgreementStudentSummaryDto(
    val name: String? = null,
    val university: String? = null,
)

@Serializable
data class ClosingCodeResponseDto(
    val qrToken: String,
    val backupCode: String,
    val expiresAt: String,
)

@Serializable
data class LandlordAgreementResponseDto(
    val id: String,
    val room: AgreementRoomSummaryDto,
    val student: AgreementStudentSummaryDto? = null,
    val terms: AgreementTermsResponseDto,
    val status: String,
    val closingCode: ClosingCodeResponseDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val activatedAt: String? = null,
    val endedAt: String? = null,
    val cancelledAt: String? = null,
)

@Serializable
data class LandlordAgreementListResponseDto(
    val items: List<LandlordAgreementResponseDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class AgreementBuildingSummaryDto(
    val name: String,
    val rules: String? = null,
)

@Serializable
data class AgreementLandlordSummaryDto(
    val name: String,
)

@Serializable
data class AgreementPreviewResponseDto(
    val id: String,
    val room: AgreementRoomSummaryDto,
    val building: AgreementBuildingSummaryDto,
    val landlord: AgreementLandlordSummaryDto,
    val terms: AgreementTermsResponseDto,
    val codeExpiresAt: String,
)

@Serializable
data class RentalNextPaymentDto(
    val dueDate: String,
    val amount: Int,
)

@Serializable
data class RentalAddressDto(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
)

@Serializable
data class RentalLocationDto(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class RentalResponseDto(
    val agreementId: String,
    val room: AgreementRoomSummaryDto,
    val building: AgreementBuildingSummaryDto,
    val terms: AgreementTermsResponseDto,
    val nextPayment: RentalNextPaymentDto? = null,
    val address: RentalAddressDto,
    val location: RentalLocationDto? = null,
    val landlord: AgreementLandlordSummaryDto,
    val conversationId: String,
    val activatedAt: String? = null,
)
