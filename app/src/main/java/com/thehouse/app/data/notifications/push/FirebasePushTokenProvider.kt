package com.thehouse.app.data.notifications.push

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.thehouse.app.BuildConfig
import com.thehouse.app.domain.push.PushLogger
import com.thehouse.app.domain.push.PushTokenProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Token FCM oficial. Si el proyecto Firebase no está configurado
 * (`google-services.json` ausente) devuelve null sin romper la app.
 */
class FirebasePushTokenProvider(
    private val context: Context,
    private val logger: PushLogger,
) : PushTokenProvider {

    @Suppress("DEPRECATION")
    override suspend fun currentToken(): String? {
        if (!BuildConfig.FCM_CONFIGURED) {
            logger.info("fcm_not_configured")
            return null
        }
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                logger.info("fcm_not_initialized")
                return null
            }
            withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
                FirebaseMessaging.getInstance().token.awaitResult()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            logger.warn("fcm_token_unavailable")
            null
        }
    }

    private suspend fun Task<String>.awaitResult(): String? =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }

    private companion object {
        const val TOKEN_TIMEOUT_MS = 10_000L
    }
}
