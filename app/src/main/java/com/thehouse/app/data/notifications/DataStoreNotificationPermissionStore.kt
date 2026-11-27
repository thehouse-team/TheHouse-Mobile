package com.thehouse.app.data.notifications

import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.domain.repository.NotificationPermissionStore
import kotlinx.coroutines.flow.first

/** Implementación con DataStore no sensible (solo booleano de decisión). */
class DataStoreNotificationPermissionStore(
    private val preferences: PreferencesStore,
) : NotificationPermissionStore {

    override suspend fun wasAsked(): Boolean = preferences.observeBoolean(KEY, false).first()

    override suspend fun markAsked() {
        preferences.putBoolean(KEY, true)
    }

    private companion object {
        const val KEY = "notification_permission_asked"
    }
}
