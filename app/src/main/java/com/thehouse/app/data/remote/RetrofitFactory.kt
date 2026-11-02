package com.thehouse.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fábrica base de Retrofit.
 *
 * No declara servicios ni endpoints funcionales. La URL base es un placeholder
 * no productivo hasta que una spec funcional autorice el servicio real.
 */
object RetrofitFactory {
    fun create(
        baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        require(baseUrl.startsWith("https://")) {
            "baseUrl must use HTTPS"
        }
        require(baseUrl.endsWith("/")) {
            "baseUrl must end with '/'"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
