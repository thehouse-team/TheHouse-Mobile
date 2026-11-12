package com.thehouse.app.data.landlord.repository

import com.thehouse.app.data.landlord.mapper.toDomain
import com.thehouse.app.data.remote.api.LandlordApi
import com.thehouse.app.data.remote.dto.landlord.BuildingAddressDto
import com.thehouse.app.data.remote.dto.landlord.CreateBuildingDto
import com.thehouse.app.data.remote.dto.landlord.CreateLandlordProfileDto
import com.thehouse.app.data.remote.dto.landlord.GenerateRoomsDto
import com.thehouse.app.data.remote.dto.landlord.ProblemDetailsDto
import com.thehouse.app.data.remote.dto.landlord.RoomBaseDto
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.AllowedTenants
import com.thehouse.app.domain.model.Building
import com.thehouse.app.domain.model.BuildingAddress
import com.thehouse.app.domain.model.BuildingLocation
import com.thehouse.app.domain.model.BuildingPatch
import com.thehouse.app.domain.model.BuildingType
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.LandlordProfile
import com.thehouse.app.domain.model.PagedBuildings
import com.thehouse.app.domain.model.PagedRooms
import com.thehouse.app.domain.model.PublicationStatus
import com.thehouse.app.domain.model.RentalMode
import com.thehouse.app.domain.model.Room
import com.thehouse.app.domain.model.RoomBase
import com.thehouse.app.domain.model.RoomPatch
import com.thehouse.app.domain.repository.LandlordRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import retrofit2.Response

class LandlordRepositoryImpl(
    private val api: LandlordApi,
    private val json: Json,
) : LandlordRepository {

    override suspend fun createProfile(
        fullName: String,
        phone: String,
        rentalMode: RentalMode,
        rfc: String?,
        acceptedTermsVersion: String,
    ): Result<LandlordProfile> = safeCall {
        val response = api.createProfile(
            CreateLandlordProfileDto(
                fullName = fullName,
                phone = phone,
                rentalMode = rentalMode.api,
                rfc = rfc?.takeIf { it.isNotBlank() },
                acceptedTermsVersion = acceptedTermsVersion,
            ),
        )
        finish(response) { it.toDomain() }
    }

    override suspend fun getProfile(): Result<LandlordProfile?> = safeCall {
        val response = api.getProfile()
        when {
            response.isSuccessful && response.body() != null ->
                Result.success(response.body()!!.toDomain())
            response.code() == 404 -> Result.success(null)
            else -> Result.failure(errorFor(response))
        }
    }

    override suspend fun getBuildings(page: Int, pageSize: Int): Result<PagedBuildings> = safeCall {
        val response = api.getBuildings(page = page, pageSize = pageSize)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(
                PagedBuildings(
                    items = body.items.map { it.toDomain() },
                    page = body.page,
                    pageSize = body.pageSize,
                    totalItems = body.totalItems,
                    totalPages = body.totalPages,
                    summary = body.summary?.toDomain(),
                ),
            )
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun getBuilding(id: String): Result<Building> = safeCall {
        finish(api.getBuilding(id)) { it.toDomain() }
    }

    override suspend fun createBuilding(
        type: com.thehouse.app.domain.model.BuildingType,
        name: String,
        address: BuildingAddress,
        location: BuildingLocation,
        nearestUniversityId: String,
        allowedTenants: AllowedTenants,
        services: List<com.thehouse.app.domain.model.Service>,
        rules: String?,
    ): Result<Building> = safeCall {
        finish(
            api.createBuilding(
                CreateBuildingDto(
                    type = type.api,
                    name = name,
                    address = BuildingAddressDto(
                        street = address.street,
                        number = address.number,
                        neighborhood = address.neighborhood,
                        city = address.city,
                        state = address.state,
                        postalCode = address.postalCode,
                    ),
                    location = com.thehouse.app.data.remote.dto.landlord.BuildingLocationDto(
                        lat = location.lat,
                        lng = location.lng,
                    ),
                    nearestUniversityId = nearestUniversityId,
                    allowedTenants = allowedTenants.api,
                    services = services.map { it.api },
                    rules = rules?.takeIf { it.isNotBlank() },
                ),
            ),
        ) { it.toDomain() }
    }

    /** PATCH parcial: solo los campos a cambiar + version (contract §6). */
    override suspend fun updateBuilding(
        id: String,
        version: Int,
        patch: BuildingPatch,
    ): Result<Building> = safeCall {
        val body = buildJsonObject {
            put("version", version)
            patch.type?.let { put("type", it.api) }
            patch.name?.let { put("name", it) }
            patch.address?.let { addr ->
                putJsonObject("address") {
                    put("street", addr.street)
                    put("number", addr.number)
                    put("neighborhood", addr.neighborhood)
                    put("city", addr.city)
                    put("state", addr.state)
                    put("postalCode", addr.postalCode)
                }
            }
            patch.location?.let { loc ->
                putJsonObject("location") {
                    put("lat", loc.lat)
                    put("lng", loc.lng)
                }
            }
            patch.nearestUniversityId?.let { put("nearestUniversityId", it) }
            patch.allowedTenants?.let { put("allowedTenants", it.api) }
            patch.services?.let { svcs ->
                put("services", buildJsonArray { svcs.forEach { add(JsonPrimitive(it.api)) } })
            }
            patch.rules?.let { put("rules", it) }
        }
        finish(api.updateBuilding(id, body)) { it.toDomain() }
    }

    override suspend fun deleteBuilding(id: String, version: Int): Result<Unit> = safeCall {
        val response = api.deleteBuilding(id, version)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun generateRooms(
        buildingId: String,
        floors: Int,
        roomsPerFloor: List<Int>,
        base: RoomBase,
    ): Result<PagedRooms> = safeCall {
        val response = api.generateRooms(
            id = buildingId,
            body = GenerateRoomsDto(
                floors = floors,
                roomsPerFloor = roomsPerFloor,
                numbering = "floor_prefix",
                base = base.toDto(),
            ),
        )
        finish(response) { page ->
            PagedRooms(
                items = page.items.map { it.toDomain() },
                page = page.page,
                pageSize = page.pageSize,
                totalItems = page.totalItems,
                totalPages = page.totalPages,
            )
        }
    }

    override suspend fun getRooms(buildingId: String, page: Int, pageSize: Int): Result<PagedRooms> =
        safeCall {
            val response = api.getRooms(buildingId, page = page, pageSize = pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    PagedRooms(
                        items = body.items.map { it.toDomain() },
                        page = body.page,
                        pageSize = body.pageSize,
                        totalItems = body.totalItems,
                        totalPages = body.totalPages,
                    ),
                )
            } else {
                Result.failure(errorFor(response))
            }
        }

    override suspend fun updateRoom(id: String, version: Int, patch: RoomPatch): Result<Room> =
        safeCall {
            val body = buildJsonObject {
                put("version", version)
                patch.number?.let { put("number", it) }
                patch.floor?.let { put("floor", it) }
                patch.price?.let { put("price", it) }
                patch.sizeM2?.let { put("sizeM2", it) }
                patch.type?.let { put("type", it.api) }
                patch.bathroom?.let { put("bathroom", it.api) }
                patch.furniture?.let { f ->
                    put("furniture", buildJsonArray { f.forEach { add(JsonPrimitive(it.api)) } })
                }
                patch.availableFrom?.let { put("availableFrom", it) }
                patch.minContract?.let { put("minContract", it.api) }
            }
            finish(api.updateRoom(id, body)) { it.toDomain() }
        }

    override suspend fun deleteRoom(id: String, version: Int): Result<Unit> = safeCall {
        val response = api.deleteRoom(id, version)
        if (response.isSuccessful) Result.success(Unit) else Result.failure(errorFor(response))
    }

    override suspend fun duplicateRoom(id: String): Result<Room> = safeCall {
        finish(api.duplicateRoom(id)) { it.toDomain() }
    }

    override suspend fun copyFromRoom(id: String, version: Int, sourceRoomId: String): Result<Room> =
        safeCall {
            finish(
                api.copyFromRoom(
                    id,
                    com.thehouse.app.data.remote.dto.landlord.CopyFromRoomDto(
                        sourceRoomId = sourceRoomId,
                        version = version,
                    ),
                ),
            ) { it.toDomain() }
        }

    override suspend fun pauseRoom(id: String): Result<Room> = safeCall {
        finish(api.pauseRoom(id)) { it.toDomain() }
    }

    override suspend fun resumeRoom(id: String): Result<Room> = safeCall {
        finish(api.resumeRoom(id)) { it.toDomain() }
    }

    override suspend fun getPublication(buildingId: String): Result<PublicationStatus> = safeCall {
        finish(api.getPublication(buildingId)) { it.toDomain() }
    }

    override suspend fun publishBuilding(id: String): Result<Building> = safeCall {
        finish(api.publishBuilding(id)) { it.toDomain() }
    }

    private fun RoomBase.toDto(): RoomBaseDto = RoomBaseDto(
        price = price,
        sizeM2 = sizeM2,
        type = type?.api,
        bathroom = bathroom?.api,
        furniture = furniture?.map { it.api },
        availableFrom = availableFrom,
        minContract = minContract?.api,
    )

    private fun <R, T> finish(response: Response<R>, map: (R) -> T): Result<T> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            Result.success(map(body))
        } else {
            Result.failure(errorFor(response))
        }
    }

    private fun errorFor(response: Response<*>): LandlordException {
        val code = parseCode(response)
        val domainError = when (response.code()) {
            400 -> DomainError.Unknown
            401 -> DomainError.Unauthorized
            403 -> DomainError.Unauthorized
            404 -> DomainError.NotFound
            409 -> DomainError.Conflict
            408 -> DomainError.Timeout
            429 -> DomainError.RateLimited
            in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val businessCode = when (response.code()) {
            400 -> LandlordErrorCode.VALIDATION
            403 -> LandlordErrorCode.LANDLORD_REQUIRED
            else -> code
        }
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        return LandlordException(domainError, businessCode, retryAfter)
    }

    private fun parseCode(response: Response<*>): LandlordErrorCode {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
            ?: return LandlordErrorCode.UNKNOWN
        val problem = runCatching { json.decodeFromString<ProblemDetailsDto>(raw) }.getOrNull()
        return LandlordErrorCode.fromApi(problem?.code)
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (timeout: SocketTimeoutException) {
        Result.failure(LandlordException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(LandlordException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(LandlordException(DomainError.Unknown))
    }
}
