package com.thehouse.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.components.BrandMark
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Drawer global de cuenta (D15 + D19.1 rediseño visual).
 * Renderiza exclusivamente [DrawerItemCatalog] con jerarquía por secciones,
 * iconos Material 3, área táctil de 48dp y scroll funcional.
 */
@Composable
fun AppDrawerContent(
    isGuest: Boolean,
    isLoggingOut: Boolean,
    onCloseDrawer: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenConversations: () -> Unit,
    onOpenMyRental: () -> Unit,
    onOpenRentalCode: () -> Unit,
    onOpenLandlordDashboard: () -> Unit,
    onOpenLandlordRequests: () -> Unit,
    onOpenLandlordAgreements: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogout: () -> Unit,
    selectedRoute: String? = null,
) {
    val items = if (isGuest) DrawerItemCatalog.guestItems() else DrawerItemCatalog.authItems(isLoggingOut)
    var lastSection: DrawerItemCatalog.Section? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // Encabezado de marca azul TheHouse.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark()
        }
        Text(
            text = if (isGuest) "Explora TheHouse" else "Tu cuenta TheHouse",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.sm),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        items.forEach { item ->
            if (lastSection != null && lastSection != item.section) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
            val sectionTitle = sectionTitle(item.section)
            if (item.section != lastSection && sectionTitle != null) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                )
            }
            lastSection = item.section
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                icon = {
                    Icon(
                        imageVector = iconFor(item.icon),
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.lg),
                    )
                },
                selected = selectedRoute != null &&
                    DrawerItemCatalog.routeByTag[item.tag] == selectedRoute,
                onClick = {
                    onCloseDrawer()
                    dispatch(
                        tag = item.tag,
                        isLoggingOut = isLoggingOut,
                        onRequireLogin = onRequireLogin,
                        onOpenProfile = onOpenProfile,
                        onOpenPreferences = onOpenPreferences,
                        onOpenQuickSetup = onOpenQuickSetup,
                        onOpenNotifications = onOpenNotifications,
                        onOpenRequests = onOpenRequests,
                        onOpenConversations = onOpenConversations,
                        onOpenMyRental = onOpenMyRental,
                        onOpenRentalCode = onOpenRentalCode,
                        onOpenLandlordDashboard = onOpenLandlordDashboard,
                        onOpenLandlordRequests = onOpenLandlordRequests,
                        onOpenLandlordAgreements = onOpenLandlordAgreements,
                        onOpenPrivacy = onOpenPrivacy,
                        onLogout = onLogout,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(item.tag),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

private fun sectionTitle(section: DrawerItemCatalog.Section): String? = when (section) {
    DrawerItemCatalog.Section.Account -> "Mi cuenta"
    DrawerItemCatalog.Section.Landlord -> "Arrendador"
    DrawerItemCatalog.Section.Session -> "Sesión y legal"
}

private fun iconFor(key: DrawerItemCatalog.IconKey): ImageVector = when (key) {
    DrawerItemCatalog.IconKey.Login -> Icons.Outlined.Person
    DrawerItemCatalog.IconKey.Privacy -> Icons.Outlined.PrivacyTip
    DrawerItemCatalog.IconKey.Profile -> Icons.Outlined.AccountCircle
    DrawerItemCatalog.IconKey.Preferences -> Icons.Outlined.Tune
    DrawerItemCatalog.IconKey.QuickSetup -> Icons.Outlined.SettingsSuggest
    DrawerItemCatalog.IconKey.Notifications -> Icons.Outlined.NotificationsNone
    DrawerItemCatalog.IconKey.Requests -> Icons.AutoMirrored.Outlined.Assignment
    DrawerItemCatalog.IconKey.Conversations -> Icons.AutoMirrored.Rounded.Chat
    DrawerItemCatalog.IconKey.Rental -> Icons.Outlined.Home
    DrawerItemCatalog.IconKey.Code -> Icons.Outlined.Key
    DrawerItemCatalog.IconKey.Landlord -> Icons.Outlined.Apartment
    DrawerItemCatalog.IconKey.LandlordRequests -> Icons.AutoMirrored.Outlined.Assignment
    DrawerItemCatalog.IconKey.Agreements -> Icons.Outlined.Description
    DrawerItemCatalog.IconKey.Logout -> Icons.AutoMirrored.Outlined.Logout
}

private fun dispatch(
    tag: String,
    isLoggingOut: Boolean,
    onRequireLogin: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenQuickSetup: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenConversations: () -> Unit,
    onOpenMyRental: () -> Unit,
    onOpenRentalCode: () -> Unit,
    onOpenLandlordDashboard: () -> Unit,
    onOpenLandlordRequests: () -> Unit,
    onOpenLandlordAgreements: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogout: () -> Unit,
) {
    when (DrawerNavigationCatalog.actionFor(tag)) {
        DrawerAction.REQUIRE_LOGIN -> onRequireLogin()
        DrawerAction.PROFILE -> onOpenProfile()
        DrawerAction.PREFERENCES -> onOpenPreferences()
        DrawerAction.QUICK_SETUP -> onOpenQuickSetup()
        DrawerAction.NOTIFICATIONS -> onOpenNotifications()
        DrawerAction.REQUESTS -> onOpenRequests()
        DrawerAction.CONVERSATIONS -> onOpenConversations()
        DrawerAction.MY_RENTAL -> onOpenMyRental()
        DrawerAction.RENTAL_CODE -> onOpenRentalCode()
        DrawerAction.LANDLORD_DASHBOARD -> onOpenLandlordDashboard()
        DrawerAction.LANDLORD_REQUESTS -> onOpenLandlordRequests()
        DrawerAction.LANDLORD_AGREEMENTS -> onOpenLandlordAgreements()
        DrawerAction.PRIVACY -> onOpenPrivacy()
        DrawerAction.LOGOUT -> if (!isLoggingOut) onLogout()
        null -> Unit
    }
}
