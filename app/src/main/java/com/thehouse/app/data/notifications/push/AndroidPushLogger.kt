package com.thehouse.app.data.notifications.push

import android.util.Log
import com.thehouse.app.domain.push.PushLogger

/**
 * Logger local de push. Solo acepta eventos/reason codes controlados.
 * Nunca debe recibir tokens, títulos, cuerpos, ids ni payloads completos.
 */
class AndroidPushLogger(private val tag: String = "TheHousePush") : PushLogger {

    override fun info(event: String) {
        Log.i(tag, "event=$event")
    }

    override fun warn(event: String, reason: String?) {
        if (reason.isNullOrBlank()) {
            Log.w(tag, "event=$event")
        } else {
            Log.w(tag, "event=$event reason=$reason")
        }
    }
}
