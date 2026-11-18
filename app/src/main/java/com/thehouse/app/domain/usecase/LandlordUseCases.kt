package com.thehouse.app.domain.usecase

import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.LandlordProfile
import com.thehouse.app.domain.model.PagedBuildings
import com.thehouse.app.domain.model.PagedRooms
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomBase
import com.thehouse.app.domain.model.RoomPatch
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.repository.LandlordRepository

/** Casos de uso de D12. Delegan en el repositorio; sin lógica de negocio propia. */

class ActivateLandlordUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(
        fullName: String,
        phone: String,
        rentalMode: RentalMode,
        rfc: String?,
        acceptedTermsVersion: String,
    ): Result<LandlordProfile> = repository.createProfile(
        fullName = fullName,
        phone = phone,
        rentalMode = rentalMode,
        rfc = rfc,
        acceptedTermsVersion = acceptedTermsVersion,
    )
}

class GetLandlordProfileUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(): Result<LandlordProfile?> = repository.getProfile()
}

class GetLandlordBuildingsUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): Result<PagedBuildings> =
        repository.getBuildings(page, pageSize)
}

class GetLandlordBuildingUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String): Result<Building> = repository.getBuilding(id)
}

class CreateBuildingUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(
        type: BuildingType,
        name: String,
        address: BuildingAddress,
        location: BuildingLocation,
        nearestUniversityId: String,
        allowedTenants: AllowedTenants,
        services: List<Service>,
        rules: String?,
    ): Result<Building> = repository.createBuilding(
        type = type,
        name = name,
        address = address,
        location = location,
        nearestUniversityId = nearestUniversityId,
        allowedTenants = allowedTenants,
        services = services,
        rules = rules,
    )
}

class UpdateBuildingUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String, version: Int, patch: BuildingPatch): Result<Building> =
        repository.updateBuilding(id, version, patch)
}

class DeleteBuildingUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String, version: Int): Result<Unit> =
        repository.deleteBuilding(id, version)
}

class GenerateRoomsUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(
        buildingId: String,
        floors: Int,
        roomsPerFloor: List<Int>,
        base: RoomBase,
    ): Result<PagedRooms> = repository.generateRooms(buildingId, floors, roomsPerFloor, base)
}

class GetLandlordRoomsUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String, page: Int = 1, pageSize: Int = 50): Result<PagedRooms> =
        repository.getRooms(buildingId, page, pageSize)
}

class UpdateRoomUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String, version: Int, patch: RoomPatch): Result<Room> =
        repository.updateRoom(id, version, patch)
}

class DeleteRoomUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String, version: Int): Result<Unit> =
        repository.deleteRoom(id, version)
}

class PauseRoomUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String): Result<Room> = repository.pauseRoom(id)
}

class ResumeRoomUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String): Result<Room> = repository.resumeRoom(id)
}

class GetPublicationStatusUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String): Result<PublicationStatus> =
        repository.getPublication(buildingId)
}

class PublishBuildingUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(id: String): Result<Building> = repository.publishBuilding(id)
}

class UploadBuildingPhotoUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String, file: java.io.File, mimeType: String): Result<Unit> =
        repository.uploadBuildingPhoto(buildingId, file, mimeType)
}

class DeleteBuildingPhotoUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String): Result<Unit> =
        repository.deleteBuildingPhoto(buildingId)
}

class UploadRoomPhotoUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(roomId: String, file: java.io.File, mimeType: String): Result<Unit> =
        repository.uploadRoomPhoto(roomId, file, mimeType)
}

class DeleteRoomPhotoUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(roomId: String, photoId: String): Result<Unit> =
        repository.deleteRoomPhoto(roomId, photoId)
}

class UploadIneDocumentUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String, file: java.io.File, mimeType: String): Result<Unit> =
        repository.uploadIneDocument(buildingId, file, mimeType)
}

class UploadPropertyProofDocumentUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String, file: java.io.File, mimeType: String): Result<Unit> =
        repository.uploadPropertyProofDocument(buildingId, file, mimeType)
}

class GetBuildingDocumentsUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(buildingId: String): Result<com.thehouse.app.domain.model.BuildingDocuments> =
        repository.getBuildingDocuments(buildingId)
}

class GetLandlordStatsRoomsUseCase(private val repository: LandlordRepository) {
    suspend operator fun invoke(
        from: String,
        to: String,
        buildingId: String? = null,
        page: Int = 1,
        pageSize: Int = 50,
    ): Result<com.thehouse.app.domain.model.PagedLandlordRoomStats> =
        repository.getLandlordStatsRooms(from, to, buildingId, page, pageSize)
}
