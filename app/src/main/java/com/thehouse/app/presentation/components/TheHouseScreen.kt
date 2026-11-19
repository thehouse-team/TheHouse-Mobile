package com.thehouse.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thehouse.app.ui.theme.Spacing

/**
 * Shell visual común de las pantallas de detalle y flujo.
 *
 * - Fondo de tema (nunca blanco puro).
 * - Barra superior unificada con volver accesible.
 * - Contenido con el gutter del Design System.
 *
 * No contiene lógica de negocio ni conoce ViewModels.
 */
@Composable
fun TheHouseScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TheHouseTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                actions = actions,
            )
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

/** Encabezado de contenido para pantallas sin barra superior de detalle. */
@Composable
fun TheHouseHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!subtitle.isNullOrBlank()) {
            androidx.compose.material3.Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
