package com.thehouse.app.presentation.navigation

import java.net.URLEncoder

/** Solo viaja el id; el detalle siempre obtiene sus datos del repositorio. */
data class PropertyDetailRoute(val propertyId: String) : AppRoute {
    override val route: String = "propiedad/" +
        URLEncoder.encode(propertyId, "UTF-8").replace("+", "%20")

    companion object {
        const val ID_ARGUMENT = "propertyId"
        const val PATTERN = "propiedad/{$ID_ARGUMENT}"
    }
}
