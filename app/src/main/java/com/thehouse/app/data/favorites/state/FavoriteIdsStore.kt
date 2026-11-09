package com.thehouse.app.data.favorites.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado global ligero de ids de favoritos (D7.1).
 * Backend sigue siendo fuente de verdad; se actualiza tras éxito.
 */
class FavoriteIdsStore {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun setAll(ids: Set<String>) {
        _ids.value = ids
    }

    fun add(id: String) {
        _ids.value = _ids.value + id
    }

    fun remove(id: String) {
        _ids.value = _ids.value - id
    }

    fun contains(id: String): Boolean = id in _ids.value
}