package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

/** D9: composer de solicitud de cuarto. */
data class CreateRequestRoute(val roomId: String) : AppRoute {
    override val route: String = "solicitud/crear/" +
        URLEncoder.encode(roomId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "roomId"
        const val PATTERN = "solicitud/crear/{$ID_ARGUMENT}"
    }
}

/** D9: detalle de solicitud (estudiante). */
data class RequestDetailRoute(val requestId: String) : AppRoute {
    override val route: String = "solicitud/" +
        URLEncoder.encode(requestId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "requestId"
        const val PATTERN = "solicitud/{$ID_ARGUMENT}"
    }
}

/** D9: detalle de solicitud (arrendador). */
data class LandlordRequestDetailRoute(val requestId: String) : AppRoute {
    override val route: String = "solicitud_arrendador/" +
        URLEncoder.encode(requestId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "requestId"
        const val PATTERN = "solicitud_arrendador/{$ID_ARGUMENT}"
    }
}

/** D9: conversación de chat (estado cerrado inicial vía query param). */
data class ConversationRoute(
    val conversationId: String,
    val closed: Boolean = false,
) : AppRoute {
    override val route: String = "conversacion/" +
        URLEncoder.encode(conversationId, "UTF-8").replace("+", "%20") +
        (if (closed) "?closed=true" else "")

    companion object {
        const val ID_ARGUMENT = "conversationId"
        const val PATTERN = "conversacion/{$ID_ARGUMENT}?closed={closed}"
    }
}
