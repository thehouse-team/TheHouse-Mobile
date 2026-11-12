package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingStatus
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomStatus
import com.thehouse.app.domain.usecase.DeleteBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordBuildingUseCase
import com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase
import com.thehouse.app.domain.usecase.GetPublicationStatusUseCase
import com.thehouse.app.domain.usecase.PauseRoomUseCase
import com.thehouse.app.domain.usecase.PublishBuildingUseCase
import com.thehouse.app.domain.usecase.ResumeRoomUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent

/**
 * Detalle de edificio + cuartos + publicación.
 * La `version` del edificio y de cada cuarto proviene SIEMPRE del Backend.
 */
data class BuildingDetailUiState(
    val isLoading: Boolean = false,
    val building: Building? = null,
    val rooms: List<Room> = emptyList(),
    val publication: PublicationStatus? = null,
    val isPublishing: Boolean = false,
    val isDeleting: Boolean = false,
    val busyRoomId: String? = null,
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

    private fun loadPublication() {
        launch {
            getPublication(buildingId).fold(
                onSuccess = { publication -> updateState { it.copy(publication = publication) } },
                onFailure = { throwable -> handleFailure(throwable) },
            )
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
                        )
                    }
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
