package com.thehouse.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.thehouse.app.presentation.navigation.BottomTab

/**
 * Barra inferior principal del prototipo (D4, NV-05).
 * - 4 destinos: Inicio, Explorar, Smart Match, Favoritos.
 * - Material3 NavigationBar accesible (48+ dp, semantics, selected).
 * - Sin lógica de negocio: delega clics (sin duplicar destinos por
 *   `launchSingleTop` + `saveState`/`restoreState` en NavHost).
 */
@Composable
fun BottomNavigation(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomTab.values().forEach { tab ->
            NavigationBarItem(
                modifier = Modifier.semantics {
                    contentDescription = tab.label
                },
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = iconFor(tab),
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}

private fun iconFor(tab: BottomTab): ImageVector = when (tab) {
    BottomTab.Inicio -> Icons.Filled.Home
    BottomTab.Explorar -> Icons.Filled.Explore
    BottomTab.SmartMatch -> Icons.Filled.AutoAwesome
    BottomTab.Favoritos -> Icons.Filled.Favorite
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationPreview() {
    BottomNavigation(selectedTab = BottomTab.Inicio, onTabSelected = {})
}
