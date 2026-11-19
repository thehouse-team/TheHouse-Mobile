package com.thehouse.app.presentation.common

import com.thehouse.app.domain.error.DomainException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Convierte errores de Data/Domain en un error seguro para Presentation.
 *
 * No expone mensajes crudos, códigos HTTP ni tipos de infraestructura.
 * Los errores HTTP no mapeados por Data caen en [UiError.Unknown] para no
 * filtrar detalles del backend.
 */
fun Throwable.toSafeUiError(): UiError = when (this) {
    is DomainException -> error.toUiError()
    is SocketTimeoutException -> UiError.Timeout
    is IOException -> UiError.Network
    else -> UiError.Unknown
}

/** Mensaje humano y seguro para mostrar en pantalla. */
fun Throwable.toSafeUiMessage(): String = UiErrorMessages.map(toSafeUiError())
