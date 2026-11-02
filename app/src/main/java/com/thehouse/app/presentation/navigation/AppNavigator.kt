package com.thehouse.app.presentation.navigation

import androidx.navigation.NavHostController

/**
 * Wrapper mínimo de Navigation Compose.
 *
 * No contiene destinos ni lógica de negocio.
 */
class AppNavigator(
    private val navController: NavHostController,
) {
    fun navigate(route: AppRoute) {
        navController.navigate(route.route)
    }

    fun navigateUp(): Boolean = navController.navigateUp()
}
