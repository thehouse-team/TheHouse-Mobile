package com.thehouse.app.presentation.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.AgreementStatus
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.InfoRow
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.components.StatusChip
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseSecondaryButton
import com.thehouse.app.presentation.components.TheHouseTextAction
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Spacing

@Composable
fun LandlordAgreementDetailDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
) {
    val viewModel: LandlordAgreementDetailViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LandlordAgreementDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onEvent(LandlordAgreementDetailEvent.Load) },
        onIssueCode = { viewModel.onEvent(LandlordAgreementDetailEvent.IssueCode) },
        onCancel = { viewModel.onEvent(LandlordAgreementDetailEvent.Cancel) },
        onEnd = { reason, note ->
            viewModel.onEvent(LandlordAgreementDetailEvent.End(reason, note))
        },
    )
}

@Composable
fun LandlordAgreementDetailScreen(
    state: LandlordAgreementDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onIssueCode: () -> Unit,
    onCancel: () -> Unit,
    onEnd: (String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var endReason by remember { mutableStateOf<String?>(null) }
    var endNote by remember { mutableStateOf("") }
    var showEnd by remember { mutableStateOf(false) }

    TheHouseScreen(
        title = "Detalle de acuerdo",
        subtitle = state.agreement?.room?.title,
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
                state.errorMessage != null && state.agreement == null -> ErrorStateView(
                    title = "No se pudo cargar",
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
                state.agreement != null -> {
                    val agreement = state.agreement ?: return@Column

                    TheHouseCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Text(
                                    text = agreement.room.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = agreement.room.city,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusChip(
                                label = agreementStatusLabel(agreement.status),
                                tone = agreementStatusTone(agreement.status),
                            )
                        }
                        agreement.student?.let { student ->
                            student.name?.let { name -> InfoRow("Estudiante", name) }
                            student.university?.let { university ->
                                InfoRow("Universidad", university)
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
                                "Renta",
                                "${agreement.terms.monthlyRent} ${agreement.terms.currency}",
                            )
                            InfoRow(
                                "Depósito",
                                "${agreement.terms.deposit} ${agreement.terms.currency}",
                            )
                            InfoRow("Inicio", agreement.terms.startDate)
                            InfoRow("Fin", agreement.terms.endDate)
                            InfoRow("Día de pago", agreement.terms.paymentDay.toString())
                            InfoRow("Creado", agreement.createdAt ?: "-")
                        }
                    }

                    val closingCode = agreement.closingCode
                    if (closingCode != null) {
                        TheHouseCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    text = "Código de respaldo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = closingCode.backupCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Expira: ${closingCode.expiresAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    } else {
                        InlineMessage(
                            text = "Código: no disponible en el detalle. Genera uno nuevo si es necesario.",
                            tone = StatusTone.Neutral,
                        )
                    }

                    state.errorMessage?.let { message ->
                        InlineMessage(text = message)
                    }

                    if (agreement.status == AgreementStatus.PENDING) {
                        TheHousePrimaryButton(
                            text = if (state.isBusy) "Generando…" else "Generar nuevo código",
                            onClick = onIssueCode,
                            enabled = !state.isBusy,
                            loading = state.isBusy,
                        )
                        TheHouseSecondaryButton(
                            text = "Cancelar acuerdo",
                            onClick = onCancel,
                            enabled = !state.isBusy,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (agreement.status == AgreementStatus.ACTIVE) {
                        if (!showEnd) {
                            TheHouseSecondaryButton(
                                text = "Finalizar acuerdo",
                                onClick = { showEnd = true },
                                enabled = !state.isBusy,
                                contentColor = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            TheHouseCard {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                    Text(
                                        text = "Finalizar acuerdo",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Motivo (opcional)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                        listOf(
                                            "contract_finished" to "Contrato cumplido",
                                            "early_termination" to "Terminación anticipada",
                                            "other" to "Otro",
                                        ).forEach { (reason, label) ->
                                            FilterChip(
                                                selected = endReason == reason,
                                                onClick = { endReason = reason },
                                                label = { Text(label) },
                                            )
                                        }
                                    }
                                    TheHouseTextField(
                                        value = endNote,
                                        onValueChange = { endNote = it.take(300) },
                                        label = "Nota (opcional, máx 300)",
                                        supportingText = "${endNote.length}/300",
                                        singleLine = false,
                                        minLines = 2,
                                    )
                                    TheHousePrimaryButton(
                                        text = "Confirmar finalización",
                                        onClick = {
                                            onEnd(endReason, endNote)
                                            showEnd = false
                                        },
                                        enabled = !state.isBusy,
                                        loading = state.isBusy,
                                    )
                                    TheHouseTextAction(
                                        text = "Cancelar",
                                        onClick = { showEnd = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
