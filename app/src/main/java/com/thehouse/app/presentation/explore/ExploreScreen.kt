package com.thehouse.app.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.components.PropertyCard
import com.thehouse.app.presentation.demo.PrototypeProperties

/**
 * Explorar parcial D4 (integración parcial NV-07, sin mapa completo).
 *
 * - Guest y Auth pueden abrir Explorar (sin backend).
 * - Muestra las tarjetas del prototipo con la etiqueta "Demo del prototipo".
 * - Acciones protegidas delegan al callback [onRequireLogin] cuando
 *   el llamador (Home/Shell) detecta Guest; esta pantalla no decide auth.
 * - Mapa completo, SDK y permisos de ubicación quedan fuera de D4 (NV-07/NV-21).
 */
@Composable
fun ExploreScreen(
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
                text = "Explorar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título Explorar" },
            )
            Text(
                text = "Rentas cerca de UTTT (demo del prototipo). Mapa completo pendiente de NV-07.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PrototypeProperties.featured.forEach { property ->
                PropertyCard(
                    property = property,
                    onFavoriteClick = onFavoriteClick,
                    onContactClick = onContactClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
