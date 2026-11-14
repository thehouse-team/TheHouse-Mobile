package com.thehouse.app.presentation.rental

import com.thehouse.app.domain.model.AgreementBuildingSummary
import com.thehouse.app.domain.model.AgreementLandlordSummary
import com.thehouse.app.domain.model.AgreementPreview
import com.thehouse.app.domain.model.AgreementRoomSummary
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.domain.model.AgreementTerms
import com.thehouse.app.domain.model.LandlordAgreement
import com.thehouse.app.domain.model.MyRental
import com.thehouse.app.domain.model.PagedResult
import com.thehouse.app.domain.model.RentalAddress
import com.thehouse.app.domain.repository.AgreementsRepository
import com.thehouse.app.domain.usecase.AcceptAgreementUseCase
import com.thehouse.app.domain.usecase.PreviewAgreementUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgreementPreviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val preview = AgreementPreview(
        id = "ag-1",
        room = AgreementRoomSummary("room-1", "b-1", "Cuarto 1", "Tulancingo"),
        building = AgreementBuildingSummary("Casa Norte", "Reglas"),
        landlord = AgreementLandlordSummary("Carlos"),
        terms = AgreementTerms(4500, 4500, "2026-12-01", 6, "2027-05-31", 5, "MXN"),
        codeExpiresAt = "2026-11-13T10:10:00Z",
    )

    private val rental = MyRental(
        agreementId = "ag-1",
        room = preview.room,
        building = preview.building,
        terms = preview.terms,
        nextPayment = null,
        address = RentalAddress("Calle 1", "10", "Centro", "Tulancingo", "Hidalgo", "43600"),
        location = null,
        landlord = preview.landlord,
        conversationId = "conv-1",
        activatedAt = null,
    )

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `accept requires explicit terms and does not auto accept`() = runTest {
        var acceptCalls = 0
        val base = FakeAgreementsRepository(preview = preview, rental = rental)
        val acceptRepo = object : AgreementsRepository by base {
            override suspend fun acceptAgreement(code: String?, qrToken: String?, acceptedTerms: Boolean): Result<MyRental> {
                acceptCalls++
                assertTrue(acceptedTerms)
                return Result.success(rental)
            }
        }
        val vm = AgreementPreviewViewModel(
            code = "k7pm-2qxa",
            previewAgreement = PreviewAgreementUseCase(base),
            acceptAgreement = AcceptAgreementUseCase(acceptRepo),
        )
        advanceUntilIdle()
        assertEquals(preview, vm.uiState.value.preview)
        assertFalse(vm.uiState.value.acceptedTerms)
        vm.onEvent(AgreementPreviewEvent.Accept)
        advanceUntilIdle()
        assertEquals(0, acceptCalls)

        vm.onEvent(AgreementPreviewEvent.AcceptedTermsChanged(true))
        vm.onEvent(AgreementPreviewEvent.Accept)
        advanceUntilIdle()
        assertEquals(1, acceptCalls)
        assertFalse(vm.uiState.value.isAccepting)
    }

    private class FakeAgreementsRepository(
        private val preview: AgreementPreview? = null,
        private val rental: MyRental? = null,
    ) : AgreementsRepository {
        override suspend fun createAgreement(requestId: String, monthlyRent: Int, deposit: Int, startDate: String, durationMonths: Int, paymentDay: Int): Result<LandlordAgreement> = error("unused")
        override suspend fun getLandlordAgreements(status: AgreementStatus?, roomId: String?, page: Int, pageSize: Int): Result<PagedResult<LandlordAgreement>> = error("unused")
        override suspend fun getLandlordAgreement(id: String): Result<LandlordAgreement> = error("unused")
        override suspend fun issueAgreementCode(id: String): Result<LandlordAgreement> = error("unused")
        override suspend fun cancelAgreement(id: String): Result<LandlordAgreement> = error("unused")
        override suspend fun endAgreement(id: String, reason: String?, note: String?): Result<LandlordAgreement> = error("unused")
        override suspend fun previewAgreement(code: String?, qrToken: String?): Result<AgreementPreview> = Result.success(preview!!)
        override suspend fun acceptAgreement(code: String?, qrToken: String?, acceptedTerms: Boolean): Result<MyRental> = Result.success(rental!!)
        override suspend fun getMyRental(): Result<MyRental> = Result.success(rental!!)
    }
}
