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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.common.UiErrorMessages
import com.thehouse.app.presentation.components.EmptyStateView
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.MatchBadge
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.domain.smartmatch.SmartMatchResult

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

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Smart Match",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título Smart Match" },
            )
            when (val s = state) {
                SmartMatchUiState.Loading -> {
                    LoadingView(
                        message = "Calculando recomendaciones con ML…",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                SmartMatchUiState.RequiresAuth -> {
                    EmptyStateView(
                        title = "Inicia sesión",
                        message = "Necesitas iniciar sesión para ver recomendaciones personalizadas.",
                        modifier = Modifier.fillMaxSize(),
                        ctaLabel = "Iniciar sesión",
                        onCtaClick = onRequireLogin,
                    )
                }

                SmartMatchUiState.NoPreferences -> {
                    EmptyStateView(
                        title = "Configura tus preferencias",
                        message = "Para generar Smart Match necesitamos tus preferencias de búsqueda.",
                        modifier = Modifier.fillMaxSize(),
                        ctaLabel = "Configurar preferencias",
                        onCtaClick = onConfigurePreferences,
                    )
                }

                SmartMatchUiState.Empty -> {
                    EmptyStateView(
                        title = "Sin recomendaciones",
                        message = "No encontramos propiedades candidatas por ahora. Intenta más tarde.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is SmartMatchUiState.Error -> {
                    ErrorStateView(
                        title = "No pudimos cargar Smart Match",
                        message = UiErrorMessages.map(s.error),
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { vm.load() },
                    )
                }

                is SmartMatchUiState.Content -> {
                    Text(
                        text = "Recomendaciones basadas en tus preferencias (ML on-device).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Compatibilidad: ${result.score}%",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        MatchBadge(matchPercent = result.score)
        if (result.factors.isNotEmpty()) {
            Text(
                text = "Factores",
                style = MaterialTheme.typography.labelLarge,
            )
            result.factors.take(4).forEach { factor ->
                Text(
                    text = "• $factor",
                    style = MaterialTheme.typography.bodySmall,
                )
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
