package com.thehouse.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens de espaciado y dimensiones del prototipo TheHouse.
 * gutter 16dp, space-xs/sm/md/lg/xl = 4/8/16/24/32dp.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val gutter: Dp = 16.dp
}

/** Alturas normalizadas de botones e inputs. */
object Heights {
    val button: Dp = 48.dp
    val buttonLarge: Dp = 56.dp
    val input: Dp = 56.dp
}

/** Elevaciones normalizadas de cards. */
object Elevations {
    val card: Dp = 2.dp
    val raised: Dp = 4.dp
}
