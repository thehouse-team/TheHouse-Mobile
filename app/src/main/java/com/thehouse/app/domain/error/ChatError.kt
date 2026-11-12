package com.thehouse.app.domain.error

/**
 * Códigos de negocio confirmados del contrato de conversaciones/mensajes.
 */
enum class ChatErrorCode {
    CONVERSATION_CLOSED,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): ChatErrorCode = when (value) {
            "conversation_closed" -> CONVERSATION_CLOSED
            else -> UNKNOWN
        }
    }
}

class ChatException(
    val domainError: DomainError,
    val code: ChatErrorCode = ChatErrorCode.UNKNOWN,
) : Exception()
