package com.thehouse.app.domain.model

/** Modelos de dominio de D12. Enums EXACTOS del contrato Backend NV-12. */

enum class RentalMode(val api: String) {
    INDIVIDUAL("individual"),
    AGENCY("agency"),
}

data class LandlordProfile(
    val fullName: String,
    val phone: String,
    val rentalMode: RentalMode,
    val rfc: String?,
    val acceptedTermsVersion: String,
    val createdAt: String?,
)

enum class BuildingType(val api: String) {
    ROOMS_BUILDING("rooms_building"),
    HOUSE_ROOMS("house_rooms"),
    FULL_APARTMENT("full_apartment");

    companion object {
        fun fromApi(value: String): BuildingType =
            entries.firstOrNull { it.api == value } ?: ROOMS_BUILDING
    }
}

enum class AllowedTenants(val api: String) {
    WOMEN("women"),
    MEN("men"),
    ANY("any");

    companion object {
        fun fromApi(value: String): AllowedTenants =
            entries.firstOrNull { it.api == value } ?: ANY
    }
}

enum class BuildingStatus(val api: String) {
    DRAFT("draft"),
    IN_REVIEW("in_review"),
    VERIFIED("verified");

    companion object {
        fun fromApi(value: String): BuildingStatus =
            entries.firstOrNull { it.api == value } ?: DRAFT
    }
}

/** Catálogo cerrado de servicios (igual que NV-11 del Backend). */
enum class Service(val api: String) {
    WIFI("wifi"),
    WATER("water"),
    ELECTRICITY("electricity"),
    GAS("gas"),
    HOT_WATER("hot_water"),
    LAUNDRY("laundry"),
    KITCHEN("kitchen"),
    FURNISHED("furnished"),
    PARKING("parking"),
    SECURITY("security"),
    CLEANING("cleaning");

    companion object {
        fun fromApi(value: String): Service? = entries.firstOrNull { it.api == value }
    }
}

data class BuildingAddress(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
)

data class BuildingLocation(
    val lat: Double,
    val lng: Double,
)

data class NearestUniversity(
    val id: String,
    val code: String,
    val name: String,
)

data class Building(
    val id: String,
    val type: BuildingType,
    val name: String,
    val address: BuildingAddress,
    val location: BuildingLocation,
    val nearestUniversity: NearestUniversity,
    val distanceToUniversityKm: Double,
    val walkingMinutes: Int,
    val allowedTenants: AllowedTenants,
    val services: List<Service>,
    val rules: String?,
    val status: BuildingStatus,
    val roomsTotal: Int,
    val roomsAvailable: Int,
    val version: Int,
    val createdAt: String?,
    val updatedAt: String?,
)

/** Tablero RF-22: únicamente datos reales del Backend. */
data class DashboardSummary(
    val buildings: Int,
    val rooms: Int,
    val available: Int,
    val occupancyPercent: Double,
    val monthlyIncome: Double,
)

data class PagedBuildings(
    val items: List<Building>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
    val summary: DashboardSummary?,
)

enum class RoomType(val api: String) {
    SINGLE("single"),
    DOUBLE("double"),
    SHARED("shared");

    companion object {
        fun fromApi(value: String): RoomType =
            entries.firstOrNull { it.api == value } ?: SINGLE
    }
}

enum class BathroomType(val api: String) {
    PRIVATE("private"),
    SHARED("shared");

    companion object {
        fun fromApi(value: String): BathroomType =
            entries.firstOrNull { it.api == value } ?: SHARED
    }
}

enum class Furniture(val api: String) {
    BED("bed"),
    DESK("desk"),
    CHAIR("chair"),
    CLOSET("closet"),
    DRESSER("dresser"),
    TV("tv"),
    FRIDGE("fridge"),
    MICROWAVE("microwave");

    companion object {
        fun fromApi(value: String): Furniture? = entries.firstOrNull { it.api == value }
    }
}

enum class MinContract(val api: String) {
    FOUR_MONTHS("four_months"),
    SIX_MONTHS("six_months"),
    ANNUAL("annual");

    companion object {
        fun fromApi(value: String): MinContract =
            entries.firstOrNull { it.api == value } ?: FOUR_MONTHS
    }
}

enum class RoomStatus(val api: String) {
    SETUP("setup"),
    READY("ready"),
    IN_REVIEW("in_review"),
    AVAILABLE("available"),
    PAUSED("paused"),
    RENTED("rented");

    companion object {
        fun fromApi(value: String): RoomStatus =
            entries.firstOrNull { it.api == value } ?: SETUP
    }
}

data class Room(
    val id: String,
    val buildingId: String,
    val number: String,
    val floor: Int,
    val price: Int,
    val currency: String,
    val sizeM2: Int,
    val type: RoomType,
    val bathroom: BathroomType,
    val furniture: List<Furniture>,
    val availableFrom: String,
    val minContract: MinContract,
    val status: RoomStatus,
    val version: Int,
    val createdAt: String?,
    val updatedAt: String?,
)

data class PagedRooms(
    val items: List<Room>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
)

data class RoomBase(
    val price: Int? = null,
    val sizeM2: Int? = null,
    val type: RoomType? = null,
    val bathroom: BathroomType? = null,
    val furniture: List<Furniture>? = null,
    val availableFrom: String? = null,
    val minContract: MinContract? = null,
)

data class PublicationStep(
    val id: String,
    val done: Boolean,
) {
    companion object {
        const val STEP_BUILDING_DATA = "building_data"
        const val STEP_LOCATION = "location"
        const val STEP_SERVICES = "services"
        const val STEP_ROOM_READY = "room_ready"
        const val STEP_DOCUMENTS = "documents"
    }
}

data class PublicationStatus(
    val canPublish: Boolean,
    val steps: List<PublicationStep>,
)

/** Campos editables de edificio (PATCH parcial). Los calculados/status/version quedan fuera. */
data class BuildingPatch(
    val type: BuildingType? = null,
    val name: String? = null,
    val address: BuildingAddress? = null,
    val location: BuildingLocation? = null,
    val nearestUniversityId: String? = null,
    val allowedTenants: AllowedTenants? = null,
    val services: List<Service>? = null,
    val rules: String? = null,
)

/** Campos editables de cuarto (PATCH parcial). Los calculados/status/version quedan fuera. */
data class RoomPatch(
    val number: String? = null,
    val floor: Int? = null,
    val price: Int? = null,
    val sizeM2: Int? = null,
    val type: RoomType? = null,
    val bathroom: BathroomType? = null,
    val furniture: List<Furniture>? = null,
    val availableFrom: String? = null,
    val minContract: MinContract? = null,
)
