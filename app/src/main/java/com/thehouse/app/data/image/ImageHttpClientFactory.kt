package com.thehouse.app.data.image

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * D21 hotfix: cliente HTTP **dedicado a imágenes**.
 *
 * El cliente de Retrofit inyecta `Authorization: Bearer` y ejecuta el
 * authenticator de refresh. Compartirlo con Coil enviaba credenciales a
 * cualquier dominio de una URL de fotografía y podía disparar refreshes por
 * un 401 de una imagen. Las fotografías son recursos públicos: este cliente
 * no tiene interceptor de autorización ni authenticator y usa timeouts
 * acotados para no bloquear la UI.
 */
object ImageHttpClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}
