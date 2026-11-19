package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.landlord.AgreementErrorMessages
import com.thehouse.app.ui.theme.Spacing

@Composable
fun AgreementPreviewDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onNavigateToMyRental: () -> Unit,
) {
    val viewModel: AgreementPreviewViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AgreementPreviewEffect.NavigateToMyRental -> onNavigateToMyRental()
            }
        }
    }
    AgreementPreviewScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(AgreementPreviewEvent.Load) },
        onAcceptedTermsChanged = { viewModel.onEvent(AgreementPreviewEvent.AcceptedTermsChanged(it)) },
        onAccept = { viewModel.onEvent(AgreementPreviewEvent.Accept) },
    )
}

@Composable
fun AgreementPreviewScreen(
    state: AgreementPreviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAcceptedTermsChanged: (Boolean) -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Revisar acuerdo",
        subtitle = state.preview?.room?.title,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = "Cargando acuerdo…",
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorMessage != null && state.preview == null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.preview != null -> {
                    val preview = state.preview ?: return@Column

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = preview.room.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = preview.room.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            InfoRow("Edificio", preview.building.name)
                            InfoRow("Arrendador", preview.landlord.name)
                        }
                    }

                    preview.building.rules?.takeIf { it.isNotBlank() }?.let { rules ->
                        TheHouseCard {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    text = "Reglas",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = rules,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    TheHouseCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = "Condiciones",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            InfoRow(
                                "Renta mensual",
                                "${preview.terms.monthlyRent} ${preview.terms.currency}",
                            )
                            InfoRow(
                                "Depósito",
                                "${preview.terms.deposit} ${preview.terms.currency}",
                            )
                            InfoRow("Inicio", preview.terms.startDate)
                            InfoRow("Duración", "${preview.terms.durationMonths} meses")
                            InfoRow("Fin", preview.terms.endDate)
                            InfoRow("Día de pago", preview.terms.paymentDay.toString())
                        }
                    }

                    InlineMessage(
                        text = "Código expira: ${preview.codeExpiresAt}",
                        tone = StatusTone.Neutral,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = state.acceptedTerms,
                                role = Role.Checkbox,
                                onValueChange = onAcceptedTermsChanged,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Checkbox(
                            checked = state.acceptedTerms,
                            onCheckedChange = null,
                        )
                        Text(
                            text = "Acepto los términos del acuerdo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    state.retryAfterSeconds?.let { seconds ->
                        InlineMessage(
                            text = AgreementErrorMessages.retryHint(seconds)
                                ?: "Espera e intenta de nuevo.",
                            tone = StatusTone.Warning,
                        )
                    }

                    state.errorMessage?.let { message ->
                        InlineMessage(text = message)
                    }

                    TheHousePrimaryButton(
                        text = "Aceptar acuerdo",
                        onClick = onAccept,
                        enabled = state.acceptedTerms && state.retryAfterSeconds == null,
                        loading = state.isAccepting,
                    )
                }
            }
        }
    }
}
