package com.thehouse.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.demo.PrototypeProperty
import com.thehouse.app.presentation.demo.PrototypeProperties

/**
 * Tarjeta de renta del prototipo (D4, NV-05).
 * - Reutilizable: muestra universidad/sector, renta destacada, precio,
 *   distancia al campus, indicador Match y verificado.
 * - Acciones de contacto/favorito son eventos de UI (sin tocar backend).
 * - Datos de prototipo se etiquetan como demo, nunca como backend.
 */
@Composable
fun PropertyCard(
    property: PrototypeProperty,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Renta ${property.title}" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MatchBadge(matchPercent = property.matchPercent)
                if (property.verified) VerifiedBadge()
            }
            Text(
                text = property.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = distanceText(property),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$" + property.priceMxn.toString() + " MXN/mes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = property.services.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (property.isDemo) {
                Text(
                    text = "Demo del prototipo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                ) {
                    Text(if (isFavorite) "Quitar favorito" else "Guardar favorito")
                }
                OutlinedButton(
                    onClick = onContactClick,
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                ) {
                    Text("Contactar")
                }
            }
        }
    }
}

private fun distanceText(property: PrototypeProperty): String {
    val meters = property.distanceMeters
    return if (meters != null) {
        "A ${property.distanceMinutes} min de tu universidad ($meters m)"
    } else {
        "A ${property.distanceMinutes} min de tu universidad"
    }
}

@Preview(showBackground = true)
@Composable
private fun PropertyCardPreview() {
    MaterialTheme {
        PropertyCard(
            property = PrototypeProperties.featuredProperty,
            onFavoriteClick = {},
            onContactClick = {},
        )
    }
}
