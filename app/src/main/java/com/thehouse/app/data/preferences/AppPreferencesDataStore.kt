package com.thehouse.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_NAME = "thehouse_preferences"

/**
 * DataStore de preferencias no sensibles.
 *
 * No debe almacenar tokens, credenciales, API keys ni datos personales sensibles.
 */
val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
)
