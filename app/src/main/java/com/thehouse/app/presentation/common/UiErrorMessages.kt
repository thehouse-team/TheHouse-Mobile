package com.thehouse.app.presentation.common

/**
 * Mensajes de error seguros en español para las pantallas de la fase U2.
 *
 * Problem Details no expone raw HTTP: la UI nunca ve códigos ni excepciones.
 */
object UiErrorMessages {

    fun map(error: UiError): String = when (error) {
        UiError.Network -> "No hay conexión. Verifica tu red e intenta de nuevo."
        UiError.Timeout -> "La conexión tardó demasiado. Intenta de nuevo."
        UiError.BadRequest -> "Los filtros no son válidos. Ajusta la búsqueda e intenta de nuevo."
        UiError.Unauthorized -> "Credenciales inválidas. Revisa tu correo y contraseña."
        UiError.NotFound -> "No se encontró lo solicitado."
        UiError.Conflict -> "Ese correo ya está registrado. Inicia sesión en lugar de crear una cuenta nueva."
        UiError.RateLimited -> "Demasiados intentos. Espera un momento e intenta de nuevo."
        UiError.Server -> "El servicio no está disponible ahora. Intenta más tarde."
        UiError.Unknown -> "Ocurrió un error inesperado. Intenta de nuevo."
    }
}
