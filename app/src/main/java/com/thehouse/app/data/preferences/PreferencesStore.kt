package com.thehouse.app.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Boundary de preferencias no sensibles.
 *
 * La UI no consume esta interfaz directamente. Los repositorios de Data la
 * encapsulan cuando exista una necesidad funcional aprobada.
 */
interface PreferencesStore {
    fun observeString(key: String): Flow<String?>
    fun observeBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>
    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun remove(key: String)
    suspend fun clear()
}
