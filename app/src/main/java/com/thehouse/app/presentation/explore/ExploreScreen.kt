package com.thehouse.app.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.room.RoomSearchCatalog
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.RoomSearchState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.OfflineStatusBanner
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.components.RoomCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.presentation.components.TheHouseSearchField
import com.thehouse.app.ui.theme.Spacing

/**
 * Explorar real D5 + D20 (hotfix): catálogo de habitaciones con filtros
 * SIEMPRE accesibles, búsqueda textual local etiquetada y scroll completo.
 */
@Composable
fun ExploreScreen(
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory? = null,
    roomsEnabled: Boolean = true,
    onRoomClick: (String) -> Unit = {},
) {
    val viewModel: ExploreViewModel? = if (factory != null) {
        viewModel(factory = factory)
    } else null
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: ExploreUiState()
    val isOnline = viewModel?.isOnline?.collectAsStateWithLifecycle()?.value ?: true
    val properties = state.properties
    val query = state.query
    val roomsState = state.rooms
    val showRooms = roomsEnabled

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
                title = "Explora alojamientos",
                subtitle = "Resultados reales de rooms/search. Búsqueda por texto limitada a lo cargado.",
                modifier = Modifier.semantics { contentDescription = "Título Explorar" },
            )

            if (!isOnline) {
                OfflineStatusBanner(
                    text = if (showRooms && roomsState is RoomSearchState.Content) {
                        "Sin conexión · Mostrando resultados cargados"
                    } else "Sin conexión",
                )
            }

            TheHouseSearchField(
                value = query,
                onValueChange = { viewModel?.onEvent(ExploreEvent.QueryChanged(it)) },
                placeholder = if (showRooms) "Filtrar resultados cargados (local)" else "Busca por título o ciudad",
            )

            if (showRooms) {
                FilterBar(
                    filters = state.filters,
                    expanded = state.showFilters,
                    universities = state.universities,
                    onToggle = { viewModel?.onEvent(ExploreEvent.ToggleFilters) },
                    onFiltersChange = { viewModel?.onEvent(ExploreEvent.FilterChanged(it)) },
                )
            }

            if (!showRooms) {
                when (properties) {
                    CatalogLoadState.Loading -> LoadingView(message = "Buscando rentas…", modifier = Modifier.weight(1f))
                    CatalogLoadState.Empty -> EmptyStateView(
                        title = "Aún no hay rentas",
                        message = "No hay alojamientos disponibles por ahora.",
                        modifier = Modifier.weight(1f),
                        ctaLabel = "Reintentar",
                        onCtaClick = { viewModel?.onEvent(ExploreEvent.RetryClicked) },
                    )
                    is CatalogLoadState.Error -> ErrorStateView(
                        title = "Algo salió mal",
                        message = properties.errorMessage() ?: "No pudimos cargar las rentas.",
                        modifier = Modifier.weight(1f),
                        onRetry = { viewModel?.onEvent(ExploreEvent.RetryClicked) },
                    )
                    is CatalogLoadState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
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
            } else {
                when (val rooms = roomsState) {
                    RoomSearchState.Loading -> LoadingView(
                        message = "Buscando alojamientos…",
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    RoomSearchState.Empty -> EmptyStateView(
                        title = if (state.filters.universityId.isNullOrBlank()) {
                            "Selecciona tu universidad"
                        } else "Sin alojamientos",
                        message = if (state.filters.universityId.isNullOrBlank()) {
                            "Elige una universidad para ver habitaciones reales cerca de ti."
                        } else {
                            "Prueba con otros filtros o amplía la distancia (máx. 30 km)."
                        },
                        modifier = Modifier.weight(1f),
                        ctaLabel = "Reintentar",
                        onCtaClick = { viewModel?.onEvent(ExploreEvent.RefreshRoomsClicked) },
                    )
                    is RoomSearchState.Error -> ErrorStateView(
                        title = "Algo salió mal",
                        message = rooms.errorMessage() ?: "No pudimos cargar los alojamientos.",
                        modifier = Modifier.weight(1f),
                        onRetry = { viewModel?.onEvent(ExploreEvent.RefreshRoomsClicked) },
                    )
                    is RoomSearchState.Content -> {
                        val visible = state.visibleRooms()
                        Text(
                            text = if (query.isBlank()) {
                                "${visible.size} de ${rooms.totalItems} alojamientos"
                            } else {
                                "${visible.size} resultados locales de ${rooms.items.size} cargados"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                            contentPadding = PaddingValues(bottom = Spacing.lg),
                        ) {
                            items(visible, key = { it.roomId }) { room ->
                                RoomCard(
                                    room = room,
                                    onRoomClick = { onRoomClick(room.roomId) },
                                    onFavoriteClick = {
                                        viewModel?.onEvent(ExploreEvent.FavoriteRoomClicked(room.roomId))
                                        onFavoriteClick()
                                    },
                                    isFavorite = room.roomId in state.favoriteRoomIds,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (query.isBlank() && rooms.page < rooms.totalPages) {
                                item {
                                    TextButton(
                                        onClick = { viewModel?.onEvent(ExploreEvent.LoadMoreRoomsClicked) },
                                        enabled = !rooms.isPaginating,
                                    ) {
                                        Text(if (rooms.isPaginating) "Cargando…" else "Cargar más")
                                    }
                                }
                            }
                            if (rooms.paginationError != null) {
                                item {
                                    Text(
                                        text = "No se pudo cargar la siguiente página. Reintentá.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    filters: ExploreFiltersUi,
    expanded: Boolean,
    universities: List<com.thehouse.app.domain.model.University>,
    onToggle: () -> Unit,
    onFiltersChange: (ExploreFiltersUi) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(selected = expanded, onClick = onToggle, label = { Text("Filtros") })
            FilterChip(
                selected = filters.roomType != null,
                onClick = {
                    val next = if (filters.roomType != null) null else RoomSearchCatalog.ROOM_TYPES.first()
                    onFiltersChange(filters.copy(roomType = next))
                },
                label = {
                    Text(filters.roomType?.let { RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, it) } ?: "Tipo cuarto")
                },
            )
            FilterChip(
                selected = filters.sort != "distance",
                onClick = {
                    val idx = RoomSearchCatalog.SORTS.indexOf(filters.sort)
                    val next = RoomSearchCatalog.SORTS[(idx + 1) % RoomSearchCatalog.SORTS.size]
                    onFiltersChange(filters.copy(sort = next))
                },
                label = { Text("Orden: " + RoomSearchCatalog.label(RoomSearchCatalog.Kind.SORT, filters.sort)) },
            )
        }
        if (expanded) {
            ClosedDropdown(
                label = "Universidad",
                options = universities.map { it.id },
                labels = universities.associate { it.id to it.name },
                selected = filters.universityId,
                kind = null,
            ) { onFiltersChange(filters.copy(universityId = it)) }
            ClosedDropdown(
                label = "Tipo de inmueble",
                options = RoomSearchCatalog.BUILDING_TYPES,
                selected = filters.buildingType,
                kind = RoomSearchCatalog.Kind.BUILDING_TYPE,
            ) { onFiltersChange(filters.copy(buildingType = it)) }
            ClosedDropdown(
                label = "Baño",
                options = RoomSearchCatalog.BATHROOMS,
                selected = filters.bathroom,
                kind = RoomSearchCatalog.Kind.BATHROOM,
            ) { onFiltersChange(filters.copy(bathroom = it)) }
            ClosedDropdown(
                label = "Convivencia",
                options = RoomSearchCatalog.ALLOWED_TENANTS,
                selected = filters.allowedTenants,
                kind = RoomSearchCatalog.Kind.ALLOWED_TENANTS,
            ) { onFiltersChange(filters.copy(allowedTenants = it)) }
            ClosedDropdown(
                label = "Servicios (multi-selección simple)",
                options = listOf("wifi", "parking", "laundry", "kitchen"),
                selected = filters.services.firstOrNull(),
                kind = null,
            ) { code ->
                onFiltersChange(filters.copy(services = if (code == null) emptyList() else listOf(code)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = filters.minPrice?.toString().orEmpty(),
                    onValueChange = { value ->
                        val parsed = value.toIntOrNull()
                        onFiltersChange(
                            filters.copy(
                                minPrice = if (parsed != null && parsed in RoomSearchCatalog.MIN_PRICE..RoomSearchCatalog.MAX_PRICE) parsed else null,
                            ),
                        )
                    },
                    label = { Text("Precio mín. MXN") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = filters.maxPrice?.toString().orEmpty(),
                    onValueChange = { value ->
                        val parsed = value.toIntOrNull()
                        onFiltersChange(
                            filters.copy(
                                maxPrice = if (parsed != null && parsed in RoomSearchCatalog.MIN_PRICE..RoomSearchCatalog.MAX_PRICE) parsed else null,
                            ),
                        )
                    },
                    label = { Text("Precio máx. MXN") },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = filters.maxDistanceKm.toString(),
                onValueChange = { value ->
                    val parsed = value.toDoubleOrNull()
                    onFiltersChange(
                        filters.copy(
                            maxDistanceKm = if (parsed != null && parsed in RoomSearchCatalog.MIN_DISTANCE_KM..RoomSearchCatalog.MAX_DISTANCE_KM) {
                                parsed
                            } else RoomSearchCatalog.DEFAULT_DISTANCE_KM,
                        ),
                    )
                },
                label = { Text("Distancia máx. km (0.5–30)") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = {
                onFiltersChange(ExploreFiltersUi(universityId = filters.universityId))
            }) { Text("Limpiar filtros") }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClosedDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    kind: RoomSearchCatalog.Kind?,
    labels: Map<String, String> = emptyMap(),
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.let { code -> labels[code] ?: kind?.let { RoomSearchCatalog.label(it, code) } ?: code } ?: "Todos"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { onSelect(null); expanded = false })
            options.forEach { code ->
                val text = labels[code] ?: kind?.let { RoomSearchCatalog.label(it, code) } ?: code
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(code); expanded = false })
            }
        }
    }
}
