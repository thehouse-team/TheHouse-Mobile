package com.thehouse.app.testutil

import com.thehouse.app.data.preferences.PreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** PreferencesStore en memoria para tests JVM (sin Android). */
class InMemoryPreferencesStore : PreferencesStore {
    private val values = MutableStateFlow<Map<String, Any>>(emptyMap())

    override fun observeString(key: String): Flow<String?> =
        values.map { it[key] as? String }

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        values.map { it[key] as? Boolean ?: defaultValue }

    override suspend fun putString(key: String, value: String) {
        values.value = values.value + (key to value)
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        values.value = values.value + (key to value)
    }

    override suspend fun remove(key: String) {
        values.value = values.value - key
    }

    override suspend fun clear() {
        values.value = emptyMap()
    }
}
