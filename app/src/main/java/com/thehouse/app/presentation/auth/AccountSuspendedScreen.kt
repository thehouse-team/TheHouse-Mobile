package com.thehouse.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing
import com.thehouse.app.ui.theme.TheHouseTheme

/** Sin acciones protegidas ni logout implícito. La identidad se conserva en Session. */
@Composable
fun AccountSuspendedScreen(
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg)
            .testTag("account_suspended")
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
    ) {
        Text(
            "Tu cuenta está suspendida.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "No puedes continuar con acciones protegidas. No se ha cerrado tu sesión automáticamente.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth().heightIn(min = Heights.button),
        ) {
            Text(if (isLoggingOut) "Cerrando sesión…" else "Cerrar sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSuspendedPreview() {
    TheHouseTheme { AccountSuspendedScreen(false, {}) }
}
