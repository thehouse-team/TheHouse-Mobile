package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

/**
 * Detalle de cuarto real de /rooms/{id} (NV-07 D10).
 * El endpoint requiere sesión; el guard vive en ExploreViewModel.
 */
data class RoomDetailRoute(val roomId: String) : AppRoute {
    override val route: String = "cuarto/" +
        URLEncoder.encode(roomId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "roomId"
        const val PATTERN = "cuarto/{$ID_ARGUMENT}"
    }
}
