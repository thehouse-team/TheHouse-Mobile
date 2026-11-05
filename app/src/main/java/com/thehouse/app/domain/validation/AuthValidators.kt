package com.thehouse.app.domain.validation

/**
 * Resultado de validación de dominio.
 *
 * Los mensajes son cadenas de texto en español listas para la UI; no se
 * traducen en Presentation para simplificar el alcance U2.
 */
data class ValidationResult(
    val errors: List<String> = emptyList(),
) {
    val isValid: Boolean get() = errors.isEmpty()

    fun asError(): String? = errors.firstOrNull()
}

/**
 * Validadores de entrada de auth (RF-01/RF-02).
 *
 * Sin dependencias de Android ni de red.
 */
object AuthValidators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MESSAGE_NAME_REQUIRED = "Ingresa tu nombre."
    const val MESSAGE_NAME_SHORT = "El nombre debe tener al menos 2 caracteres."
    const val MESSAGE_EMAIL_INVALID = "Ingresa un correo válido."
    const val MESSAGE_PASSWORD_REQUIRED = "Ingresa tu contraseña."
    const val MESSAGE_PASSWORD_SHORT = "La contraseña debe tener al menos 8 caracteres."
    const val MESSAGE_TERMS_REQUIRED = "Debes aceptar los términos."
    const val MESSAGE_PRIVACY_REQUIRED = "Debes aceptar el aviso de privacidad."

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> MESSAGE_EMAIL_INVALID
        !EMAIL_REGEX.matches(email.trim()) -> MESSAGE_EMAIL_INVALID
        else -> null
    }

    fun validatePassword(password: String, minimum: Boolean = true): String? = when {
        password.isEmpty() -> MESSAGE_PASSWORD_REQUIRED
        password.length < MIN_PASSWORD_LENGTH && minimum ->
            "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
        else -> null
    }

    fun validateName(name: String): String? = when {
        name.isBlank() -> MESSAGE_NAME_REQUIRED
        name.trim().length < 2 -> MESSAGE_NAME_SHORT
        else -> null
    }

    fun validateLogin(email: String, password: String): ValidationResult = ValidationResult(
        buildList {
            validateEmail(email)?.let { add(it) }
            if (password.isEmpty()) add(MESSAGE_PASSWORD_REQUIRED)
        },
    )

    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean,
        acceptedPrivacy: Boolean,
    ): ValidationResult = ValidationResult(
        buildList {
            validateName(name)?.let { add(it) }
            validateEmail(email)?.let { add(it) }
            validatePassword(password)?.let { add(it) }
            if (!acceptedTerms) add(MESSAGE_TERMS_REQUIRED)
            if (!acceptedPrivacy) add(MESSAGE_PRIVACY_REQUIRED)
        },
    )

    const val MIN_PASSWORD_LENGTH = 8
}
