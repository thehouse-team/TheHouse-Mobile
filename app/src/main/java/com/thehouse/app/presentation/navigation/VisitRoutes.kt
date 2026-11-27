package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

data class ScheduleVisitRoute(val propertyId: String) : AppRoute {
    override val route: String = "visita/agendar/" +
        URLEncoder.encode(propertyId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "propertyId"
        const val PATTERN = "visita/agendar/{$ID_ARGUMENT}"
    }
}

/** D20: agendar visita con roomId real (XOR: nunca propertyId). */
data class ScheduleRoomVisitRoute(val roomId: String) : AppRoute {
    override val route: String = "visita/agendar/cuarto/" +
        URLEncoder.encode(roomId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "roomId"
        const val PATTERN = "visita/agendar/cuarto/{$ID_ARGUMENT}"
    }
}

data class VisitDetailRoute(val visitId: String) : AppRoute {
    override val route: String = "visita/" +
        URLEncoder.encode(visitId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "visitId"
        const val PATTERN = "visita/{$ID_ARGUMENT}"
    }
}