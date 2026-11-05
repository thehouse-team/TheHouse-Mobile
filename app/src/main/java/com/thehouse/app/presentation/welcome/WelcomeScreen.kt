package com.thehouse.app.presentation.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.navigation.AppRoutes

/**
 * Bienvenida personalizada tras el onboarding.
 *
 * U2: el nombre disponible proviene de la respuesta real de login/register.
 * La identificación remota (GET /api/v1/users/me) NO está confirmada:
 * BLOCKED-BACKEND. Si el backend no entregó nombre se usa un saludo genérico.
 */
@Composable
fun WelcomeScreen(
    displayName: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = displayName?.takeIf { it.isNotBlank() }?.let { "¡Hola, $it!" }
        ?: "¡Bienvenido a TheHouse!"
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Tu nuevo espacio de vivienda está a un paso.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(top = 8.dp),
            ) {
                Text("Continuar")
            }
        }
    }
}
