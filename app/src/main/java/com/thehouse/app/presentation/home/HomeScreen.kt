package com.thehouse.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.demo.PrototypeProperties

/**
 * Home visual del prototipo (D4, NV-05, 07-inicio-estudiante).
 *
 * - Universidad seleccionada/buscador (fixture "UTTT"), renta destacada
 *   con precio, distancia, Match y verificado, acceso a Explorar.
 * - Guest y Auth comparten el mismo shell; la diferencia es el guard
 *   de acciones protegidas (favorito/contactar/publicar → Login).
 * - Sin backend: datos solo como fixture de prototipo etiquetado.
 *
 * La barra inferior vive en el shell (MainActivity D4) para no duplicar
 * destinos; esta pantalla solo declara el contenido de Inicio.
 */
@Composable
fun HomeScreen(
    displayName: String,
    onLoggedOut: () -> Unit,
    onRequireLogin: () -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
) {
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.LoggedOut -> onLoggedOut()
                HomeEffect.RequireLogin -> onRequireLogin()
            }
        }
    }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Cabecera + universidad/buscador (prototipo: "Universidad UTTT").
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.isGuest) {
                        "Hola, invitado 👋"
                    } else if (displayName.isNotBlank()) {
                        "Hola, $displayName 👋"
                    } else {
                        "Hola 👋"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "Saludo de inicio"
                    },
                )
                if (state.isGuest) {
                    Text(
                        text = "Explorando como invitado. Inicia sesión para guardar favoritos y contactar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "Universidad: UTTT",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = onNavigateToExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Buscar tu universidad")
                }
            }

            // Smart Match activo (fixture).
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Smart Match Activo",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "95% de compatibilidad calculada",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = "Recomendados para ti",
                style = MaterialTheme.typography.titleMedium,
            )
            PrototypeProperties.featured.forEach { property ->
                PropertyCard(
                    property = property,
                    onFavoriteClick = { viewModel.onEvent(HomeEvent.FavoriteClicked) },
                    onContactClick = { viewModel.onEvent(HomeEvent.ContactClicked) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onNavigateToExplore,
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                ) {
                    Text("Ver todas las rentas")
                }
                OutlinedButton(
                    onClick = { viewModel.onEvent(HomeEvent.PublishClicked) },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                ) {
                    Text("Publicar mi renta")
                }
            }

            if (!state.isGuest) {
                if (state.isLoggingOut) {
                    CircularProgressIndicator(modifier = Modifier.height(44.dp))
                }
                Button(
                    onClick = { viewModel.onEvent(HomeEvent.LogoutClicked) },
                    enabled = !state.isLoggingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Cerrar sesión")
                }
            } else {
                OutlinedButton(
                    onClick = onRequireLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Iniciar sesión")
                }
            }
        }
    }
}
