package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.InlineMessage
import com.thehouse.app.presentation.components.StatusTone
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseScreen
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.presentation.landlord.AgreementErrorMessages
import com.thehouse.app.ui.theme.Spacing

@Composable
fun RentalCodeDestination(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
) {
    val viewModel: RentalCodeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RentalCodeEffect.NavigateToPreview -> onNavigateToPreview(effect.code)
            }
        }
    }
    RentalCodeScreen(
        state = state,
        onBack = onBack,
        onCodeChanged = { viewModel.onEvent(RentalCodeEvent.CodeChanged(it)) },
        onSubmit = { viewModel.onEvent(RentalCodeEvent.Submit) },
    )
}

@Composable
fun RentalCodeScreen(
    state: RentalCodeUiState,
    onBack: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TheHouseScreen(
        title = "Introducir código de renta",
        subtitle = "Vincula tu acuerdo de renta",
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
                text = "Ingresa el código de respaldo que te compartió el arrendador " +
                    "(ej. k7pm-2qxa).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TheHouseTextField(
                value = state.code,
                onValueChange = onCodeChanged,
                label = "Código",
                placeholder = "k7pm-2qxa",
            )

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
                text = "Continuar",
                onClick = onSubmit,
                enabled = state.retryAfterSeconds == null,
                loading = state.isLoading,
            )
        }
    }
}
