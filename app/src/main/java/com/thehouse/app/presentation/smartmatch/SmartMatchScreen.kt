package com.thehouse.app.presentation.smartmatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.components.MatchBadge
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.demo.PrototypeProperties

/**
 * Smart Match parcial D4 (sin ML/IA, sin backend).
 *
 * - Guest y Auth pueden abrirlo.
 * - Solo muestra el orden del prototipo con la etiqueta de demo;
 *   el cálculo real pertenece a NV-17 (Smart Match / ML / IA) y no se
 *   implementa aquí.
 */
@Composable
fun SmartMatchScreen(
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Smart Match",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título Smart Match" },
            )
            Text(
                text = "Te mostramos primero las que van contigo (demo del prototipo). El modelo ML/IA pertenece a NV-17.",
                style = MaterialTheme.typography.bodyMedium,
            )
            MatchBadge(matchPercent = 95)
            Text(
                text = "Hasta $3,500 · A pie < 10 min",
                style = MaterialTheme.typography.bodyMedium,
            )
            PrototypeProperties.featured.forEach { property ->
                PropertyCard(
                    property = property,
                    onFavoriteClick = onFavoriteClick,
                    onContactClick = onContactClick,
                )
            }
        }
    }
}
