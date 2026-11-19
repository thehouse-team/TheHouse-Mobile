package com.thehouse.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Sello de verificación. Reutiliza [StatusChip] para mantener un único
 * lenguaje visual en toda la app (propiedades, edificios y perfiles).
 */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    StatusChip(
        label = "Verificado",
        tone = StatusTone.Success,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun VerifiedBadgePreview() {
    MaterialTheme { VerifiedBadge() }
}
