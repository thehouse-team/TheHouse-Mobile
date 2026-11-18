package com.thehouse.app.domain.repository

import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingDocuments
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Service
import com.thehouse.app.domain.model.LandlordProfile
import com.thehouse.app.domain.model.PagedBuildings
import com.thehouse.app.domain.model.PagedRooms
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomBase
import com.thehouse.app.domain.model.RoomPatch
import java.io.File

/** Contrato de dominio del arrendador (D12). La Data lo implementa. */
interface LandlordRepository {

    suspend fun createProfile(
        fullName: String,
        phone: String,
        rentalMode: RentalMode,
        rfc: String?,
        acceptedTermsVersion: String,
    ): Result<LandlordProfile>

    suspend fun getProfile(): Result<LandlordProfile?>

    suspend fun createBuilding(
        type: BuildingType,
        name: String,
        address: BuildingAddress,
        location: BuildingLocation,
        nearestUniversityId: String,
        allowedTenants: AllowedTenants,
        services: List<Service>,
        rules: String?,
    ): Result<Building>

    suspend fun getBuildings(page: Int = 1, pageSize: Int = 20): Result<PagedBuildings>

    suspend fun getBuilding(id: String): Result<Building>

    /** PATCH parcial; exige [version] del recurso actual. Devuelve el edificio actualizado. */
    suspend fun updateBuilding(id: String, version: Int, patch: BuildingPatch): Result<Building>

    suspend fun deleteBuilding(id: String, version: Int): Result<Unit>

    suspend fun generateRooms(
        buildingId: String,
        floors: Int,
        roomsPerFloor: List<Int>,
        base: RoomBase,
    ): Result<PagedRooms>

    suspend fun getRooms(buildingId: String, page: Int = 1, pageSize: Int = 50): Result<PagedRooms>

    suspend fun updateRoom(id: String, version: Int, patch: RoomPatch): Result<Room>

    suspend fun deleteRoom(id: String, version: Int): Result<Unit>

    suspend fun duplicateRoom(id: String): Result<Room>

    suspend fun copyFromRoom(id: String, version: Int, sourceRoomId: String): Result<Room>

    suspend fun pauseRoom(id: String): Result<Room>

    suspend fun resumeRoom(id: String): Result<Room>

    suspend fun getPublication(buildingId: String): Result<PublicationStatus>

    /** 202 = enviado a revisión; el Backend indica el estado real. */
    suspend fun publishBuilding(id: String): Result<Building>

    suspend fun uploadBuildingPhoto(buildingId: String, file: File, mimeType: String): Result<Unit>
    suspend fun deleteBuildingPhoto(buildingId: String): Result<Unit>
    suspend fun uploadRoomPhoto(roomId: String, file: File, mimeType: String): Result<Unit>
    suspend fun deleteRoomPhoto(roomId: String, photoId: String): Result<Unit>
    suspend fun uploadIneDocument(buildingId: String, file: File, mimeType: String): Result<Unit>
    suspend fun uploadPropertyProofDocument(buildingId: String, file: File, mimeType: String): Result<Unit>
    suspend fun getBuildingDocuments(buildingId: String): Result<BuildingDocuments>
}
