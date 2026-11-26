package com.thehouse.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.thehouse.app.ui.theme.Spacing

/** Indicador de compatibilidad del prototipo (D4, NV-05). */
@Composable
fun MatchBadge(
    matchPercent: Int,
    modifier: Modifier = Modifier,
) {
    val label = "$matchPercent% Match"
    Surface(
        modifier = modifier.semantics {
            contentDescription = "Compatibilidad $label"
        },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchBadgePreview() {
    MaterialTheme { MatchBadge(matchPercent = 95) }
}
