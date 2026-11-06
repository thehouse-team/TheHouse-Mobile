package com.thehouse.app.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Favoritos D4 (sin backend NV-09).
 *
 * - Guest abre la tab pero ve el muro de acceso requerido con botón
 *   Iniciar sesión (no hay favoritos de invitado ni identidad fake).
 * - Autenticado ve estado vacío con nota de backend pendiente.
 */
@Composable
fun FavoritesScreen(
    isAuthenticated: Boolean,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Favoritos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título Favoritos" },
            )
            if (!isAuthenticated) {
                Text(
                    text = "Necesitas iniciar sesión para ver tus favoritos.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Iniciar sesión")
                }
            } else {
                Text(
                    text = "Sin favoritos aún. (Backend de favoritos pendiente de NV-09.)",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
