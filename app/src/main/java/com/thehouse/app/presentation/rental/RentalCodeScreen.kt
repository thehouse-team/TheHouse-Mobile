package com.thehouse.app.presentation.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.presentation.components.ErrorStateView
import com.thehouse.app.presentation.components.LoadingView
import com.thehouse.app.presentation.landlord.AgreementErrorMessages

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
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
            Text("Introducir código de renta", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Ingresa el código de respaldo (ej. k7pm-2qxa).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChanged,
                label = { Text("Código") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.retryAfterSeconds?.let { sec ->
                Text(
                    AgreementErrorMessages.retryHint(sec) ?: "Espera e intenta de nuevo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onSubmit,
                enabled = !state.isLoading && state.retryAfterSeconds == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                else Text("Continuar")
            }
        }
    }
}
