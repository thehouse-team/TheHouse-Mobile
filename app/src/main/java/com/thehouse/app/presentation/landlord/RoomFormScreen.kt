package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.error.LandlordErrorCode
import com.thehouse.app.domain.error.LandlordException
import com.thehouse.app.domain.model.BathroomType
import com.thehouse.app.domain.model.Furniture
import com.thehouse.app.domain.model.MinContract
import com.thehouse.app.domain.model.RoomType
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

@Composable
fun RoomFormDestination(
    buildingId: String,
    roomId: String?,
    factory: com.thehouse.app.presentation.U2ViewModelFactory,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val formFactory = remember(buildingId, roomId) { factory.roomForm(buildingId, roomId) }
    val viewModel: RoomFormViewModel = viewModel(factory = formFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is RoomFormEffect.Saved || effect is RoomFormEffect.Deleted) onSaved()
        }
    }
    RoomFormScreen(
        state = state,
        onBack = onBack,
        onEvent = { viewModel.onEvent(it) },
        onReload = { viewModel.onEvent(RoomFormEvent.Retry) },
    )
}

@Composable
fun RoomFormScreen(
    state: RoomFormUiState,
    onBack: () -> Unit,
    onEvent: (RoomFormEvent) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = Heights.button)) {
                Text("Volver")
            }
            Text(
                if (state.isEdit) "Editar cuarto" else "Agregar cuartos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            when {
                state.isLoading -> LoadingView(
                    message = "Cargando cuarto…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.versionConflict -> ErrorStateView(
                    title = "Conflicto de edición",
                    message = LandlordErrorMessages.map(
                        LandlordException(DomainError.Conflict, LandlordErrorCode.VERSION_CONFLICT),
                    ),
                    retryLabel = "Recargar datos",
                    onRetry = onReload,
                )
                else -> {
                    if (!state.isEdit) {
                        Text(
                            "Los cuartos se generan por piso con numeración automática " +
                                "(101, 102, 201…).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = state.floors,
                                onValueChange = { onEvent(RoomFormEvent.FloorsChanged(it)) },
                                label = { Text("Pisos") },
                                isError = state.floorsError != null,
                                supportingText = { state.floorsError?.let { Text(it) } },
                                modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = state.roomsPerFloor,
                                onValueChange = { onEvent(RoomFormEvent.RoomsPerFloorChanged(it)) },
                                label = { Text("Cuartos por piso") },
                                isError = state.roomsPerFloorError != null,
                                supportingText = { state.roomsPerFloorError?.let { Text(it) } },
                                modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                                singleLine = true,
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = state.number,
                                onValueChange = { onEvent(RoomFormEvent.NumberChanged(it)) },
                                label = { Text("Número") },
                                modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = state.floor,
                                onValueChange = { onEvent(RoomFormEvent.FloorChanged(it)) },
                                label = { Text("Piso") },
                                modifier = Modifier.weight(1f).heightIn(min = Heights.input),
                                singleLine = true,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.price,
                        onValueChange = { onEvent(RoomFormEvent.PriceChanged(it)) },
                        label = { Text("Renta mensual (MXN)") },
                        isError = state.priceError != null,
                        supportingText = { state.priceError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.sizeM2,
                        onValueChange = { onEvent(RoomFormEvent.SizeChanged(it)) },
                        label = { Text("Tamaño (m²)") },
                        isError = state.sizeError != null,
                        supportingText = { state.sizeError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                        singleLine = true,
                    )

                    Text("Tipo de cuarto", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        RoomType.entries.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { onEvent(RoomFormEvent.TypeChanged(type)) },
                                label = { Text(typeLabel(type)) },
                            )
                        }
                    }

                    Text("Baño", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BathroomType.entries.forEach { bath ->
                            FilterChip(
                                selected = state.bathroom == bath,
                                onClick = { onEvent(RoomFormEvent.BathroomChanged(bath)) },
                                label = { Text(if (bath == BathroomType.PRIVATE) "Privado" else "Compartido") },
                            )
                        }
                    }

                    Text("Muebles", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Furniture.entries.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                rowItems.forEach { item ->
                                    FilterChip(
                                        selected = item in state.furniture,
                                        onClick = { onEvent(RoomFormEvent.FurnitureToggled(item)) },
                                        label = { Text(furnitureLabel(item)) },
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.availableFrom,
                        onValueChange = { onEvent(RoomFormEvent.AvailableFromChanged(it)) },
                        label = { Text("Disponible desde (YYYY-MM-DD)") },
                        isError = state.availableFromError != null,
                        supportingText = { state.availableFromError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Heights.input),
                        singleLine = true,
                    )

                    Text("Contrato mínimo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        MinContract.entries.forEach { contract ->
                            FilterChip(
                                selected = state.minContract == contract,
                                onClick = { onEvent(RoomFormEvent.MinContractChanged(contract)) },
                                label = { Text(contractLabel(contract)) },
                            )
                        }
                    }

                    state.errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Button(
                            onClick = { onEvent(RoomFormEvent.Submit) },
                            enabled = !state.isSaving && !state.isLoading && state.isValid,
                            modifier = Modifier.weight(2f).heightIn(min = Heights.button),
                        ) {
                            Text(
                                when {
                                    state.isSaving -> "Guardando…"
                                    state.isEdit -> "Guardar cambios"
                                    else -> "Generar cuartos"
                                },
                            )
                        }
                        if (state.isEdit) {
                            OutlinedButton(
                                onClick = { onEvent(RoomFormEvent.Delete) },
                                enabled = !state.isSaving,
                                modifier = Modifier.weight(1f).heightIn(min = Heights.button),
                            ) {
                                Text(
                                    if (state.isSaving) "…" else "Eliminar",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun typeLabel(type: RoomType): String = when (type) {
    RoomType.SINGLE -> "Individual"
    RoomType.DOUBLE -> "Doble"
    RoomType.SHARED -> "Compartido"
}

private fun furnitureLabel(furniture: Furniture): String = when (furniture) {
    Furniture.BED -> "Cama"
    Furniture.DESK -> "Escritorio"
    Furniture.CHAIR -> "Silla"
    Furniture.CLOSET -> "Clóset"
    Furniture.DRESSER -> "Cómoda"
    Furniture.TV -> "TV"
    Furniture.FRIDGE -> "Refri"
    Furniture.MICROWAVE -> "Micro"
}

private fun contractLabel(contract: MinContract): String = when (contract) {
    MinContract.FOUR_MONTHS -> "4 meses"
    MinContract.SIX_MONTHS -> "6 meses"
    MinContract.ANNUAL -> "Anual"
}
