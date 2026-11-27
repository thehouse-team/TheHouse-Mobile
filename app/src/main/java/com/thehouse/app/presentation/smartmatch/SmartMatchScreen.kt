package com.thehouse.app.presentation.smartmatch

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.ui.theme.Spacing

@Composable
fun SmartMatchScreen(
    factory: U2ViewModelFactory,
    onRequireLogin: () -> Unit,
    onConfigurePreferences: (List<String>) -> Unit,
    onOpenProfile: () -> Unit = {},
    onRoomClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SmartMatchViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val missingFields by vm.profileMissing.collectAsStateWithLifecycle()

    SmartMatchScreenExitLogger.logEnter()
    LaunchedEffect(Unit) {
        vm.load()
        vm.observeConnectivity()
    }

    LaunchedEffect(state) {
        if (state is SmartMatchUiState.RequiresAuth) {
            onRequireLogin()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                title = "Smart Match",
                subtitle = "Encuentra alojamientos compatibles con tus preferencias.",
                modifier = Modifier.semantics { contentDescription = "Título Smart Match" },
            )
            when (val s = state) {
                SmartMatchUiState.Loading -> {
                    LoadingView(
                        message = "Buscando alojamientos compatibles…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                SmartMatchUiState.RequiresAuth -> {
                    EmptyStateView(
                        title = "Inicia sesión",
                        message = "Necesitas iniciar sesión para ver recomendaciones personalizadas.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        ctaLabel = "Iniciar sesión",
                        onCtaClick = onRequireLogin,
                    )
                }

                SmartMatchUiState.NoPreferences -> {
                    // El Backend reporta los campos reales en el 422.
                    // Nunca se inventan valores ni se activa fallback local por 422.
                    val missingLabels = missingFields.map(::labelForMissingField)
                    EmptyStateView(
                        title = "Completa tu perfil para Smart Match",
                        message = if (missingLabels.isEmpty()) {
                            "Para generar Smart Match necesitamos tus preferencias de búsqueda."
                        } else {
                            "Faltan: ${missingLabels.joinToString(", ")}. " +
                                "Completa estos datos para ver recomendaciones."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        ctaLabel = if (missingFields.contains("university")) {
                            "Completar mi perfil"
                        } else {
                            "Configurar preferencias"
                        },
                        onCtaClick = {
                            if (missingFields.contains("university")) {
                                onOpenProfile()
                            } else {
                                onConfigurePreferences(missingFields)
                            }
                        },
                    )
                    if (missingLabels.isNotEmpty()) {
                        Text(
                            text = "Campos pendientes: ${missingLabels.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "smart.match.missing.$missingFields" },
                        )
                    }
                    if (missingFields.any { it != "university" } && missingFields.contains("university")) {
                        TextButton(onClick = { onConfigurePreferences(missingFields.filter { it != "university" }) }) {
                            Text("Configurar preferencias")
                        }
                    }
                }

                SmartMatchUiState.Empty -> {
                    EmptyStateView(
                        title = "Sin recomendaciones",
                        message = "No encontramos alojamientos compatibles con tus preferencias actuales.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                is SmartMatchUiState.Error -> {
                    ErrorStateView(
                        title = "No pudimos cargar Smart Match",
                        message = UiErrorMessages.map(s.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onRetry = { vm.load() },
                    )
                }

                is SmartMatchUiState.Content -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        contentPadding = PaddingValues(bottom = Spacing.xxl),
                    ) {
                        // Clave estable: roomId real del Backend (nunca propertyId).
                        items(s.results, key = { it.roomId }) { result ->
                            SmartMatchRecommendationCard(
                                result = result,
                                onRoomClick = { onRoomClick(result.roomId) },
                                onFavoriteClick = { onFavoriteClick(result.property.id) },
                                onContactClick = { onContactClick(result.property.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Contrato real: matchScore entero 0..100 → "73%". Nunca "73.0%". */
internal fun formatMatchScore(score: Int): String = "$score%"

/** Etiquetas legibles de los campos que el Backend reporta como incompletos. */
internal fun labelForMissingField(field: String): String = when (field) {
    "university" -> "universidad"
    "budget.max" -> "presupuesto máximo"
    "budget" -> "presupuesto"
    "livingWith" -> "con quién vivir"
    "placeType" -> "tipo de lugar"
    else -> field
}
