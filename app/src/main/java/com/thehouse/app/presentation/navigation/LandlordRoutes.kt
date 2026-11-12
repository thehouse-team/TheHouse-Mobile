package com.thehouse.app.presentation.navigation

/** Rutas D12 (arrendador: edificios y cuartos). */
data class BuildingFormRoute(val buildingId: String?) : AppRoute {
    override val route: String = "arrendador/edificio/" +
        (buildingId?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: "nuevo")

    companion object {
        const val ID_ARGUMENT = "buildingId"
        const val PATTERN = "arrendador/edificio/{$ID_ARGUMENT}"
    }
}

data class BuildingDetailRoute(val buildingId: String) : AppRoute {
    override val route: String = "arrendador/alojamiento/" +
        java.net.URLEncoder.encode(buildingId, "UTF-8")

    companion object {
        const val ID_ARGUMENT = "buildingId"
        const val PATTERN = "arrendador/alojamiento/{$ID_ARGUMENT}"
    }
}

data class RoomFormRoute(val buildingId: String, val roomId: String?) : AppRoute {
    override val route: String = "arrendador/cuarto/" +
        java.net.URLEncoder.encode(buildingId, "UTF-8") + "/" +
        (roomId?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: "nuevo")

    companion object {
        const val BUILDING_ARGUMENT = "buildingId"
        const val ROOM_ARGUMENT = "roomId"
        const val PATTERN = "arrendador/cuarto/{$BUILDING_ARGUMENT}/{$ROOM_ARGUMENT}"
    }
}
