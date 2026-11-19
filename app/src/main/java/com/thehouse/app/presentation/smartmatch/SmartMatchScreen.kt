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
import com.thehouse.app.domain.smartmatch.SmartMatchResult
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

    LaunchedEffect(Unit) { vm.load() }

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

@Composable
private fun SmartMatchResultCard(
    result: SmartMatchResult,
    onPropertyClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        text = "${result.score}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                MatchBadge(matchPercent = result.score)
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            ScoreBar(score = result.score)
            if (result.factors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Factores",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                result.factors.take(4).forEach { factor ->
                    Text(
                        text = "• $factor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
private fun ScoreBar(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = score.coerceIn(0, 100) / 100f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
