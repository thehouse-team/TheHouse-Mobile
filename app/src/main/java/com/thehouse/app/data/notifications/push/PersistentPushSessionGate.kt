package com.thehouse.app.data.notifications.push

import com.thehouse.app.data.auth.session.InMemorySessionStore
import com.thehouse.app.domain.model.SessionState
import com.thehouse.app.domain.push.PushSessionGate
import com.thehouse.app.domain.usecase.RestoreSessionUseCase

/**
 * Reutiliza la restauración de sesión persistente existente.
 *
 * Si el proceso murió y FCM despierta la app, el refresh cifrado permite
 * resolver la sesión antes de mostrar contenido privado.
 */
class PersistentPushSessionGate(
    private val sessionStore: InMemorySessionStore,
    private val restoreSession: RestoreSessionUseCase,
) : PushSessionGate {

    override suspend fun hasActiveSession(): Boolean {
        if (sessionStore.state.value is SessionState.Active) return true
        runCatching { restoreSession() }
        return sessionStore.state.value is SessionState.Active
    }
}
