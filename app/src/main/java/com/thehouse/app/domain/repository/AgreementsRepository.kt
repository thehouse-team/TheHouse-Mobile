package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.model.PagedResult

interface AgreementsRepository {
    suspend fun createAgreement(
        requestId: String,
        monthlyRent: Int,
        deposit: Int,
        startDate: String,
        durationMonths: Int,
        paymentDay: Int,
    ): Result<LandlordAgreement>

    suspend fun getLandlordAgreements(
        status: AgreementStatus? = null,
        roomId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PagedResult<LandlordAgreement>>

    suspend fun getLandlordAgreement(id: String): Result<LandlordAgreement>

    suspend fun issueAgreementCode(id: String): Result<LandlordAgreement>

    suspend fun cancelAgreement(id: String): Result<LandlordAgreement>

    suspend fun endAgreement(
        id: String,
        reason: String? = null,
        note: String? = null,
    ): Result<LandlordAgreement>

    suspend fun previewAgreement(
        code: String? = null,
        qrToken: String? = null,
    ): Result<AgreementPreview>

    suspend fun acceptAgreement(
        code: String? = null,
        qrToken: String? = null,
        acceptedTerms: Boolean,
    ): Result<MyRental>

    suspend fun getMyRental(): Result<MyRental>
}
