package com.thehouse.app.domain.model

enum class AgreementStatus(val api: String) {
    PENDING("pending"),
    ACTIVE("active"),
    ENDED("ended"),
    CANCELLED("cancelled");

    companion object {
        fun fromApi(value: String?): AgreementStatus = when (value?.lowercase()) {
            "active" -> ACTIVE
            "ended" -> ENDED
            "cancelled" -> CANCELLED
            else -> PENDING
        }
    }
}

enum class EndReason(val api: String) {
    CONTRACT_FINISHED("contract_finished"),
    EARLY_TERMINATION("early_termination"),
    OTHER("other");

    companion object {
        fun fromApi(value: String?): EndReason? = when (value?.lowercase()) {
            "contract_finished" -> CONTRACT_FINISHED
            "early_termination" -> EARLY_TERMINATION
            "other" -> OTHER
            else -> null
        }
    }
}

data class AgreementRoomSummary(
    val id: String,
    val buildingId: String,
    val title: String,
    val city: String,
)

data class AgreementTerms(
    val monthlyRent: Int,
    val deposit: Int,
    val startDate: String,
    val durationMonths: Int,
    val endDate: String,
    val paymentDay: Int,
    val currency: String,
)

data class AgreementStudentSummary(
    val name: String?,
    val university: String?,
)

data class ClosingCode(
    val backupCode: String,
    val expiresAt: String,
)

data class LandlordAgreement(
    val id: String,
    val room: AgreementRoomSummary,
    val student: AgreementStudentSummary?,
    val terms: AgreementTerms,
    val status: AgreementStatus,
    val closingCode: ClosingCode?,
    val createdAt: String?,
    val updatedAt: String?,
    val activatedAt: String?,
    val endedAt: String?,
    val cancelledAt: String?,
)

data class AgreementBuildingSummary(
    val name: String,
    val rules: String?,
)

data class AgreementLandlordSummary(
    val name: String,
)

data class AgreementPreview(
    val id: String,
    val room: AgreementRoomSummary,
    val building: AgreementBuildingSummary,
    val landlord: AgreementLandlordSummary,
    val terms: AgreementTerms,
    val codeExpiresAt: String,
)

data class RentalNextPayment(
    val dueDate: String,
    val amount: Int,
)

data class RentalAddress(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
)

data class RentalLocation(
    val lat: Double,
    val lng: Double,
)

data class MyRental(
    val agreementId: String,
    val room: AgreementRoomSummary,
    val building: AgreementBuildingSummary,
    val terms: AgreementTerms,
    val nextPayment: RentalNextPayment?,
    val address: RentalAddress,
    val location: RentalLocation?,
    val landlord: AgreementLandlordSummary,
    val conversationId: String,
    val activatedAt: String?,
)
