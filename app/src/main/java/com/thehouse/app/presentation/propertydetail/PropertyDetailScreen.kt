package com.thehouse.app.presentation.propertydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.thehouse.app.domain.auth.ProtectedAction
import com.thehouse.app.domain.model.Property
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.OfflineStatusBanner

@Composable
fun PropertyDetailDestination(
    propertyId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onScheduleVisit: (String) -> Unit,
    onOpenVisits: () -> Unit,
    onOpenRequests: () -> Unit = {},
    onMakeRequest: (String) -> Unit = {},
) {
    // onOpenVisits se usa en la barra superior de la pantalla stateless.
    val viewModel: PropertyDetailViewModel = viewModel(
        key = "property-detail:$propertyId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, onRequireLogin) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PropertyDetailEffect.RequireLogin -> onRequireLogin()
                PropertyDetailEffect.ScheduleVisit -> onScheduleVisit(propertyId)
                PropertyDetailEffect.MakeRequest -> onMakeRequest(propertyId)
                is PropertyDetailEffect.ActionUnavailable -> snackbarState.showSnackbar(
                    when (effect.action) {
                        ProtectedAction.SaveFavorite -> "Favoritos aún no disponible. No se ha guardado la propiedad."
                        else -> "Contacto aún no disponible. No se ha enviado ningún mensaje."
                    },
                )
            }
        }
    }

    PropertyDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(PropertyDetailEvent.Retry) },
        onFavoriteClick = { viewModel.onEvent(PropertyDetailEvent.FavoriteClicked) },
        onContactClick = { viewModel.onEvent(PropertyDetailEvent.ContactClicked) },
        onVisitClick = { viewModel.onEvent(PropertyDetailEvent.VisitClicked) },
        onOpenVisits = onOpenVisits,
        onOpenRequests = onOpenRequests,
        isOnline = isOnline,
        snackbarHost = { SnackbarHost(snackbarState) },
    )
}

/** UI stateless: no red, DTO, sesión ni carga ligada a recomposición. */
@Composable
fun PropertyDetailScreen(
    state: PropertyDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onVisitClick: () -> Unit,
    onOpenVisits: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    isOnline: Boolean = true,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            Row {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
                ) {
                    Text("Volver")
                }
                TextButton(
                    onClick = onOpenVisits,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Mis visitas")
                }
                TextButton(
                    onClick = onOpenRequests,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Mis solicitudes")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Detalle de la propiedad",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (!isOnline) {
                item {
                    OfflineStatusBanner(
                        text = if (state is PropertyDetailUiState.Content) {
                            "Sin conexión · Mostrando información guardada"
                        } else {
                            "Sin conexión"
                        }
                    )
                }
            }
            when (state) {
                PropertyDetailUiState.Loading -> item {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("property_detail_loading")
                            .semantics { contentDescription = "Cargando propiedad" },
                    )
                }
                PropertyDetailUiState.NotFound -> item {
                    DetailFailure("Propiedad no encontrada.", onRetry, "property_detail_not_found")
                }
                is PropertyDetailUiState.Error -> item {
                    DetailFailure(
                        UiErrorMessages.map(state.error),
                        onRetry,
                        "property_detail_error",
                    )
                }
                is PropertyDetailUiState.Content -> {
                    item { PropertyPhotos(state.property) }
                    item { PropertyInformation(state.property) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onFavoriteClick,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text("Guardar favorito")
                            }
                            Button(
                                onClick = onContactClick,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text("Solicitar este cuarto")
                            }
                            OutlinedButton(
                                onClick = onVisitClick,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text("Agendar visita")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFailure(message: String, onRetry: () -> Unit, tag: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun PropertyPhotos(property: Property) {
    val photos = property.photos
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .testTag("property_image_placeholder"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Sin imágenes disponibles",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = photo.url.ifBlank { photo.thumbUrl },
                contentDescription = "Foto de ${property.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun PropertyInformation(property: Property) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("property_detail_content"),
    ) {
        if (property.title.isNotBlank()) {
            Text(
                property.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            "${property.price} ${property.currency}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (property.city.isNotBlank()) {
            Text(property.city, style = MaterialTheme.typography.bodyLarge)
        }
        property.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text("Descripción", style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
        val amenities = property.amenities.filter { it.isNotBlank() }
        if (amenities.isNotEmpty()) {
            Text("Amenidades", style = MaterialTheme.typography.titleMedium)
            amenities.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailLoadingPreview() {
    MaterialTheme {
        PropertyDetailScreen(PropertyDetailUiState.Loading, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailNotFoundPreview() {
    MaterialTheme {
        PropertyDetailScreen(PropertyDetailUiState.NotFound, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailErrorPreview() {
    MaterialTheme {
        PropertyDetailScreen(PropertyDetailUiState.Error(UiError.Network), {}, {}, {}, {}, {})
    }
}
