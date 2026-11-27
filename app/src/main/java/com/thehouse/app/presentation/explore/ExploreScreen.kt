package com.thehouse.app.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

/** Etiquetas y testTags estables del panel de filtros (D20 hotfix). */
object ExploreFilterTags {
    const val SHEET = "FILTER_SHEET"
    const val PRICE_MIN = "FILTER_PRICE_MIN"
    const val PRICE_MAX = "FILTER_PRICE_MAX"
    const val DISTANCE = "FILTER_DISTANCE"
    const val APPLY = "FILTER_APPLY"
    const val CLEAR = "FILTER_CLEAR"
    const val ERROR = "FILTER_ERROR"
}

/**
 * Explorar real D5 + D20: catálogo de habitaciones con filtros en un
 * ModalBottomSheet acotado, búsqueda textual local etiquetada y scroll completo.
 * El panel NUNCA empuja los resultados fuera del viewport.
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
                subtitle = "Encuentra alojamientos cerca de tu universidad.",
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
                placeholder = if (showRooms) {
                    "Buscar entre los alojamientos mostrados"
                } else "Busca por título o ciudad",
            )

            if (showRooms) {
                FilterBar(
                    filters = state.filters,
                    expanded = state.showFilters,
                    onToggle = { viewModel?.onEvent(ExploreEvent.ToggleFilters) },
                    onQuickApply = { viewModel?.onEvent(ExploreEvent.FilterChanged(it)) },
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
                                "${visible.size} resultados entre los alojamientos mostrados"
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
                                        modifier = Modifier.heightIn(min = 48.dp),
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

    if (showRooms && state.showFilters) {
        FilterBottomSheet(
            draft = state.filterDraft,
            validationError = state.filterValidationError,
            universities = state.universities,
            onDraftChange = { viewModel?.onEvent(ExploreEvent.DraftFilterChanged(it)) },
            onApply = { viewModel?.onEvent(ExploreEvent.ApplyFilters) },
            onClear = { viewModel?.onEvent(ExploreEvent.ClearFilters) },
            onDismiss = { viewModel?.onEvent(ExploreEvent.ToggleFilters) },
        )
    }
}

/** Fila compacta de chips: siempre visible, nunca contiene el formulario. */
@Composable
private fun FilterBar(
    filters: ExploreFiltersUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onQuickApply: (ExploreFiltersUi) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterChip(
            selected = expanded,
            onClick = onToggle,
            label = { Text("Filtros") },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = filters.roomType != null,
            onClick = {
                val next = if (filters.roomType != null) null else RoomSearchCatalog.ROOM_TYPES.first()
                onQuickApply(filters.copy(roomType = next))
            },
            label = {
                Text(filters.roomType?.let { RoomSearchCatalog.label(RoomSearchCatalog.Kind.ROOM_TYPE, it) } ?: "Tipo cuarto")
            },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = filters.sort != "distance",
            onClick = {
                val idx = RoomSearchCatalog.SORTS.indexOf(filters.sort)
                val next = RoomSearchCatalog.SORTS[(idx + 1) % RoomSearchCatalog.SORTS.size]
                onQuickApply(filters.copy(sort = next))
            },
            label = { Text("Orden: " + RoomSearchCatalog.label(RoomSearchCatalog.Kind.SORT, filters.sort)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

/**
 * Panel acotado: el contenido se desplaza y las dos acciones quedan fijas
 * abajo, visibles con teclado abierto o cerrado (imePadding aplicado una vez).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    draft: FilterDraftUi,
    validationError: String?,
    universities: List<com.thehouse.app.domain.model.University>,
    onDraftChange: (FilterDraftUi) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .semantics { contentDescription = "Panel de filtros" },
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = "Filtros de búsqueda",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { contentDescription = "Título Filtros" },
                )

                ClosedDropdown(
                    label = "Universidad",
                    options = universities.map { it.id },
                    labels = universities.associate { it.id to it.name },
                    selected = draft.base.universityId,
                    kind = null,
                ) { onDraftChange(draft.copy(base = draft.base.copy(universityId = it))) }

                ClosedDropdown(
                    label = "Tipo de cuarto",
                    options = RoomSearchCatalog.ROOM_TYPES,
                    selected = draft.base.roomType,
                    kind = RoomSearchCatalog.Kind.ROOM_TYPE,
                ) { onDraftChange(draft.copy(base = draft.base.copy(roomType = it))) }

                ClosedDropdown(
                    label = "Tipo de inmueble",
                    options = RoomSearchCatalog.BUILDING_TYPES,
                    selected = draft.base.buildingType,
                    kind = RoomSearchCatalog.Kind.BUILDING_TYPE,
                ) { onDraftChange(draft.copy(base = draft.base.copy(buildingType = it))) }

                ClosedDropdown(
                    label = "Baño",
                    options = RoomSearchCatalog.BATHROOMS,
                    selected = draft.base.bathroom,
                    kind = RoomSearchCatalog.Kind.BATHROOM,
                ) { onDraftChange(draft.copy(base = draft.base.copy(bathroom = it))) }

                ClosedDropdown(
                    label = "Convivencia",
                    options = RoomSearchCatalog.ALLOWED_TENANTS,
                    selected = draft.base.allowedTenants,
                    kind = RoomSearchCatalog.Kind.ALLOWED_TENANTS,
                ) { onDraftChange(draft.copy(base = draft.base.copy(allowedTenants = it))) }

                ServicesChips(
                    selected = draft.base.services,
                    onToggle = { code, checked ->
                        val next = if (checked) draft.base.services + code else draft.base.services - code
                        onDraftChange(draft.copy(base = draft.base.copy(services = next)))
                    },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = draft.minPriceRaw,
                        onValueChange = { onDraftChange(draft.copy(minPriceRaw = it)) },
                        label = { Text("Precio mín. MXN") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Precio mínimo en pesos" },
                    )
                    OutlinedTextField(
                        value = draft.maxPriceRaw,
                        onValueChange = { onDraftChange(draft.copy(maxPriceRaw = it)) },
                        label = { Text("Precio máx. MXN") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Precio máximo en pesos" },
                    )
                }

                OutlinedTextField(
                    value = draft.distanceRaw,
                    onValueChange = { onDraftChange(draft.copy(distanceRaw = it)) },
                    label = { Text("Distancia máx. km (0.5–30)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Distancia máxima en kilómetros" },
                )

                ClosedDropdown(
                    label = "Ordenar por",
                    options = RoomSearchCatalog.SORTS,
                    selected = draft.base.sort,
                    kind = RoomSearchCatalog.Kind.SORT,
                ) { code ->
                    onDraftChange(draft.copy(base = draft.base.copy(sort = code ?: "distance")))
                }

                if (validationError != null) {
                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { contentDescription = "Error de filtros: $validationError" },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) { Text("Aplicar filtros") }
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Limpiar filtros") }
            }
        }
    }
}

/** Servicios con selección múltiple usando códigos contractuales del Backend. */
@Composable
private fun ServicesChips(
    selected: List<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    val services = listOf("wifi", "parking", "laundry", "kitchen")
    Text("Servicios", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        services.forEach { code ->
            FilterChip(
                selected = code in selected,
                onClick = { onToggle(code, code !in selected) },
                label = { Text(code.replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
