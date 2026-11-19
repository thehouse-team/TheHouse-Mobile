package com.thehouse.app.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.OfflineStatusBanner
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.presentation.components.TheHouseSearchField
import com.thehouse.app.ui.theme.Spacing

/**
 * Explorar real D5 (NV-07 parcial, sin mapa).
 *
 * - Listado real con scroll, tarjetas reales (PropertyCard real),
 *   búsqueda local solo sobre título/ciudad (campos reales).
 * - Sin filtros ni mapa (placeholder textual); Smart Match sin cambios D4.
 * - Acciones protegidas delegan al llamador si es Guest.
 */
@Composable
fun ExploreScreen(
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory? = null,
) {
    val viewModel: ExploreViewModel? = if (factory != null) {
        viewModel(factory = factory)
    } else {
        null
    }
    val state = viewModel?.uiState?.collectAsState()?.value
    val isOnline = viewModel?.isOnline?.collectAsState()?.value ?: true
    val properties = state?.properties ?: CatalogLoadState.Empty
    val query = state?.query ?: ""
    val isLoading = properties is CatalogLoadState.Loading

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TheHouseHeader(
                title = "Explora rentas cerca de ti",
                subtitle = "Búsqueda por título o ciudad.",
                modifier = Modifier.semantics { contentDescription = "Título Explorar" },
            )

            if (!isOnline) {
                OfflineStatusBanner(
                    text = if (properties is CatalogLoadState.Success) {
                        "Sin conexión · Mostrando información guardada"
                    } else {
                        "Sin conexión"
                    },
                )
            }

            TheHouseSearchField(
                value = query,
                onValueChange = { viewModel?.onEvent(ExploreEvent.QueryChanged(it)) },
                placeholder = "Busca por título o ciudad",
            )

            when (properties) {
                CatalogLoadState.Loading -> {
                    LoadingView(
                        message = "Buscando rentas…",
                        modifier = Modifier.weight(1f),
                    )
                }
                CatalogLoadState.Empty -> {
                    EmptyStateView(
                        title = if (query.isBlank()) "Aún no hay rentas" else "Sin resultados",
                        message = if (query.isBlank()) {
                            "No hay propiedades disponibles por ahora. Vuelve a intentarlo más tarde."
                        } else {
                            "No encontramos rentas para tu búsqueda. Prueba con otro término."
                        },
                        modifier = Modifier.weight(1f),
                        ctaLabel = "Reintentar",
                        onCtaClick = viewModel?.let { vm -> { vm.onEvent(ExploreEvent.RetryClicked) } },
                    )
                }
                is CatalogLoadState.Error -> {
                    ErrorStateView(
                        title = "Algo salió mal",
                        message = properties.errorMessage()
                            ?: "No pudimos cargar las rentas. Revisa tu conexión e inténtalo de nuevo.",
                        modifier = Modifier.weight(1f),
                        onRetry = { viewModel?.onEvent(ExploreEvent.RetryClicked) },
                    )
                }
                is CatalogLoadState.Success -> {
                    if (!isLoading) {
                        Text(
                            text = if (properties.items.size == 1) {
                                "1 renta disponible"
                            } else {
                                "${properties.items.size} rentas disponibles"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                            contentPadding = PaddingValues(bottom = Spacing.lg),
                        ) {
                            items(properties.items, key = { it.id }) { property ->
                                PropertyCard(
                                    property = property,
                                    onFavoriteClick = onFavoriteClick,
                                    onContactClick = onContactClick,
                                    onPropertyClick = { onPropertyClick(property.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
