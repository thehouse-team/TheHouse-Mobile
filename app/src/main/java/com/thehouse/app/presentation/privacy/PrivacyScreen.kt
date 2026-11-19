package com.thehouse.app.presentation.privacy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.privacy.PrivacyPolicyUrl
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Entrada de Privacidad desde el área de cuenta (Home).
 *
 * La ruta es pública: un invitado puede leer el estado del aviso. La acción de
 * eliminar cuenta solo aparece con sesión activa.
 */
@Composable
fun PrivacyDestination(
    factory: U2ViewModelFactory,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val privacyFactory = remember(factory) { factory.privacy() }
    val viewModel: PrivacyViewModel = viewModel(factory = privacyFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PrivacyEffect.OpenPrivacyPolicy -> {
                    val opened = openPrivacyPolicy(context, effect.url)
                    if (!opened) {
                        viewModel.onEvent(PrivacyEvent.PrivacyPolicyLaunchFailed)
                    }
                }
                PrivacyEffect.AccountDeleted -> onAccountDeleted()
                PrivacyEffect.RequireLogin -> onRequireLogin()
            }
        }
    }

    PrivacyScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    onBack: () -> Unit,
    onEvent: (PrivacyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Privacidad",
        subtitle = "Aviso legal y datos de tu cuenta",
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Consulta el aviso de privacidad y, si tienes una cuenta, " +
                    "las opciones sobre tus datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PrivacyPolicySection(state = state, onEvent = onEvent)

            if (state.isAuthenticated) {
                DeleteAccountSection(state = state, onEvent = onEvent)
            }

            state.info?.let { message ->
                InlineMessage(text = message, tone = StatusTone.Warning)
            }
            state.error?.let { message ->
                InlineMessage(text = message, tone = StatusTone.Error)
            }
        }
    }

    if (state.showDeleteConfirmation) {
        DeleteAccountDialog(state = state, onEvent = onEvent)
    }
}

@Composable
private fun PrivacyPolicySection(
    state: PrivacyUiState,
    onEvent: (PrivacyEvent) -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionTitle(
                title = "Aviso de privacidad",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizes.lg),
                    )
                },
            )
            if (state.privacyPolicyUrl == null) {
                Text(
                    text = "El aviso de privacidad aún no está publicado. " +
                        "La app no abrirá una URL no válida ni un contenido alternativo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TheHouseSecondaryButton(
                    text = "Aviso no disponible",
                    onClick = { onEvent(PrivacyEvent.OpenPrivacyPolicyClicked) },
                    enabled = false,
                )
            } else {
                Text(
                    text = "El aviso se abre en tu navegador mediante una conexión HTTPS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TheHousePrimaryButton(
                    text = "Ver aviso de privacidad",
                    onClick = { onEvent(PrivacyEvent.OpenPrivacyPolicyClicked) },
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                )
            }
        }
    }
}

@Composable
private fun DeleteAccountSection(
    state: PrivacyUiState,
    onEvent: (PrivacyEvent) -> Unit,
) {
    TheHouseCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionTitle(
                title = "Eliminar cuenta",
                color = MaterialTheme.colorScheme.error,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(IconSizes.lg),
                    )
                },
            )
            Text(
                text = "Eliminar tu cuenta es una acción permanente e irreversible. " +
                    "Se cerrará tu sesión y se borrarán los datos locales de tu cuenta " +
                    "en este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.isDeletionAvailable) {
                InlineMessage(
                    text = "La eliminación de cuenta todavía no está disponible porque " +
                        "el servicio no ofrece esta operación. No se ha eliminado nada.",
                    tone = StatusTone.Warning,
                )
            }
            TheHouseSecondaryButton(
                text = "Eliminar cuenta",
                onClick = { onEvent(PrivacyEvent.DeleteAccountClicked) },
                enabled = state.isDeletionAvailable && !state.isDeleting,
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    state: PrivacyUiState,
    onEvent: (PrivacyEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isDeleting) onEvent(PrivacyEvent.CancelDeleteClicked)
        },
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "Esta acción es permanente e irreversible. Se cerrará tu " +
                        "sesión y se borrarán los datos locales de tu cuenta en " +
                        "este dispositivo. No podrás recuperar la cuenta.",
                )
                if (state.isDeleting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Eliminando cuenta…")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(PrivacyEvent.ConfirmDeleteClicked) },
                enabled = !state.isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Eliminar cuenta")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(PrivacyEvent.CancelDeleteClicked) },
                enabled = !state.isDeleting,
            ) {
                Text("Cancelar")
            }
        },
    )
}

/**
 * Abre exclusivamente URLs HTTPS validadas. Devuelve false si no hay navegador
 * o si Android rechaza el intent; el llamador informa sin crash.
 */
private fun openPrivacyPolicy(context: Context, url: String): Boolean {
    val safeUrl = PrivacyPolicyUrl.normalize(url) ?: return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, safeUrl.toUri()))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyScreenBlockedPreview() {
    com.thehouse.app.ui.theme.TheHouseTheme {
        PrivacyScreen(
            state = PrivacyUiState(
                privacyPolicyUrl = null,
                isAuthenticated = true,
                isDeletionAvailable = false,
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
