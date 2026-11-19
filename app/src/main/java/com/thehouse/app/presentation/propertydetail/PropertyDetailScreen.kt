package com.thehouse.app.presentation.propertydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.thehouse.app.presentation.components.InfoChip
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.OfflineStatusBanner
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.ui.theme.TheHouseTheme

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
    val viewModel: PropertyDetailViewModel = viewModel(
        key = "property-detail:$propertyId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(viewModel, onRequireLogin) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PropertyDetailEffect.RequireLogin -> onRequireLogin()
                PropertyDetailEffect.ScheduleVisit -> onScheduleVisit(propertyId)
                PropertyDetailEffect.MakeRequest -> onMakeRequest(propertyId)
                is PropertyDetailEffect.ActionUnavailable -> snackbarState.showSnackbar(
                    when (effect.action) {
                        ProtectedAction.SaveFavorite ->
                            "Favoritos aún no disponible. No se ha guardado la propiedad."
                        else ->
                            "Contacto aún no disponible. No se ha enviado ningún mensaje."
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
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarState) },
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
    modifier: Modifier = Modifier,
    onOpenVisits: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    isOnline: Boolean = true,
    snackbarHost: @Composable () -> Unit = {},
) {
    TheHouseScreen(
        title = "Detalle de la propiedad",
        onBack = onBack,
        modifier = modifier,
        snackbarHost = snackbarHost,
        actions = {
            IconButton(onClick = onOpenVisits) {
                Icon(
                    imageVector = Icons.Outlined.EventAvailable,
                    contentDescription = "Mis visitas",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenRequests) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Mis solicitudes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.xl,
                end = Spacing.xl,
                top = Spacing.md,
                bottom = Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (!isOnline) {
                item {
                    OfflineStatusBanner(
                        text = if (state is PropertyDetailUiState.Content) {
                            "Sin conexión · Mostrando información guardada"
                        } else {
                            "Sin conexión"
                        },
                    )
                }
            }
            when (state) {
                PropertyDetailUiState.Loading -> item {
                    LoadingView(
                        message = "Cargando propiedad",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xxxl)
                            .testTag("property_detail_loading"),
                    )
                }
                PropertyDetailUiState.NotFound -> item {
                    DetailFailure(
                        message = "Propiedad no encontrada.",
                        onRetry = onRetry,
                        tag = "property_detail_not_found",
                    )
                }
                is PropertyDetailUiState.Error -> item {
                    DetailFailure(
                        message = UiErrorMessages.map(state.error),
                        onRetry = onRetry,
                        tag = "property_detail_error",
                    )
                }
                is PropertyDetailUiState.Content -> {
                    item { PropertyPhotos(state.property) }
                    item { PropertyInformation(state.property) }
                    item {
                        DetailActions(
                            onContactClick = onContactClick,
                            onVisitClick = onVisitClick,
                            onFavoriteClick = onFavoriteClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFailure(message: String, onRetry: () -> Unit, tag: String) {
    TheHouseCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            TheHouseSecondaryButton(
                text = "Reintentar",
                onClick = onRetry,
            )
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
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                )
                .testTag("property_image_placeholder"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Roofing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(IconSizes.hero),
                )
                Text(
                    text = "Sin imágenes disponibles",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = photo.url.ifBlank { photo.thumbUrl },
                contentDescription = "Foto de ${property.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(240.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
    }
}

@Composable
private fun PropertyInformation(property: Property) {
    TheHouseCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("property_detail_content"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (property.title.isNotBlank()) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "${property.price} ${property.currency}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "por mes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
            if (property.city.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSizes.sm),
                    )
                    Text(
                        text = property.city,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            property.description?.takeIf { it.isNotBlank() }?.let { description ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val amenities = property.amenities.filter { it.isNotBlank() }
            if (amenities.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Amenidades",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                amenities.chunked(2).forEach { rowAmenities ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        rowAmenities.forEach { amenity ->
                            InfoChip(
                                label = amenity,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowAmenities.size == 1) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailActions(
    onContactClick: () -> Unit,
    onVisitClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TheHousePrimaryButton(
            text = "Solicitar este cuarto",
            onClick = onContactClick,
        )
        TheHouseSecondaryButton(
            text = "Agendar visita",
            onClick = onVisitClick,
        )
        TheHouseSecondaryButton(
            text = "Guardar favorito",
            onClick = onFavoriteClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailLoadingPreview() {
    TheHouseTheme {
        PropertyDetailScreen(PropertyDetailUiState.Loading, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailNotFoundPreview() {
    TheHouseTheme {
        PropertyDetailScreen(PropertyDetailUiState.NotFound, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailErrorPreview() {
    TheHouseTheme {
        PropertyDetailScreen(PropertyDetailUiState.Error(UiError.Network), {}, {}, {}, {}, {})
    }
}
