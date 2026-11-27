package com.thehouse.app.data.device

import com.thehouse.app.data.preferences.PreferencesStore
import com.thehouse.app.domain.device.InstallationId
import com.thehouse.app.domain.repository.InstallationIdStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [InstallationIdStore] sobre DataStore de preferencias no sensibles.
 *
 * La escritura está protegida por un Mutex para que dos corrutinas concurrentes
 * (por ejemplo login y restauración de sesión) no generen dos GUID distintos.
 */
class DataStoreInstallationIdStore(
    private val preferences: PreferencesStore,
) : InstallationIdStore {

    private val writeMutex = Mutex()

    override suspend fun getOrCreate(): InstallationId = writeMutex.withLock {
        val stored = preferences.observeString(KEY).first()
        if (InstallationId.isValid(stored)) {
            return@withLock InstallationId(stored!!)
        }
        val created = InstallationId.generate()
        preferences.putString(KEY, created.value)
        created
    }

    private companion object {
        // Preferencia no sensible: no contiene credenciales ni datos personales.
        const val KEY = "device_installation_id"
    }
}
