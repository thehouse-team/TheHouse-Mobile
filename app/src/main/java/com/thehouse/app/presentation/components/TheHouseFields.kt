package com.thehouse.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.thehouse.app.ui.theme.IconSizes
import com.thehouse.app.ui.theme.Spacing

/**
 * Campo de texto TheHouse: radio 16, contenedor blanco, borde outlineVariant,
 * error y texto de apoyo consistentes. No contiene lógica de negocio.
 */
@Composable
fun TheHouseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val helper = when {
        isError && !errorMessage.isNullOrBlank() -> errorMessage
        !supportingText.isNullOrBlank() -> supportingText
        else -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = helper?.let { { Text(it) } },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Botón de ojo para alternar la visibilidad de contraseña (48dp de toque). */
@Composable
fun PasswordVisibilityButton(
    visible: Boolean,
    onToggle: () -> Unit,
    visibleIcon: ImageVector,
    hiddenIcon: ImageVector,
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.padding(end = Spacing.xs),
    ) {
        Icon(
            imageVector = if (visible) visibleIcon else hiddenIcon,
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
            modifier = Modifier.padding(2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Variante compacta para formularios largos. */
@Composable
fun TheHouseFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        TheHouseTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            errorMessage = errorMessage,
            supportingText = supportingText,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
        )
    }
}

/** Icono de campo con tamaño uniforme. */
@Composable
internal fun FieldIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSizes.md))
}
