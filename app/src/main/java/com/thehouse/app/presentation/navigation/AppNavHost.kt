package com.thehouse.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Base reusable de Navigation Compose.
 *
 * El grafo real se declarará cuando exista un destino autorizado por una spec funcional.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
        builder = builder,
    )
}
