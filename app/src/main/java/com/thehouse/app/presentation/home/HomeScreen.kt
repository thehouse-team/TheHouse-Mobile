package com.thehouse.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory

/**
 * Inicio provisional (U2): saludo del usuario real (nombre de login/register)
 * y botón `Cerrar sesión` (POST /api/v1/auth/logout + limpieza local).
 * Referencia visual: `07-inicio-estudiante.html` (solo encabezado; fidelidad
 * pendiente humana). La exploración de propiedades pertenece a specs futuras.
 */
@Composable
fun HomeScreen(
    displayName: String,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
) {
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.LoggedOut -> onLoggedOut()
            }
        }
    }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (displayName.isNotBlank()) "¡Hola, $displayName!" else "¡Hola!",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Bienvenido a TheHouse. Próximamente: catálogo y búsqueda de propiedades.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.isLoggingOut) {
                CircularProgressIndicator(modifier = Modifier.height(44.dp))
            }
            Button(
                onClick = { viewModel.onEvent(HomeEvent.LogoutClicked) },
                enabled = !state.isLoggingOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}
