package com.thehouse.app.domain.push

import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fuente del token FCM (Firebase en producción, fake en tests). */
interface PushTokenProvider {
    suspend fun currentToken(): String?
}

sealed interface PushTokenState {
    /** Aún no se consultó Firebase. */
    data object Unknown : PushTokenState

    /** Firebase no configurado, sin token o error recuperable. */
    data object Unavailable : PushTokenState

    /** Token disponible. Solo se expone un fingerprint truncado, nunca el token. */
    data class Available(val fingerprint: String) : PushTokenState
}

object PushTokenFingerprint {
    private const val FINGERPRINT_LENGTH = 12

    fun of(token: String?): String? {
        if (token.isNullOrBlank()) return null
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(FINGERPRINT_LENGTH)
    }
}

/**
 * Obtiene y mantiene el token FCM en memoria.
 *
 * - No persiste el token en disco: no es necesario mientras no exista contrato
 *   Backend de registro (`FCM-TOKEN-BACKEND-BLOCKED`).
 * - `onNewToken` y `refresh` comparten la misma validación.
 * - Nunca loguea el token completo; el estado público solo expone fingerprint.
 */
class PushTokenCoordinator(
    private val provider: PushTokenProvider,
    private val logger: PushLogger,
) {

    @Volatile
    private var token: String? = null

    private val _state = MutableStateFlow<PushTokenState>(PushTokenState.Unknown)
    val state: StateFlow<PushTokenState> = _state.asStateFlow()

    suspend fun refresh() {
        val fetched = try {
            provider.currentToken()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            logger.warn("fcm_token_unavailable")
            null
        }
        applyToken(fetched, source = "refresh")
    }

    /** Llamado desde `FirebaseMessagingService.onNewToken`. */
    fun onNewToken(newToken: String) {
        applyToken(newToken, source = "on_new_token")
        onTokenChanged?.invoke()
    }

    /**
     * D21: callback invocado cuando el token cambia (incluido `onNewToken`).
     * El coordinador de devices lo usa para sincronizar el token rotado.
     */
    var onTokenChanged: (() -> Unit)? = null

    /** D21: limpia el token al cambiar de cuenta (aislamiento A " B). */
    @Synchronized
    fun clear() {
        token = null
        _state.value = PushTokenState.Unavailable
    }

    /**
     * Token disponible para una futura sincronización con Backend.
     * Hoy no se usa: no existe contrato real de registro de devices.
     */
    internal fun currentTokenForBackendSync(): String? = token

    @Synchronized
    private fun applyToken(rawToken: String?, source: String) {
        val validToken = validate(rawToken)
        if (validToken == null) {
            token = null
            _state.value = PushTokenState.Unavailable
            logger.info("fcm_token_${source}_unavailable")
            return
        }
        token = validToken
        _state.value = PushTokenState.Available(
            fingerprint = PushTokenFingerprint.of(validToken).orEmpty(),
        )
        // Solo evento controlado: sin token, sin prefijos ni contenido del payload.
        logger.info("fcm_token_${source}_updated")
    }

    private fun validate(rawToken: String?): String? {
        val candidate = rawToken?.trim().orEmpty()
        if (candidate.length !in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH) return null
        if (candidate.any { it.isWhitespace() || it.isISOControl() }) return null
        return candidate
    }

    private companion object {
        const val MIN_TOKEN_LENGTH = 16
        const val MAX_TOKEN_LENGTH = 4096
    }
}
