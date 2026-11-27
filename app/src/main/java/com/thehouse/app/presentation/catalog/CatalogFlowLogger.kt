package com.thehouse.app.presentation.catalog

import android.util.Log
import com.thehouse.app.BuildConfig

/**
 * Logs DEBUG dirigidos del catálogo (D20 hotfix). Sin credenciales, tokens,
 * datos personales ni cuerpos completos. Solo estado, conteos y tipos.
 *
 * Uso: `home("event_name", "clave", valor, "clave2", valor2)` — se formatea
 * como `clave=valor` en grupos de dos.
 */
object CatalogFlowLogger {
    private const val HOME = "HomeFlow"
    private const val EXPLORE = "ExploreFlow"
    private const val SEARCH = "RoomSearchFlow"

    private fun format(fields: Array<out Any?>): String = buildString {
        var i = 0
        while (i < fields.size) {
            val key = fields[i]
            val value = if (i + 1 < fields.size) fields[i + 1] else ""
            if (isNotEmpty()) append(' ')
            append(key).append('=').append(value)
            i += 2
        }
    }

    private fun log(tag: String, event: String, fields: Array<out Any?>) {
        if (!BuildConfig.DEBUG) return
        // Los tests JVM no tienen android.util.Log implementado; el logger es
        // diagnóstico DEBUG y nunca debe alterar el comportamiento.
        runCatching { Log.d(tag, "$event ${format(fields)}".trim()) }
    }

    fun home(event: String, vararg fields: Any?) = log(HOME, event, fields)
    fun explore(event: String, vararg fields: Any?) = log(EXPLORE, event, fields)
    fun search(event: String, vararg fields: Any?) = log(SEARCH, event, fields)
}
