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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.components.PropertyCard

/**
 * Home visual (D4 diseño + D5 datos reales, NV-05/NV-06/NV-07).
 *
 * - Universidad seleccionada REAL (lista backend + persistencia local;
 *   sin UTTT hardcodeada productiva).
 * - Propiedad destacada REAL = primera disponible (regla UI local,
 *   no marcada por el backend).
 * - Solo campos del contrato (sin Match/verificación/distancia inventados).
 * - Estados loading/empty/error + retry para universidades y propiedades.
 * - Guest y Auth comparten shell; acciones protegidas → Login.
 *
 * La barra inferior vive en el shell (MainActivity D4).
 */
@Composable
fun HomeScreen(
    displayName: String,
    onLoggedOut: () -> Unit,
    onRequireLogin: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
    onOpenRequests: () -> Unit = {},
    onOpenConversations: () -> Unit = {},
    onOpenLandlordRequests: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var showUniversityPicker by remember { mutableStateOf(false) }

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
            }

            // Selector de universidad real (loading/empty/error/retry).
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Universidad",
                    style = MaterialTheme.typography.titleMedium,
                )
                when (val universities = state.universities) {
                    CatalogLoadState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    }
                    CatalogLoadState.Empty -> {
                        Text(
                            text = "Sin universidades disponibles.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is CatalogLoadState.Error -> {
                        Text(
                            text = universities.errorMessage() ?: "Algo salió mal.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = { viewModel.onEvent(HomeEvent.RetryUniversitiesClicked) },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("Reintentar")
                        }
                    }
                    is CatalogLoadState.Success -> {
                        val selectedName = universities.items.firstOrNull { it.id == state.selectedUniversityId }?.name
                        Text(
                            text = "Universidad: " + (selectedName ?: "Selecciona tu universidad"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(
                            onClick = { showUniversityPicker = !showUniversityPicker },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text(if (selectedName != null) "Cambiar universidad" else "Buscar tu universidad")
                        }
                        if (showUniversityPicker) {
                            universities.items.forEach { university ->
                                TextButton(
                                    onClick = {
                                        viewModel.onEvent(HomeEvent.UniversitySelected(university.id))
                                        showUniversityPicker = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(text = university.name + " · " + university.city)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Smart Match",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Ver compatibilidad en la pestaña Smart Match.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = "Recomendados para ti",
                style = MaterialTheme.typography.titleMedium,
            )
            when (val properties = state.properties) {
                CatalogLoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                }
                CatalogLoadState.Empty -> {
                    Text(
                        text = "Sin propiedades disponibles.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is CatalogLoadState.Error -> {
                    Text(
                        text = properties.errorMessage() ?: "Algo salió mal.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = { viewModel.onEvent(HomeEvent.RetryPropertiesClicked) },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Reintentar")
                    }
                }
                is CatalogLoadState.Success -> {
                    val featured = state.featuredProperty
                    if (featured != null) {
                        PropertyCard(
                            property = featured,
                            onFavoriteClick = { viewModel.onEvent(HomeEvent.FavoriteClicked) },
                            onContactClick = { viewModel.onEvent(HomeEvent.ContactClicked) },
                            onPropertyClick = { onPropertyClick(featured.id) },
                        )
                    }
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenRequests,
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f),
                    ) {
                        Text("Mis solicitudes")
                    }
                    OutlinedButton(
                        onClick = onOpenConversations,
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f),
                    ) {
                        Text("Conversaciones")
                    }
                }
                OutlinedButton(
                    onClick = onOpenLandlordRequests,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Solicitudes recibidas (arrendador)")
                }
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
