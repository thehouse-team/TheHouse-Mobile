package com.thehouse.app.presentation.smartmatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.smartmatch.SmartMatchFactor
import com.thehouse.app.domain.smartmatch.SmartMatchResult
import com.thehouse.app.domain.smartmatch.SmartMatchSource
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.MatchBadge
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHouseHeader
import com.thehouse.app.ui.theme.Spacing

@Composable
fun SmartMatchScreen(
    factory: U2ViewModelFactory,
    onRequireLogin: () -> Unit,
    onConfigurePreferences: () -> Unit,
    onPropertyClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SmartMatchViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    SmartMatchScreenExitLogger.logEnter()
    LaunchedEffect(Unit) {
        vm.load()
        // D16: reconexión sustituye la estimación local por el resultado Backend.
        vm.observeConnectivity()
    }

    LaunchedEffect(state) {
        if (state is SmartMatchUiState.RequiresAuth) {
            onRequireLogin()
        }
    }

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
                title = "Smart Match",
                subtitle = "Recomendaciones según tus preferencias (ML on-device).",
                modifier = Modifier.semantics { contentDescription = "Título Smart Match" },
            )
            when (val s = state) {
                SmartMatchUiState.Loading -> {
                    LoadingView(
                        message = "Calculando recomendaciones con ML…",
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
                    EmptyStateView(
                        title = "Configura tus preferencias",
                        message = "Para generar Smart Match necesitamos tus preferencias de búsqueda.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        ctaLabel = "Configurar preferencias",
                        onCtaClick = onConfigurePreferences,
                    )
                }

                SmartMatchUiState.Empty -> {
                    EmptyStateView(
                        title = "Sin recomendaciones",
                        message = "No encontramos propiedades candidatas por ahora. Intenta más tarde.",
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
                        contentPadding = PaddingValues(bottom = Spacing.lg),
                    ) {
                        items(s.results, key = { it.property.id }) { result ->
                            SmartMatchResultCard(
                                result = result,
                                onPropertyClick = { onPropertyClick(result.property.id) },
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

@Composable
private fun SmartMatchResultCard(
    result: SmartMatchResult,
    onPropertyClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLocal = result.source == SmartMatchSource.LOCAL_ESTIMATE
    val scoreText = formatMatchScore(result.matchScore)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TheHouseCard(contentPadding = PaddingValues(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compatibilidad",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "smart.match.score.$scoreText" },
                    )
                }
                MatchBadge(matchPercent = result.matchScore)
            }
            ScoreBar(progress = (result.matchScore / 100.0).coerceIn(0.0, 1.0))
            if (isLocal) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Estimación local",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = "smart.match.local.fallback" },
                )
            }
            if (result.factors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Factores",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                result.factors.forEach { factor ->
                    SmartMatchFactorRow(factor = factor, showBars = !isLocal)
                }
            }
        }
        PropertyCard(
            property = result.property,
            onFavoriteClick = onFavoriteClick,
            onContactClick = onContactClick,
            onPropertyClick = onPropertyClick,
        )
    }
}

@Composable
private fun SmartMatchFactorRow(
    factor: SmartMatchFactor,
    showBars: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${factor.label}: ${formatFactorPoints(factor.points)} / ${formatFactorPoints(factor.maxPoints)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Contrato confirmado: fraction = si maxPoints>0 → points/maxPoints
        // clamp 0..1; si no → 0.0. Nunca NaN ni división por cero.
        val progress = if (factor.maxPoints > 0.0) {
            (factor.points / factor.maxPoints).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        if (showBars && factor.maxPoints > 0.0) {
            ScoreBar(
                progress = progress,
                modifier = Modifier.semantics { contentDescription = "smart.match.factor.${factor.factor}" },
            )
        }
    }
}

private fun formatFactorPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value)

@Composable
private fun ScoreBar(
    progress: Double,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (fraction <= 0f) 0f else fraction.coerceAtLeast(0.01f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
