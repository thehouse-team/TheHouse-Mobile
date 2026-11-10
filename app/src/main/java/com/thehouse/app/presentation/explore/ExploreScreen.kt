package com.thehouse.app.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.PlaceSuggestion
import com.thehouse.app.domain.model.RoomSearchFilters
import com.thehouse.app.domain.model.RoomSort
import com.thehouse.app.domain.model.SearchCatalogs
import com.thehouse.app.domain.model.University
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.catalog.CatalogLoadState
import com.thehouse.app.presentation.catalog.errorMessage
import com.thehouse.app.presentation.common.UiError
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.RoomCard
import com.thehouse.app.presentation.components.SearchLabels
import com.thehouse.app.presentation.components.formatDistance

@Composable
fun ExploreDestination(
    factory: U2ViewModelFactory,
    onOpenRoomDetail: (String) -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ExploreViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExploreEffect.OpenRoomDetail -> onOpenRoomDetail(effect.roomId)
                ExploreEffect.RequireLogin -> onRequireLogin()
            }
        }
    }

    ExploreScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/** UI stateless: no red, DTO ni lógica de negocio. */
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onEvent: (ExploreEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUniversityPicker by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Explorar",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "Título Explorar"
                        heading()
                    },
                )
            }
            item {
                Text(
                    text = "Rentas reales cerca de tu universidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                UniversitySection(
                    universities = state.universities,
                    selectedUniversity = state.selectedUniversity,
                    selectedUniversityId = state.selectedUniversityId,
                    showPicker = showUniversityPicker,
                    onTogglePicker = { showUniversityPicker = !showUniversityPicker },
                    onSelect = { id ->
                        showUniversityPicker = false
                        onEvent(ExploreEvent.UniversitySelected(id))
                    },
                    onRetry = { onEvent(ExploreEvent.Retry) },
                )
            }

            if (state.selectedUniversityId != null) {
                item {
                    PlacesSearch(
                        query = state.placesQuery,
                        selectedPlace = state.selectedPlace,
                        suggestions = state.placeSuggestions,
                        isSearching = state.isSearchingPlaces,
                        error = state.placesError,
                        onQueryChange = { onEvent(ExploreEvent.QueryChanged(it)) },
                        onPlaceSelected = { onEvent(ExploreEvent.PlaceSelected(it)) },
                        onClear = { onEvent(ExploreEvent.ClearPlace) },
                    )
                }
                item {
                    ModeSelector(
                        mode = state.mode,
                        onModeChange = { onEvent(ExploreEvent.ModeChanged(it)) },
                    )
                }
                item {
                    FilterBar(
                        filters = state.filters,
                        expanded = state.filtersExpanded,
                        onToggle = { onEvent(ExploreEvent.ToggleFilters) },
                        onFiltersChange = { onEvent(ExploreEvent.FiltersChanged(it)) },
                    )
                }
                if (state.filtersExpanded) {
                    item {
                        FilterPanel(
                            filters = state.filters,
                            onFiltersChange = { onEvent(ExploreEvent.FiltersChanged(it)) },
                        )
                    }
                }

                if (state.mode == ExploreMode.LIST) {
                    searchItems(state, onEvent)
                } else {
                    mapItems(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun UniversitySection(
    universities: CatalogLoadState<University>,
    selectedUniversity: University?,
    selectedUniversityId: String?,
    showPicker: Boolean,
    onTogglePicker: () -> Unit,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (universities) {
        CatalogLoadState.Loading -> CircularProgressIndicator(modifier = Modifier.height(24.dp))
        CatalogLoadState.Empty -> Text(
            "Sin universidades disponibles.",
            style = MaterialTheme.typography.bodyMedium,
        )
        is CatalogLoadState.Error -> {
            Text(
                universities.errorMessage() ?: "No se pudieron cargar las universidades.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Reintentar")
            }
        }
        is CatalogLoadState.Success -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Universidad",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = selectedUniversity?.name
                        ?: if (selectedUniversityId != null) "Universidad seleccionada" else "Selecciona tu universidad para buscar",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = onTogglePicker,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(if (selectedUniversity != null) "Cambiar universidad" else "Buscar tu universidad")
                }
                if (showPicker) {
                    universities.items.forEach { university ->
                        TextButton(
                            onClick = { onSelect(university.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("${university.name} · ${university.city}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesSearch(
    query: String,
    selectedPlace: PlaceSuggestion?,
    suggestions: List<PlaceSuggestion>,
    isSearching: Boolean,
    error: UiError?,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlaceSuggestion) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Buscar colonia o ciudad") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            supportingText = {
                Text("Autocompletado real de colonias (acentos incluidos). El listado usa los filtros.")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp))
        }
        if (error != null) {
            Text(
                text = UiErrorMessages.map(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (selectedPlace != null) {
            Text(
                text = "Colonia seleccionada: ${selectedPlace.name} · ${formatDistance(selectedPlace.distanceKm)} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        suggestions.forEach { place ->
            TextButton(
                onClick = { onPlaceSelected(place) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    "${place.name} · ${SearchLabels.placeType(place.type)} · " +
                        "${formatDistance(place.distanceKm)} km",
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    mode: ExploreMode,
    onModeChange: (ExploreMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == ExploreMode.LIST,
            onClick = { onModeChange(ExploreMode.LIST) },
            label = { Text("Lista") },
            leadingIcon = { Icon(Icons.Filled.List, contentDescription = null) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = mode == ExploreMode.MAP,
            onClick = { onModeChange(ExploreMode.MAP) },
            label = { Text("Mapa") },
            leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun FilterBar(
    filters: RoomSearchFilters,
    expanded: Boolean,
    onToggle: () -> Unit,
    onFiltersChange: (RoomSearchFilters) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RoomSort.entries.toList()) { sort ->
                FilterChip(
                    selected = filters.sort == sort,
                    onClick = { onFiltersChange(filters.copy(sort = sort)) },
                    label = {
                        Text(
                            when (sort) {
                                RoomSort.DISTANCE -> "Distancia"
                                RoomSort.PRICE -> "Precio"
                                RoomSort.NEWEST -> "Recientes"
                            },
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            item {
                FilterChip(
                    selected = expanded,
                    onClick = onToggle,
                    label = { Text("Filtros") },
                    leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    filters: RoomSearchFilters,
    onFiltersChange: (RoomSearchFilters) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Precio máximo", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.maxPrice == null,
                        onClick = { onFiltersChange(filters.copy(maxPrice = null)) },
                        label = { Text("Sin límite") },
                    )
                }
                items(listOf(3000, 5000, 8000, 12000)) { price ->
                    FilterChip(
                        selected = filters.maxPrice == price,
                        onClick = { onFiltersChange(filters.copy(maxPrice = price)) },
                        label = { Text("$$price") },
                    )
                }
            }
            Text("Precio mínimo", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.minPrice == null,
                        onClick = { onFiltersChange(filters.copy(minPrice = null)) },
                        label = { Text("Cualquiera") },
                    )
                }
                items(listOf(500, 1500, 2500, 4000)) { price ->
                    FilterChip(
                        selected = filters.minPrice == price,
                        onClick = { onFiltersChange(filters.copy(minPrice = price)) },
                        label = { Text("Desde $$price") },
                    )
                }
            }
            Text("Distancia a la universidad", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(2.0, 5.0, 10.0, 30.0)) { distance ->
                    FilterChip(
                        selected = filters.maxDistanceKm == distance,
                        onClick = { onFiltersChange(filters.copy(maxDistanceKm = distance)) },
                        label = { Text("${formatDistance(distance)} km") },
                    )
                }
            }
            Text("Servicios", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchCatalogs.services) { service ->
                    FilterChip(
                        selected = service in filters.services,
                        onClick = {
                            val services = if (service in filters.services) {
                                filters.services - service
                            } else {
                                filters.services + service
                            }
                            onFiltersChange(filters.copy(services = services))
                        },
                        label = { Text(SearchLabels.service(service)) },
                    )
                }
            }
            Text("Tipo de cuarto", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.roomType == null,
                        onClick = { onFiltersChange(filters.copy(roomType = null)) },
                        label = { Text("Todos") },
                    )
                }
                items(SearchCatalogs.roomTypes) { type ->
                    FilterChip(
                        selected = filters.roomType == type,
                        onClick = { onFiltersChange(filters.copy(roomType = type)) },
                        label = { Text(SearchLabels.roomType(type).orEmpty()) },
                    )
                }
            }
            Text("Baño", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.bathroom == null,
                        onClick = { onFiltersChange(filters.copy(bathroom = null)) },
                        label = { Text("Cualquiera") },
                    )
                }
                items(SearchCatalogs.bathrooms) { bathroom ->
                    FilterChip(
                        selected = filters.bathroom == bathroom,
                        onClick = { onFiltersChange(filters.copy(bathroom = bathroom)) },
                        label = { Text(SearchLabels.bathroom(bathroom).orEmpty()) },
                    )
                }
            }
            Text("Tipo de edificio", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.buildingType == null,
                        onClick = { onFiltersChange(filters.copy(buildingType = null)) },
                        label = { Text("Cualquiera") },
                    )
                }
                items(SearchCatalogs.buildingTypes) { type ->
                    FilterChip(
                        selected = filters.buildingType == type,
                        onClick = { onFiltersChange(filters.copy(buildingType = type)) },
                        label = { Text(SearchLabels.buildingType(type)) },
                    )
                }
            }
            Text("Pueden vivir", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.allowedTenants == null,
                        onClick = { onFiltersChange(filters.copy(allowedTenants = null)) },
                        label = { Text("Cualquiera") },
                    )
                }
                items(SearchCatalogs.allowedTenants) { tenants ->
                    FilterChip(
                        selected = filters.allowedTenants == tenants,
                        onClick = { onFiltersChange(filters.copy(allowedTenants = tenants)) },
                        label = { Text(SearchLabels.allowedTenants(tenants)) },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchItems(
    state: ExploreUiState,
    onEvent: (ExploreEvent) -> Unit,
) {
    when (val results = state.results) {
        SearchResultsState.Idle -> item {
            Text("Selecciona tu universidad para buscar.", style = MaterialTheme.typography.bodyMedium)
        }
        SearchResultsState.Loading -> item {
            LoadingRow("Buscando rentas…")
        }
        SearchResultsState.Empty -> item {
            EmptyOrError(
                message = "No encontramos cuartos con esos filtros.",
                detail = "Ajusta los filtros o amplía la distancia.",
                onRetry = { onEvent(ExploreEvent.Retry) },
            )
        }
        is SearchResultsState.Error -> item {
            EmptyOrError(
                message = UiErrorMessages.map(results.error),
                detail = "Puedes reintentar sin perder la pantalla.",
                onRetry = { onEvent(ExploreEvent.Retry) },
            )
        }
        is SearchResultsState.Content -> {
            item {
                Text(
                    text = "${results.totalItems} rentas encontradas",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(results.items, key = { it.id }) { room ->
                RoomCard(
                    room = room,
                    onClick = { onEvent(ExploreEvent.RoomClicked(room.id)) },
                )
            }
            if (results.isLoadingMore) {
                item { LoadingRow("Cargando más resultados…") }
            } else if (results.loadMoreError != null) {
                item {
                    EmptyOrError(
                        message = UiErrorMessages.map(results.loadMoreError),
                        detail = "Los resultados ya cargados siguen disponibles.",
                        onRetry = { onEvent(ExploreEvent.LoadMore) },
                    )
                }
            } else if (results.page < results.totalPages) {
                item {
                    OutlinedButton(
                        onClick = { onEvent(ExploreEvent.LoadMore) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Cargar más")
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mapItems(
    state: ExploreUiState,
    onEvent: (ExploreEvent) -> Unit,
) {
    when (val map = state.map) {
        MapResultsState.Idle -> item { LoadingRow("Preparando el mapa…") }
        MapResultsState.Loading -> item { LoadingRow("Cargando marcadores…") }
        MapResultsState.Empty -> item {
            EmptyOrError(
                message = "Sin marcadores en esta área.",
                detail = "Mueve el mapa o ajusta los filtros para buscar en otra zona.",
                onRetry = { onEvent(ExploreEvent.RetryMap) },
            )
        }
        MapResultsState.InvalidArea -> item {
            EmptyOrError(
                message = "El área es demasiado grande.",
                detail = "Acerca el mapa (máximo 0.3° por lado) para ver marcadores.",
                onRetry = { onEvent(ExploreEvent.RetryMap) },
            )
        }
        is MapResultsState.Error -> item {
            EmptyOrError(
                message = UiErrorMessages.map(map.error),
                detail = "El mapa no está disponible en este momento.",
                onRetry = { onEvent(ExploreEvent.RetryMap) },
            )
        }
        is MapResultsState.Content -> {
            val bounds = state.mapBounds
            if (bounds != null) {
                item {
                    RoomMapView(
                        markers = map.markers,
                        bounds = bounds,
                        onViewportChanged = { onEvent(ExploreEvent.MapViewportChanged(it)) },
                        onMarkerClick = { onEvent(ExploreEvent.MarkerClicked(it)) },
                        modifier = Modifier.fillMaxWidth().height(380.dp),
                    )
                }
                item {
                    Text(
                        text = "Marcadores aproximados. La dirección exacta no se muestra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.selectedMarker?.let { marker ->
                    item {
                        MarkerPreview(
                            price = marker.price,
                            distanceKm = marker.distanceKm,
                            onOpen = { onEvent(ExploreEvent.MarkerDetailClicked) },
                            onClose = { onEvent(ExploreEvent.MarkerPreviewClosed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerPreview(
    price: Int,
    distanceKm: Double,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$$price MXN/mes", style = MaterialTheme.typography.titleMedium)
            Text(
                "A ${formatDistance(distanceKm)} km de tu universidad",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("Ver detalle")
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun LoadingRow(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyOrError(
    message: String,
    detail: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Reintentar")
        }
    }
}
