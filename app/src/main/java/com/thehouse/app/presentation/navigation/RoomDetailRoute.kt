package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

/** D18: destino tipado del detalle de cuarto real. La identidad es `roomId`. */
data class RoomDetailRoute(val roomId: String) : AppRoute {
    // El destino registrado es `cuarto/{roomId}`: el id sustituye al marcador.
    // Concatenar PATTERN + "/id" producía `cuarto/{roomId}/id`, que no coincide
    // con ningún destino y crashaba al pulsar una recomendación (2026-11-27).
    override val route: String = "cuarto/" +
        URLEncoder.encode(roomId, "UTF-8").replace("+", "%20")

    init {
        require(roomId.isNotBlank()) { "roomId no puede estar vacío" }
    }

    companion object {
        const val ID_ARGUMENT = "roomId"
        const val PATTERN = "cuarto/{$ID_ARGUMENT}"
        fun isValid(id: String?) = !id.isNullOrBlank()
    }
}
