package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.repository.AgreementsRepository

class CreateAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(
        requestId: String,
        monthlyRent: Int,
        deposit: Int,
        startDate: String,
        durationMonths: Int,
        paymentDay: Int,
    ): Result<LandlordAgreement> = repository.createAgreement(
        requestId = requestId,
        monthlyRent = monthlyRent,
        deposit = deposit,
        startDate = startDate,
        durationMonths = durationMonths,
        paymentDay = paymentDay,
    )
}

class GetLandlordAgreementsUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(
        status: AgreementStatus? = null,
        roomId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<LandlordAgreement>> = repository.getLandlordAgreements(status, roomId, page, pageSize)
}

class GetLandlordAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(id: String): Result<LandlordAgreement> = repository.getLandlordAgreement(id)
}

class IssueAgreementCodeUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(id: String): Result<LandlordAgreement> = repository.issueAgreementCode(id)
}

class CancelAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(id: String): Result<LandlordAgreement> = repository.cancelAgreement(id)
}

class EndAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(
        id: String,
        reason: String? = null,
        note: String? = null,
    ): Result<LandlordAgreement> = repository.endAgreement(id, reason, note)
}

class PreviewAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(
        code: String? = null,
        qrToken: String? = null,
    ): Result<AgreementPreview> = repository.previewAgreement(code, qrToken)
}

class AcceptAgreementUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(
        code: String? = null,
        qrToken: String? = null,
        acceptedTerms: Boolean,
    ): Result<MyRental> = repository.acceptAgreement(code, qrToken, acceptedTerms)
}

class GetMyRentalUseCase(private val repository: AgreementsRepository) {
    suspend operator fun invoke(): Result<MyRental> = repository.getMyRental()
}
