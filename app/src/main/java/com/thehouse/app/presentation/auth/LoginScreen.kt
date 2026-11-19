package com.thehouse.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thehouse.app.R
import com.thehouse.app.presentation.U2ViewModelFactory
import com.thehouse.app.presentation.components.BrandMark
import com.thehouse.app.presentation.components.PasswordVisibilityButton
import com.thehouse.app.presentation.components.TheHouseCard
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.presentation.components.TheHouseTextField
import com.thehouse.app.ui.theme.Heights
import com.thehouse.app.ui.theme.Spacing

/**
 * Pantalla de Login (RF-02), MVVM + UDF: recibe estado y emite eventos.
 * Fidelidad visual a `02-iniciar-sesion.html` con el Design System TheHouse.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    factory: U2ViewModelFactory,
) {
    val viewModel: LoginViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateAfterLogin -> onLoggedIn()
                is LoginEffect.RegisterCompleted -> Unit
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xxl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))
            BrandMark()
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TheHouseCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    TheHouseTextField(
                        value = state.email,
                        onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                        label = stringResource(R.string.field_email),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Campo de correo electrónico"
                        },
                    )

                    TheHouseTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        label = stringResource(R.string.field_password),
                        visualTransformation = if (state.passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            PasswordVisibilityButton(
                                visible = state.passwordVisible,
                                onToggle = {
                                    viewModel.onEvent(LoginEvent.PasswordVisibilityToggled)
                                },
                                visibleIcon = Icons.Outlined.Visibility,
                                hiddenIcon = Icons.Outlined.VisibilityOff,
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Campo de contraseña"
                        },
                    )

                    state.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Error: $message" },
                        )
                    }

                    TheHousePrimaryButton(
                        text = stringResource(R.string.action_login),
                        onClick = { viewModel.onEvent(LoginEvent.LoginClicked) },
                        enabled = !state.isLoading,
                        loading = state.isLoading,
                    )
                }
            }

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Heights.button),
            ) {
                Text(
                    text = stringResource(R.string.login_go_register),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}
