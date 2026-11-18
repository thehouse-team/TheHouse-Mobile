package com.thehouse.app.presentation.landlord

import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.MediaItem
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomBase
import com.thehouse.app.domain.model.RoomPatch
import com.thehouse.app.domain.model.RoomType
import com.thehouse.app.domain.usecase.DeleteRoomPhotoUseCase
import com.thehouse.app.domain.usecase.DeleteRoomUseCase
import com.thehouse.app.domain.usecase.GenerateRoomsUseCase
import com.thehouse.app.domain.usecase.GetLandlordRoomsUseCase
import com.thehouse.app.domain.usecase.UpdateRoomUseCase
import com.thehouse.app.domain.usecase.UploadRoomPhotoUseCase
import com.thehouse.app.presentation.common.BaseViewModel
import com.thehouse.app.presentation.common.UiEvent
import java.io.File

/**
 * Formulario de cuartos:
 * - Crear: rooms/generate (contrato real; el Backend asigna números floor_prefix).
 * - Editar: PATCH parcial con version; DELETE con version en query.
 */
data class RoomFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val floors: String = "1",
    val roomsPerFloor: String = "1",
    val price: String = "",
    val sizeM2: String = "",
    val type: RoomType = RoomType.SINGLE,
    val bathroom: BathroomType = BathroomType.SHARED,
    val furniture: Set<Furniture> = emptySet(),
    val availableFrom: String = "",
    val minContract: MinContract = MinContract.SIX_MONTHS,
    val number: String = "",
    val floor: String = "",
    val version: Int = 0,
    val status: com.thehouse.app.domain.model.RoomStatus? = null,
    val photos: List<MediaItem> = emptyList(),
    val infoMessage: String? = null,
    val versionConflict: Boolean = false,
    val errorCode: LandlordErrorCode? = null,
    val errorMessage: String? = null,
    val deleted: Boolean = false,
) {
    val floorsError: String? = floors.toIntOrNull()
        ?.let { if (it in 1..30) null else "Pisos entre 1 y 30." } ?: "Pisos entre 1 y 30."
    val roomsPerFloorError: String? = roomsPerFloor.toIntOrNull()
        ?.let { if (it in 1..30) null else "1 a 30 cuartos por piso." } ?: "1 a 30 cuartos por piso."
    val priceError: String? = price.toIntOrNull()
        ?.let { if (it in 500..50000) null else "Entre 500 y 50 000 MXN." } ?: "Entre 500 y 50 000 MXN."
    val sizeError: String? = sizeM2.toIntOrNull()
        ?.let { if (it in 4..200) null else "Entre 4 y 200 m²." } ?: "Entre 4 y 200 m²."
    val availableFromError: String? = if (availableFrom.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
        null
    } else {
        "Fecha YYYY-MM-DD."
    }
    val isValid: Boolean = floorsError == null && roomsPerFloorError == null &&
        priceError == null && sizeError == null && availableFromError == null &&
        (isEdit.not() || (number.isNotBlank() && floor.toIntOrNull() != null))
}

sealed interface RoomFormEvent : UiEvent {
    data object Load : RoomFormEvent
    data object Retry : RoomFormEvent
    data class FloorsChanged(val value: String) : RoomFormEvent
    data class RoomsPerFloorChanged(val value: String) : RoomFormEvent
    data class PriceChanged(val value: String) : RoomFormEvent
    data class SizeChanged(val value: String) : RoomFormEvent
    data class TypeChanged(val value: RoomType) : RoomFormEvent
    data class BathroomChanged(val value: BathroomType) : RoomFormEvent
    data class FurnitureToggled(val value: Furniture) : RoomFormEvent
    data class AvailableFromChanged(val value: String) : RoomFormEvent
    data class MinContractChanged(val value: MinContract) : RoomFormEvent
    data class NumberChanged(val value: String) : RoomFormEvent
    data class FloorChanged(val value: String) : RoomFormEvent
    data object Submit : RoomFormEvent
    data object Delete : RoomFormEvent
    data class UploadPhoto(val file: File, val mimeType: String) : RoomFormEvent
    data class DeletePhoto(val photoId: String) : RoomFormEvent
}

sealed interface RoomFormEffect : com.thehouse.app.presentation.common.UiEffect {
    data object Saved : RoomFormEffect
    data object Deleted : RoomFormEffect
}

class RoomFormViewModel(
    private val buildingId: String,
    private val roomId: String?,
    private val generateRooms: GenerateRoomsUseCase,
    private val updateRoom: UpdateRoomUseCase,
    private val deleteRoom: DeleteRoomUseCase,
    private val getRooms: GetLandlordRoomsUseCase,
    private val uploadRoomPhoto: UploadRoomPhotoUseCase,
    private val deleteRoomPhoto: DeleteRoomPhotoUseCase,
) : BaseViewModel<RoomFormUiState, RoomFormEvent, RoomFormEffect>(RoomFormUiState()) {

    init {
        onEvent(RoomFormEvent.Load)
    }

    override fun onEvent(event: RoomFormEvent) {
        when (event) {
            RoomFormEvent.Load, RoomFormEvent.Retry -> load()
            is RoomFormEvent.FloorsChanged -> updateState { it.copy(floors = event.value) }
            is RoomFormEvent.RoomsPerFloorChanged -> updateState { it.copy(roomsPerFloor = event.value) }
            is RoomFormEvent.PriceChanged -> updateState { it.copy(price = event.value) }
            is RoomFormEvent.SizeChanged -> updateState { it.copy(sizeM2 = event.value) }
            is RoomFormEvent.TypeChanged -> updateState { it.copy(type = event.value) }
            is RoomFormEvent.BathroomChanged -> updateState { it.copy(bathroom = event.value) }
            is RoomFormEvent.FurnitureToggled -> updateState {
                it.copy(
                    furniture = if (event.value in it.furniture) {
                        it.furniture - event.value
                    } else {
                        it.furniture + event.value
                    },
                )
            }
            is RoomFormEvent.AvailableFromChanged -> updateState { it.copy(availableFrom = event.value) }
            is RoomFormEvent.MinContractChanged -> updateState { it.copy(minContract = event.value) }
            is RoomFormEvent.NumberChanged -> updateState { it.copy(number = event.value) }
            is RoomFormEvent.FloorChanged -> updateState { it.copy(floor = event.value) }
            RoomFormEvent.Submit -> submit()
            RoomFormEvent.Delete -> delete()
            is RoomFormEvent.UploadPhoto -> uploadPhoto(event.file, event.mimeType)
            is RoomFormEvent.DeletePhoto -> removePhoto(event.photoId)
        }
    }

    private fun load() {
        updateState {
            it.copy(
                isLoading = true,
                isEdit = roomId != null,
                versionConflict = false,
                errorCode = null,
                errorMessage = null,
            )
        }
        if (roomId == null) {
            updateState { it.copy(isLoading = false) }
            return
        }
        launch {
            // No existe GET de cuarto individual: se lee la lista real del edificio (contract §6.1).
            getRooms(buildingId).fold(
                onSuccess = { page ->
                    val room = page.items.firstOrNull { it.id == roomId }
                    if (room == null) {
                        updateState {
                            it.copy(
                                isLoading = false,
                                errorCode = LandlordErrorCode.UNKNOWN,
                                errorMessage = "No se encontró el cuarto.",
                            )
                        }
                    } else {
                        applyRoom(room)
                    }
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun applyRoom(room: Room) {
        updateState { current ->
            current.copy(
                isLoading = false,
                isEdit = true,
                version = room.version,
                status = room.status,
                number = room.number,
                floor = room.floor.toString(),
                price = room.price.toString(),
                sizeM2 = room.sizeM2.toString(),
                type = room.type,
                bathroom = room.bathroom,
                furniture = room.furniture.toSet(),
                availableFrom = room.availableFrom,
                minContract = room.minContract,
                photos = room.photos,
            )
        }
    }

    private fun uploadPhoto(file: File, mimeType: String) {
        val currentRoomId = roomId ?: return
        if (uiState.value.isUploadingPhoto || uiState.value.photos.size >= 4) {
            file.delete()
            if (uiState.value.photos.size >= 4) {
                updateState { it.copy(errorCode = LandlordErrorCode.PHOTOS_LIMIT, errorMessage = LandlordErrorMessages.map(LandlordException(com.thehouse.app.domain.error.DomainError.Conflict, LandlordErrorCode.PHOTOS_LIMIT))) }
            }
            return
        }
        updateState { it.copy(isUploadingPhoto = true, errorCode = null, errorMessage = null, infoMessage = null) }
        launch {
            val result = uploadRoomPhoto(currentRoomId, file, mimeType)
            file.delete()
            result.fold(
                onSuccess = {
                    refreshRoom("Foto del cuarto actualizada.")
                    updateState { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { throwable ->
                    updateState { it.copy(isUploadingPhoto = false) }
                    handleFailure(throwable)
                },
            )
        }
    }

    private fun removePhoto(photoId: String) {
        val currentRoomId = roomId ?: return
        if (uiState.value.isUploadingPhoto) return
        updateState { it.copy(isUploadingPhoto = true, errorCode = null, errorMessage = null, infoMessage = null) }
        launch {
            deleteRoomPhoto(currentRoomId, photoId).fold(
                onSuccess = {
                    refreshRoom("Foto eliminada.")
                    updateState { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { throwable ->
                    updateState { it.copy(isUploadingPhoto = false) }
                    handleFailure(throwable)
                },
            )
        }
    }

    private fun refreshRoom(info: String) {
        val currentRoomId = roomId ?: return
        launch {
            getRooms(buildingId).fold(
                onSuccess = { page ->
                    val room = page.items.firstOrNull { it.id == currentRoomId }
                    if (room != null) {
                        applyRoom(room)
                        updateState { it.copy(infoMessage = info) }
                    }
                },
                onFailure = { throwable -> handleFailure(throwable) },
            )
        }
    }

    private fun submit() {
        val current = uiState.value
        if (current.isSaving || !current.isValid || current.isLoading) return
        updateState { it.copy(isSaving = true, errorCode = null, errorMessage = null, versionConflict = false) }
        launch {
            val result = if (current.isEdit && roomId != null) {
                updateRoom(
                    id = roomId,
                    version = current.version,
                    patch = RoomPatch(
                        number = current.number.trim(),
                        floor = current.floor.toIntOrNull(),
                        price = current.price.toIntOrNull(),
                        sizeM2 = current.sizeM2.toIntOrNull(),
                        type = current.type,
                        bathroom = current.bathroom,
                        furniture = current.furniture.toList(),
                        availableFrom = current.availableFrom,
                        minContract = current.minContract,
                    ),
                )
            } else {
                generateRooms(
                    buildingId = buildingId,
                    floors = current.floors.toInt(),
                    roomsPerFloor = List(current.floors.toInt()) { current.roomsPerFloor.toInt() },
                    base = RoomBase(
                        price = current.price.toIntOrNull(),
                        sizeM2 = current.sizeM2.toIntOrNull(),
                        type = current.type,
                        bathroom = current.bathroom,
                        furniture = current.furniture.toList(),
                        availableFrom = current.availableFrom.takeIf { it.isNotBlank() },
                        minContract = current.minContract,
                    ),
                )
            }
            result.fold(
                onSuccess = { _ ->
                    updateState { it.copy(isSaving = false) }
                    sendEffect(RoomFormEffect.Saved)
                },
                onFailure = { throwable ->
                    handleFailure(throwable)
                    updateState { it.copy(isSaving = false) }
                },
            )
        }
    }

    private fun delete() {
        if (roomId == null) return
        val current = uiState.value
        if (current.isSaving) return
        updateState { it.copy(isSaving = true, errorCode = null, errorMessage = null) }
        launch {
            deleteRoom(roomId, current.version).fold(
                onSuccess = { _ ->
                    updateState { it.copy(isSaving = false, deleted = true) }
                    sendEffect(RoomFormEffect.Deleted)
                },
                onFailure = { throwable ->
                    handleFailure(throwable)
                    updateState { it.copy(isSaving = false) }
                },
            )
        }
    }

    private fun handleFailure(throwable: Throwable) {
        val exception = throwable as? LandlordException
        updateState {
            it.copy(
                isLoading = false,
                versionConflict = exception?.code == LandlordErrorCode.VERSION_CONFLICT,
                errorCode = exception?.code,
                errorMessage = LandlordErrorMessages.map(exception),
            )
        }
    }
}
