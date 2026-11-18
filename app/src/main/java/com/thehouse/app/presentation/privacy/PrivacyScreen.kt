package com.thehouse.app.presentation.privacy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.thehouse.app.ui.theme.Heights
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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = Heights.button),
            ) {
                Text("Volver")
            }

            Text(
                text = "Privacidad",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Consulta el aviso de privacidad y, si tienes una cuenta, " +
                    "las opciones sobre tus datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            PrivacyPolicySection(state = state, onEvent = onEvent)

            if (state.isAuthenticated) {
                HorizontalDivider()
                DeleteAccountSection(state = state, onEvent = onEvent)
            }

            state.info?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
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
    Text(
        text = "Aviso de privacidad",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    if (state.privacyPolicyUrl == null) {
        Text(
            text = "El aviso de privacidad aún no está publicado. " +
                "La app no abrirá una URL no válida ni un contenido alternativo.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = { onEvent(PrivacyEvent.OpenPrivacyPolicyClicked) },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Heights.button),
        ) {
            Text("Aviso no disponible")
        }
    } else {
        Text(
            text = "El aviso se abre en tu navegador mediante una conexión HTTPS.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { onEvent(PrivacyEvent.OpenPrivacyPolicyClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Heights.button),
        ) {
            Text("Ver aviso de privacidad")
        }
    }
}

@Composable
private fun DeleteAccountSection(
    state: PrivacyUiState,
    onEvent: (PrivacyEvent) -> Unit,
) {
    Text(
        text = "Eliminar cuenta",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = "Eliminar tu cuenta es una acción permanente e irreversible. " +
            "Se cerrará tu sesión y se borrarán los datos locales de tu cuenta " +
            "en este dispositivo.",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!state.isDeletionAvailable) {
        Text(
            text = "La eliminación de cuenta todavía no está disponible porque el " +
                "servicio no ofrece esta operación. No se ha eliminado nada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    OutlinedButton(
        onClick = { onEvent(PrivacyEvent.DeleteAccountClicked) },
        enabled = state.isDeletionAvailable && !state.isDeleting,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Heights.button),
    ) {
        Text("Eliminar cuenta")
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
