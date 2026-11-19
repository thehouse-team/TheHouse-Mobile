package com.thehouse.app.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thehouse.app.presentation.components.BrandMark
import com.thehouse.app.presentation.components.TheHousePrimaryButton
import com.thehouse.app.ui.theme.Spacing

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
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark()
            Box(
                modifier = Modifier
                    .padding(top = Spacing.xxl)
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Roofing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp),
                )
            }
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.lg),
            )
            Text(
                text = "Tu nuevo espacio de vivienda está a un paso.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TheHousePrimaryButton(
                text = "Continuar",
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxl),
            )
        }
    }
}
