package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.landlord.DocumentUploadPolicy
import com.thehouse.app.domain.landlord.LandlordFlowPolicies
import com.thehouse.app.domain.landlord.PublicationGuide
import com.thehouse.app.domain.landlord.publicationStepLabel
import com.thehouse.app.domain.model.BuildingDocuments
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.PagedLandlordRoomStats
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.PublicationStep
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.VerificationInfo
import com.thehouse.app.domain.repository.LandlordRepository
import com.thehouse.app.domain.usecase.DeleteBuildingPhotoUseCase
import com.thehouse.app.testutil.TestMainDispatcherRule
import com.thehouse.app.domain.usecase.DeleteBuildingUseCase
import com.thehouse.app.domain.usecase.GetBuildingDocumentsUseCase
import com.thehouse.app.domain.usecase.GetBuildingVerificationUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase
import com.thehouse.app.domain.usecase.GetLandlordStatsRoomsUseCase
import com.thehouse.app.domain.usecase.GetPublicationStatusUseCase
import com.thehouse.app.domain.usecase.PauseRoomUseCase
import com.thehouse.app.domain.usecase.PublishBuildingUseCase
import com.thehouse.app.domain.usecase.ResumeRoomUseCase
import com.thehouse.app.domain.usecase.UploadBuildingPhotoUseCase
import com.thehouse.app.domain.usecase.UploadIneDocumentUseCase
import com.thehouse.app.domain.usecase.UploadPropertyProofDocumentUseCase
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Adenda D21: guía de publicación, validación de documentos, coordenadas y reflejo de estado. */
@OptIn(ExperimentalCoroutinesApi::class)
class D21PublicationFlowTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule(dispatcher)

    companion object {
        private val steps = listOf(
            PublicationStep(PublicationStep.STEP_BUILDING_DATA, true),
            PublicationStep(PublicationStep.STEP_LOCATION, true),
            PublicationStep(PublicationStep.STEP_SERVICES, true),
            PublicationStep(PublicationStep.STEP_ROOM_READY, false),
            PublicationStep(PublicationStep.STEP_BUILDING_PHOTO, false),
            PublicationStep(PublicationStep.STEP_DOCUMENTS, false),
        )
    }

    @Test
    fun `guide progress comes only from real backend steps`() {
        val guide = PublicationGuide.from(PublicationStatus(false, steps), draft = true)
        assertEquals(6, guide.totalCount)
        assertEquals(3, guide.completedCount)
        assertEquals(50, guide.progressPercent)
        assertEquals(3, guide.pendingSteps.size)
        assertEquals(3, guide.completedSteps.size)
        assertFalse(guide.canPublish)
    }

    @Test
    fun `guide orders steps by requested sequence without inventing requirements`() {
        val shuffled = steps.reversed()
        val guide = PublicationGuide.from(PublicationStatus(false, shuffled), draft = true)
        assertEquals(
            listOf(
                PublicationStep.STEP_BUILDING_DATA,
                PublicationStep.STEP_LOCATION,
                PublicationStep.STEP_SERVICES,
                PublicationStep.STEP_ROOM_READY,
                PublicationStep.STEP_BUILDING_PHOTO,
                PublicationStep.STEP_DOCUMENTS,
            ),
            guide.steps.map { it.id },
        )
    }

    @Test
    fun `guide shows no percentage when backend sends no steps`() {
        val guide = PublicationGuide.from(PublicationStatus(false, emptyList()), draft = true)
        assertNull(guide.progressPercent)
        assertEquals(0, guide.totalCount)
        assertNotNull(guide.blockReason)
    }

    @Test
    fun `guide block reason explains missing steps with real labels`() {
        val guide = PublicationGuide.from(PublicationStatus(false, steps), draft = true)
        val reason = guide.blockReason.orEmpty()
        assertTrue(reason.contains(publicationStepLabel(PublicationStep.STEP_ROOM_READY)))
        assertTrue(reason.contains(publicationStepLabel(PublicationStep.STEP_DOCUMENTS)))
    }

    @Test
    fun `guide explains non-draft state instead of claiming a missing step`() {
        val allDone = steps.map { it.copy(done = true) }
        val guide = PublicationGuide.from(PublicationStatus(false, allDone), draft = false)
        assertTrue(guide.blockReason.orEmpty().contains("borrador"))
    }

    @Test
    fun `guide accepts publication only when backend canPublish is true`() {
        val allDone = steps.map { it.copy(done = true) }
        val guide = PublicationGuide.from(PublicationStatus(true, allDone), draft = true)
        assertTrue(guide.canPublish)
        assertNull(guide.blockReason)
        assertEquals(100, guide.progressPercent)
    }

    private fun pdfBytes() = "%PDF-1.4\n%%EOF".toByteArray(Charsets.US_ASCII)
    private fun jpegBytes() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x01)
    private fun pngBytes() = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01)
    private fun webpBytes() = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)

    @Test
    fun `document policy detects real mime by magic bytes`() {
        assertEquals(DocumentUploadPolicy.MIME_PDF, DocumentUploadPolicy.detectMime(pdfBytes()))
        assertEquals(DocumentUploadPolicy.MIME_JPEG, DocumentUploadPolicy.detectMime(jpegBytes()))
        assertEquals(DocumentUploadPolicy.MIME_PNG, DocumentUploadPolicy.detectMime(pngBytes()))
        assertNull(DocumentUploadPolicy.detectMime(webpBytes()))
    }

    @Test
    fun `document policy rejects oversize file before sending`() {
        val size = DocumentUploadPolicy.MAX_DOCUMENT_BYTES + 1
        assertEquals(
            DocumentUploadPolicy.Validation.TooLarge,
            DocumentUploadPolicy.validate(pdfBytes(), size),
        )
        assertTrue(DocumentUploadPolicy.message(DocumentUploadPolicy.Validation.TooLarge).contains("10 MB"))
    }

    @Test
    fun `document policy rejects unsupported format even with pdf extension declared`() {
        assertEquals(
            DocumentUploadPolicy.Validation.UnsupportedFormat,
            DocumentUploadPolicy.validate(webpBytes()),
        )
        assertTrue(
            DocumentUploadPolicy.message(DocumentUploadPolicy.Validation.UnsupportedFormat)
                .contains("PDF, JPEG o PNG"),
        )
    }

    @Test
    fun `document policy rejects empty file`() {
        assertEquals(
            DocumentUploadPolicy.Validation.EmptyOrUnreadable,
            DocumentUploadPolicy.validate(ByteArray(0)),
        )
    }

    @Test
    fun `document policy multipart overhead matches backend constant`() {
        assertEquals(16L * 1024L, DocumentUploadPolicy.MULTIPART_OVERHEAD_BYTES)
        assertEquals(10L * 1024L * 1024L, DocumentUploadPolicy.MAX_DOCUMENT_BYTES)
        assertEquals(
            DocumentUploadPolicy.MAX_DOCUMENT_BYTES + 16L * 1024L,
            DocumentUploadPolicy.MAX_REQUEST_BYTES,
        )
    }

    @Test
    fun `coordinate ranges are contractual and inclusive`() {
        assertEquals(14.0, LandlordFlowPolicies.LATITUDE_MIN, 0.0)
        assertEquals(33.0, LandlordFlowPolicies.LATITUDE_MAX, 0.0)
        assertEquals(-118.5, LandlordFlowPolicies.LONGITUDE_MIN, 0.0)
        assertEquals(-86.0, LandlordFlowPolicies.LONGITUDE_MAX, 0.0)
        assertTrue(LandlordFlowPolicies.COORDINATE_HINT.contains("14 a 33"))
        assertTrue(LandlordFlowPolicies.COORDINATE_HINT.contains("-118.5 a -86"))
    }

    @Test
    fun `uttt distance warns without blocking`() {
        assertTrue(LandlordFlowPolicies.excessiveDistance(1957.9))
        val warning = LandlordFlowPolicies.distanceWarning(1957.9)
        assertNotNull(warning)
        assertTrue(warning!!.contains("universidad"))
        assertNull(LandlordFlowPolicies.distanceWarning(1.4))
        assertNull(LandlordFlowPolicies.distanceWarning(Double.NaN))
    }

    @Test
    fun `stats range uses mexico city date with 30 days`() {
        val today = LocalDate.of(2026, 11, 28)
        val (from, to) = LandlordFlowPolicies.statsRange(today)
        assertEquals("2026-10-30", from)
        assertEquals("2026-11-28", to)
    }

    // ---- ViewModel: refresco sin reiniciar la app ----

    private class FakeReports : com.thehouse.app.domain.repository.ReportsRepository {
        override suspend fun createReport(
            targetType: com.thehouse.app.domain.model.ReportTargetType,
            targetId: String,
            reason: com.thehouse.app.domain.model.ReportReason,
            details: String?,
        ) = error("no usado")
        override suspend fun getBuildingVerification(buildingId: String) =
            Result.success(VerificationInfo(BuildingStatus.DRAFT, null))
    }

    private class FakeRepo : LandlordRepository {
        var publicationCalls = 0
        var publication = PublicationStatus(false, steps)

        override suspend fun getPublication(buildingId: String): Result<PublicationStatus> {
            publicationCalls++
            return Result.success(publication)
        }
        override suspend fun getBuilding(buildingId: String) = Result.success(
            com.thehouse.app.domain.model.Building(
                id = "b1",
                type = com.thehouse.app.domain.model.BuildingType.ROOMS_BUILDING,
                name = "UD3",
                address = com.thehouse.app.domain.model.BuildingAddress(
                    "Av Juárez", "12", "Centro", "Pachuca", "Hidalgo", "43600",
                ),
                location = com.thehouse.app.domain.model.BuildingLocation(20.1, -98.2),
                nearestUniversity = com.thehouse.app.domain.model.NearestUniversity(
                    "uttt", "UTTT", "Universidad Tecnológica de Tula-Tepeji",
                ),
                distanceToUniversityKm = 1957.9,
                walkingMinutes = 23495,
                allowedTenants = com.thehouse.app.domain.model.AllowedTenants.ANY,
                services = emptyList(),
                rules = null,
                status = BuildingStatus.DRAFT,
                roomsTotal = 0,
                roomsAvailable = 0,
                version = 1,
                createdAt = null,
                updatedAt = null,
            ),
        )
        override suspend fun getBuildings(page: Int, pageSize: Int) = error("no usado")
        override suspend fun createProfile(
            fullName: String, phone: String,
            rentalMode: com.thehouse.app.domain.model.RentalMode,
            rfc: String?, acceptedTermsVersion: String,
        ) = error("no usado")
        override suspend fun getProfile() = error("no usado")
        override suspend fun createBuilding(
            type: com.thehouse.app.domain.model.BuildingType,
            name: String,
            address: com.thehouse.app.domain.model.BuildingAddress,
            location: com.thehouse.app.domain.model.BuildingLocation,
            nearestUniversityId: String,
            allowedTenants: com.thehouse.app.domain.model.AllowedTenants,
            services: List<com.thehouse.app.domain.model.Service>,
            rules: String?,
        ) = error("no usado")
        override suspend fun updateBuilding(
            buildingId: String, version: Int, patch: com.thehouse.app.domain.model.BuildingPatch,
        ) = error("no usado")
        override suspend fun deleteBuilding(id: String, version: Int) = error("no usado")
        override suspend fun generateRooms(
            buildingId: String, floors: Int, roomsPerFloor: List<Int>,
            base: com.thehouse.app.domain.model.RoomBase,
        ) = error("no usado")
        override suspend fun getRooms(buildingId: String, page: Int, pageSize: Int) =
            Result.success(com.thehouse.app.domain.model.PagedRooms(emptyList(), page, pageSize, 0L, 0))
        override suspend fun updateRoom(
            id: String, version: Int, patch: com.thehouse.app.domain.model.RoomPatch,
        ) = error("no usado")
        override suspend fun deleteRoom(id: String, version: Int) = error("no usado")
        override suspend fun duplicateRoom(id: String) = error("no usado")
        override suspend fun copyFromRoom(id: String, version: Int, sourceRoomId: String) = error("no usado")
        override suspend fun pauseRoom(id: String) = error("no usado")
        override suspend fun resumeRoom(id: String) = error("no usado")
        override suspend fun publishBuilding(id: String) = error("no usado")
        override suspend fun uploadBuildingPhoto(id: String, file: java.io.File, mimeType: String) = error("no usado")
        override suspend fun deleteBuildingPhoto(id: String) = error("no usado")
        override suspend fun uploadRoomPhoto(roomId: String, file: java.io.File, mimeType: String) = error("no usado")
        override suspend fun deleteRoomPhoto(roomId: String, photoId: String) = error("no usado")
        override suspend fun uploadIneDocument(id: String, file: java.io.File, mimeType: String) = error("no usado")
        override suspend fun uploadPropertyProofDocument(id: String, file: java.io.File, mimeType: String) = error("no usado")
        override suspend fun getBuildingDocuments(id: String) = Result.success(BuildingDocuments())
        override suspend fun getLandlordStatsRooms(
            from: String, to: String, buildingId: String?, page: Int, pageSize: Int,
        ) = Result.success(PagedLandlordRoomStats())
    }

    private fun viewModel(repo: FakeRepo) = BuildingDetailViewModel(
        buildingId = "b1",
        getBuilding = GetLandlordBuildingUseCase(repo),
        getRooms = GetLandlordRoomsUseCase(repo),
        getPublication = GetPublicationStatusUseCase(repo),
        publishBuilding = PublishBuildingUseCase(repo),
        deleteBuilding = DeleteBuildingUseCase(repo),
        pauseRoom = PauseRoomUseCase(repo),
        resumeRoom = ResumeRoomUseCase(repo),
        getVerification = GetBuildingVerificationUseCase(FakeReports()),
        uploadBuildingPhoto = UploadBuildingPhotoUseCase(repo),
        deleteBuildingPhoto = DeleteBuildingPhotoUseCase(repo),
        uploadIneDocument = UploadIneDocumentUseCase(repo),
        uploadPropertyProofDocument = UploadPropertyProofDocumentUseCase(repo),
        getBuildingDocuments = GetBuildingDocumentsUseCase(repo),
    )

    @Test
    fun `publication guide refreshes after successful operation without app restart`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = viewModel(repo)
        runCurrent()
        assertEquals(6, vm.uiState.value.guide.totalCount)
        assertEquals(3, vm.uiState.value.guide.completedCount)

        repo.publication = PublicationStatus(true, steps.map { it.copy(done = true) })
        vm.onEvent(BuildingDetailEvent.Retry)
        runCurrent()
        assertEquals(100, vm.uiState.value.guide.progressPercent)
        assertTrue(vm.uiState.value.guide.canPublish)
        assertTrue(repo.publicationCalls >= 2)
    }

    @Test
    fun `publication incomplete error maps to a user facing reason`() {
        val error = LandlordException(
            DomainError.Conflict,
            LandlordErrorCode.PUBLICATION_INCOMPLETE,
        )
        assertTrue(LandlordErrorMessages.map(error).contains("requisitos"))
    }

    @Test
    fun `document 413 and 415 map to distinct honest messages`() {
        val tooLarge = LandlordException(DomainError.ValidationError, LandlordErrorCode.PAYLOAD_TOO_LARGE)
        val unsupported = LandlordException(DomainError.ValidationError, LandlordErrorCode.UNSUPPORTED_MEDIA)
        assertFalse(LandlordErrorMessages.map(tooLarge) == LandlordErrorMessages.map(unsupported))
        assertTrue(LandlordErrorMessages.map(tooLarge).contains("grande"))
        assertTrue(LandlordErrorMessages.map(unsupported).contains("compatible"))
    }

    @Test
    fun `building status label is human readable`() {
        assertEquals("Borrador", LandlordErrorMessages.statusLabel(BuildingStatus.DRAFT.api))
        assertEquals("En revisión", LandlordErrorMessages.statusLabel(BuildingStatus.IN_REVIEW.api))
    }
}
