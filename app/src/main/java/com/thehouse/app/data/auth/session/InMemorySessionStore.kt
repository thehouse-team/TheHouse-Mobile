package com.thehouse.app.data.auth.session

import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Store de sesión en memoria.
 *
 * El access token y el usuario activo viven AQUÍ y nunca se serializan ni se
 * loguean. Al morir el proceso el estado se pierde; la recuperación de sesión
 * se basa en el refresh token cifrado (ver [EncryptedRefreshTokenStorage]).
 */
class InMemorySessionStore {

    private val mutableState = MutableStateFlow<SessionState>(SessionState.Anonymous)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    /** Access token actual. Solo en memoria: null si no hay sesión o expiró. */
    @Volatile
    var accessToken: String? = null
        private set

    @Synchronized
    fun activate(session: UserSession, accessToken: String?) {
        // Una hidratación en vuelo no puede deshacer una suspensión confirmada.
        if (state.value is SessionState.Suspended) return
        this.accessToken = accessToken
        mutableState.value = SessionState.Active(session)
    }

    @Synchronized
    fun updateAccessToken(token: String?) {
        if (state.value is SessionState.Suspended) return
        accessToken = token
    }

    @Synchronized
    fun suspendAccount() {
        val identity = when (val current = state.value) {
            is SessionState.Active -> current.session
            is SessionState.Suspended -> current.session
            SessionState.Anonymous -> null
        }
        mutableState.value = SessionState.Suspended(identity)
    }

    /** Limpia por completo el estado de sesión, incluido el access token. */
    @Synchronized
    fun clear() {
        accessToken = null
        mutableState.value = SessionState.Anonymous
    }
}
