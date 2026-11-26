package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

/** D18: destino tipado del detalle de cuarto real. La identidad es `roomId`. */
data class RoomDetailRoute(val roomId: String) : AppRoute {
    override val route: String = "$PATTERN/" + URLEncoder.encode(roomId, "UTF-8").replace("+", "%20")

    init {
        require(roomId.isNotBlank()) { "roomId no puede estar vacío" }
    }

    companion object {
        const val ID_ARGUMENT = "roomId"
        const val PATTERN = "cuarto/{$ID_ARGUMENT}"
        fun isValid(id: String?) = !id.isNullOrBlank()
    }
}
