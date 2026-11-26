package com.thehouse.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.thehouse.app.ui.theme.Spacing

/**
 * Drawer global de cuenta (D15). Renderiza exclusivamente [DrawerItemCatalog].
 *
 * Todas las acciones cierran el drawer antes de navegar. Cada tag del catálogo
 * se despacha a una ruta real o al evento de logout real; no hay callbacks
 * vacíos ni pantallas dummy.
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
) {
    val items = if (isGuest) DrawerItemCatalog.guestItems() else DrawerItemCatalog.authItems(isLoggingOut)
    var lastSection: DrawerItemCatalog.Section? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = "Menú",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        items.forEach { item ->
            if (lastSection != null && lastSection != item.section) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            }
            if (item.section != lastSection && item.section == DrawerItemCatalog.Section.Landlord) {
                Text(
                    text = "Arrendador",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
            lastSection = item.section
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = false,
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
                modifier = Modifier.fillMaxWidth().testTag(item.tag),
            )
        }
    }
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
    when (tag) {
        "drawer_login" -> onRequireLogin()
        "drawer_profile" -> onOpenProfile()
        "drawer_preferences" -> onOpenPreferences()
        "drawer_quick_setup" -> onOpenQuickSetup()
        "drawer_notifications" -> onOpenNotifications()
        "drawer_requests" -> onOpenRequests()
        "drawer_conversations" -> onOpenConversations()
        "drawer_my_rental" -> onOpenMyRental()
        "drawer_rental_code" -> onOpenRentalCode()
        "drawer_landlord_dashboard" -> onOpenLandlordDashboard()
        "drawer_landlord_requests" -> onOpenLandlordRequests()
        "drawer_agreements" -> onOpenLandlordAgreements()
        "drawer_privacy" -> onOpenPrivacy()
        "drawer_logout" -> if (!isLoggingOut) onLogout()
    }
}
