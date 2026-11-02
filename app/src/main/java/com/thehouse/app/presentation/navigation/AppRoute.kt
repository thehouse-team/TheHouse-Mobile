package com.thehouse.app.presentation.navigation

/**
 * Contrato de ruta tipada.
 *
 * No define destinos de negocio por sí mismo. Cada destino funcional futuro
 * deberá implementar este contrato en su propia spec.
 */
interface AppRoute {
    val route: String
}
