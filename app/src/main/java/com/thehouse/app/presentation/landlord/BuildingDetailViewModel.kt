package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.landlord.PublicationGuide
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingDocuments
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.usecase.DeleteBuildingPhotoUseCase
import com.thehouse.app.domain.usecase.DeleteBuildingUseCase
import com.thehouse.app.domain.usecase.GetBuildingDocumentsUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase
import com.thehouse.app.domain.usecase.GetPublicationStatusUseCase
import com.thehouse.app.domain.usecase.PauseRoomUseCase
import com.thehouse.app.domain.usecase.PublishBuildingUseCase
import com.thehouse.app.domain.usecase.ResumeRoomUseCase
import com.thehouse.app.domain.usecase.UploadBuildingPhotoUseCase
import com.thehouse.app.domain.usecase.UploadIneDocumentUseCase
import com.thehouse.app.domain.usecase.UploadPropertyProofDocumentUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent
import java.io.File

/**
 * Detalle de edificio + cuartos + publicación.
 * La `version` del edificio y de cada cuarto proviene SIEMPRE del Backend.
 */
data class BuildingDetailUiState(
    val isLoading: Boolean = false,
    val building: Building? = null,
    val rooms: List<Room> = emptyList(),
    val publication: PublicationStatus? = null,
    /** Guía derivada del endpoint real; se recalcula tras cada operación exitosa. */
    val guide: PublicationGuide = PublicationGuide.from(null, draft = false),
    val documents: BuildingDocuments? = null,
    val isPublishing: Boolean = false,
    val isDeleting: Boolean = false,
    val busyRoomId: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isUploadingDocument: Boolean = false,
    val verification: com.thehouse.app.domain.model.VerificationInfo? = null,
    val versionConflict: Boolean = false,
    /** Mensaje tras publicar: el estado real lo dice el Backend (in_review). */
    val infoMessage: String? = null,
    val errorCode: LandlordErrorCode? = null,
    val errorMessage: String? = null,
    val deleted: Boolean = false,
)

sealed interface BuildingDetailEvent : UiEvent {
    data object Load : BuildingDetailEvent
    data object Retry : BuildingDetailEvent
    data object Publish : BuildingDetailEvent
    data object Delete : BuildingDetailEvent
    data class PauseRoom(val roomId: String) : BuildingDetailEvent
    data class ResumeRoom(val roomId: String) : BuildingDetailEvent
    data class UploadBuildingPhoto(val file: File, val mimeType: String) : BuildingDetailEvent
    data object DeleteBuildingPhoto : BuildingDetailEvent
    data class UploadIne(val file: File, val mimeType: String) : BuildingDetailEvent
    data class UploadPropertyProof(val file: File, val mimeType: String) : BuildingDetailEvent
}

sealed interface BuildingDetailEffect : com.thehouse.app.presentation.common.UiEffect {
    data object Deleted : BuildingDetailEffect
    data class Published(val building: Building) : BuildingDetailEffect
}

class BuildingDetailViewModel(
    private val buildingId: String,
    private val getBuilding: GetLandlordBuildingUseCase,
    private val getRooms: GetLandlordRoomsUseCase,
    private val getPublication: GetPublicationStatusUseCase,
    private val publishBuilding: PublishBuildingUseCase,
    private val deleteBuilding: DeleteBuildingUseCase,
    private val pauseRoom: PauseRoomUseCase,
    private val resumeRoom: ResumeRoomUseCase,
    private val getVerification: com.thehouse.app.domain.usecase.GetBuildingVerificationUseCase,
    private val uploadBuildingPhoto: UploadBuildingPhotoUseCase,
    private val deleteBuildingPhoto: DeleteBuildingPhotoUseCase,
    private val uploadIneDocument: UploadIneDocumentUseCase,
    private val uploadPropertyProofDocument: UploadPropertyProofDocumentUseCase,
    private val getBuildingDocuments: GetBuildingDocumentsUseCase,
) : BaseViewModel<BuildingDetailUiState, BuildingDetailEvent, BuildingDetailEffect>(
    BuildingDetailUiState(),
) {

    init {
        load()
    }

    override fun onEvent(event: BuildingDetailEvent) {
        when (event) {
            BuildingDetailEvent.Load, BuildingDetailEvent.Retry -> load()
            BuildingDetailEvent.Publish -> publish()
            BuildingDetailEvent.Delete -> delete()
            is BuildingDetailEvent.PauseRoom -> changeRoomState(event.roomId, pause = true)
            is BuildingDetailEvent.ResumeRoom -> changeRoomState(event.roomId, pause = false)
            is BuildingDetailEvent.UploadBuildingPhoto -> uploadPhoto(event.file, event.mimeType)
            BuildingDetailEvent.DeleteBuildingPhoto -> removePhoto()
            is BuildingDetailEvent.UploadIne -> uploadDocument(ine = true, event.file, event.mimeType)
            is BuildingDetailEvent.UploadPropertyProof -> uploadDocument(ine = false, event.file, event.mimeType)
        }
    }

    fun load() {
        updateState {
            it.copy(
                isLoading = true,
                versionConflict = false,
                errorCode = null,
                errorMessage = null,
            )
        }
        launch {
            getBuilding(buildingId).fold(
                onSuccess = { building ->
                    updateState { it.copy(isLoading = false, building = building) }
                    loadRooms()
                    loadPublication()
                    loadVerification()
                    loadDocuments()
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun loadRooms() {
        launch {
            getRooms(buildingId).fold(
                onSuccess = { page -> updateState { it.copy(rooms = page.items) } },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun loadVerification() {
        launch {
            getVerification(buildingId).fold(
                onSuccess = { info -> updateState { it.copy(verification = info) } },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun loadPublication() {
        launch {
            getPublication(buildingId).fold(
                onSuccess = { publication ->
                    updateState { current ->
                        current.copy(
                            publication = publication,
                            guide = PublicationGuide.from(
                                publication,
                                draft = current.building?.status == BuildingStatus.DRAFT,
                            ),
                        )
                    }
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    /** Recalcula la guía sin nueva petición HTTP cuando cambia el estado local del edificio. */
    private fun refreshGuide() {
        updateState { current ->
            current.copy(
                guide = PublicationGuide.from(
                    current.publication,
                    draft = current.building?.status == BuildingStatus.DRAFT,
                ),
            )
        }
    }

    private fun loadDocuments() {
        launch {
            getBuildingDocuments(buildingId).fold(
                onSuccess = { docs -> updateState { it.copy(documents = docs) } },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun uploadPhoto(file: File, mimeType: String) {
        if (uiState.value.isUploadingPhoto) return
        updateState { it.copy(isUploadingPhoto = true, errorCode = null, errorMessage = null, infoMessage = null) }
        launch {
            val result = uploadBuildingPhoto(buildingId, file, mimeType)
            file.delete()
            result.fold(
                onSuccess = {
                    refreshBuildingAndPublication("Foto del edificio actualizada.")
                    updateState { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { throwable ->
                    updateState { it.copy(isUploadingPhoto = false) }
                    handleFailure(throwable)
                },
            )
        }
    }

    private fun removePhoto() {
        if (uiState.value.isUploadingPhoto) return
        updateState { it.copy(isUploadingPhoto = true, errorCode = null, errorMessage = null, infoMessage = null) }
        launch {
            deleteBuildingPhoto(buildingId).fold(
                onSuccess = {
                    refreshBuildingAndPublication("Foto del edificio eliminada.")
                    updateState { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { throwable ->
                    updateState { it.copy(isUploadingPhoto = false) }
                    handleFailure(throwable)
                },
            )
        }
    }

    private fun uploadDocument(ine: Boolean, file: File, mimeType: String) {
        if (uiState.value.isUploadingDocument) return
        updateState { it.copy(isUploadingDocument = true, errorCode = null, errorMessage = null, infoMessage = null) }
        launch {
            val result = if (ine) {
                uploadIneDocument(buildingId, file, mimeType)
            } else {
                uploadPropertyProofDocument(buildingId, file, mimeType)
            }
            file.delete()
            result.fold(
                onSuccess = {
                    loadDocuments()
                    loadPublication()
                    updateState {
                        it.copy(
                            isUploadingDocument = false,
                            infoMessage = if (ine) "INE cargada." else "Comprobante de propiedad cargado.",
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState { it.copy(isUploadingDocument = false) }
                    handleFailure(throwable)
                },
            )
        }
    }

    private fun refreshBuildingAndPublication(info: String) {
        launch {
            getBuilding(buildingId).onSuccess { building -> updateState { it.copy(building = building, infoMessage = info) } }
            loadPublication()
        }
    }

    /** Publicar: 202 → in_review. Sin retry automático y sin asumir publicación inmediata. */
    private fun publish() {
        if (uiState.value.isPublishing) return
        updateState { it.copy(isPublishing = true, errorCode = null, errorMessage = null) }
        launch {
            publishBuilding(buildingId).fold(
                onSuccess = { building ->
                    updateState {
                        it.copy(
                            isPublishing = false,
                            building = building,
                            infoMessage = if (building.status == BuildingStatus.IN_REVIEW) {
                                "Tu publicación está en revisión."
                            } else {
                                null
                            },
                        )
                    }
                    sendEffect(BuildingDetailEffect.Published(building))
                    refreshGuide()
                    loadPublication()
                },
                onFailure = { throwable -> handleFailure(throwable, saving = true) },
            )
        }
    }

    private fun delete() {
        val building = uiState.value.building ?: return
        if (uiState.value.isDeleting) return
        updateState { it.copy(isDeleting = true, errorCode = null, errorMessage = null) }
        launch {
            deleteBuilding(building.id, building.version).fold(
                onSuccess = { _ ->
                    updateState { it.copy(isDeleting = false, deleted = true) }
                    sendEffect(BuildingDetailEffect.Deleted)
                },
                onFailure = { throwable ->
                    handleFailure(throwable, saving = false)
                    updateState { it.copy(isDeleting = false) }
                },
            )
        }
    }

    /** Pause/resume; el Backend devuelve el cuarto con su nueva version. */
    private fun changeRoomState(roomId: String, pause: Boolean) {
        if (uiState.value.busyRoomId != null) return
        updateState { it.copy(busyRoomId = roomId, errorCode = null, errorMessage = null) }
        launch {
            val result = if (pause) pauseRoom(roomId) else resumeRoom(roomId)
            result.fold(
                onSuccess = { updated ->
                    updateState { current ->
                        current.copy(
                            busyRoomId = null,
                            rooms = current.rooms.map { if (it.id == updated.id) updated else it },
                            infoMessage = if (pause) "Cuarto pausado." else "Cuarto reanudado.",
                        )
                    }
                    // room_ready depende del estado real de los cuartos: se recarga sin reiniciar.
                    loadPublication()
                },
                onFailure = { throwable ->
                    handleFailure(throwable, saving = false)
                    updateState { it.copy(busyRoomId = null) }
                },
            )
        }
    }

    private fun handleFailure(throwable: Throwable, saving: Boolean = false) {
        val exception = throwable as? LandlordException
        updateState {
            it.copy(
                isPublishing = if (saving) false else it.isPublishing,
                versionConflict = exception?.code == LandlordErrorCode.VERSION_CONFLICT,
                errorCode = exception?.code,
                errorMessage = LandlordErrorMessages.map(exception),
            )
        }
    }
}
