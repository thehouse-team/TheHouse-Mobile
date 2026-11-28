package com.thehouse.app.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.domain.model.QuickQuestion
import com.thehouse.app.presentation.components.SectionHeader
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/** Composer de solicitud: Room Detail → Solicitar → quickQuestions/message → enviar. */
@Composable
fun CreateRequestDestination(
    roomId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onCreated: (com.thehouse.app.domain.model.RoomRequest) -> Unit,
    onOpenConversation: (String) -> Unit,
) {
    val viewModel: CreateRequestViewModel = viewModel(
        key = "create-request:$roomId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateRequestEffect.Created -> onCreated(effect.request)
                is CreateRequestEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }
    CreateRequestScreen(
        state = state,
        onBack = onBack,
        onToggleQuestion = { viewModel.onEvent(CreateRequestEvent.QuickQuestionToggled(it)) },
        onMessageChanged = { viewModel.onEvent(CreateRequestEvent.MessageChanged(it)) },
        onSend = { viewModel.onEvent(CreateRequestEvent.Send) },
    )
}

@Composable
fun CreateRequestScreen(
    state: CreateRequestUiState,
    onBack: () -> Unit,
    onToggleQuestion: (QuickQuestion) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Solicitar este cuarto",
        subtitle = "El arrendador recibirá tu solicitud",
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
            Text(
                text = "Selecciona preguntas o escribe un mensaje al arrendador.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TheHouseCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SectionHeader(
                        title = "Preguntas rápidas",
                        subtitle = "Toca las que te interesen",
                    )
                    QuickQuestionLabels.all().forEach { (question, label) ->
                        val checked = question in state.quickQuestions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Heights.button)
                                .toggleable(
                                    value = checked,
                                    role = Role.Checkbox,
                                    onValueChange = { onToggleQuestion(question) },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = label },
                            )
                        }
                    }
                }
            }

            TheHouseTextField(
                value = state.message,
                onValueChange = onMessageChanged,
                label = "Mensaje (opcional)",
                placeholder = "Cuéntale al arrendador qué necesitas…",
                supportingText = "${state.message.length}/500",
                singleLine = false,
                minLines = 3,
            )

            state.errorMessage?.let { message ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizes.md),
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            TheHousePrimaryButton(
                text = "Enviar solicitud",
                onClick = onSend,
                loading = state.isSending,
            )
        }
    }
}
