package com.thehouse.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens de espaciado TheHouse (escala 4/8/12/16/20/24/32).
 * Toda pantalla nueva o rediseñada debe usar estos tokens en lugar de dp sueltos.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val gutter: Dp = 16.dp
}

/** Alturas normalizadas de controles. */
object Heights {
    val button: Dp = 48.dp
    val buttonLarge: Dp = 56.dp
    val input: Dp = 56.dp
    val topBar: Dp = 64.dp
    val iconButton: Dp = 48.dp
    val avatar: Dp = 44.dp
}

/** Tamaños de icono unificados. */
object IconSizes {
    val sm: Dp = 18.dp
    val md: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val hero: Dp = 40.dp
}

/** Elevaciones discretas; el diseño prioriza bordes suaves sobre sombras. */
object Elevations {
    val card: Dp = 1.dp
    val raised: Dp = 3.dp
    val hero: Dp = 6.dp
}

/** Radios semánticos del prototipo. */
object Radii {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val pill: Dp = 999.dp
}
