package com.thehouse.app.domain.repository

/**
 * Borrado de datos locales asociados a la cuenta eliminada.
 *
 * Debe limpiar solo lo asociado al usuario:
 * - sesión local y refresh token;
 * - marca de onboarding de ese usuario;
 * - ids de favoritos en memoria;
 * - target pendiente de notificación y notificaciones locales;
 * - caché HTTP privada.
 *
 * No debe borrar preferencias globales del dispositivo, imágenes públicas ni
 * datos de otros usuarios. La operación es best-effort: la sesión local se
 * limpia primero para que un reinicio no restaure la cuenta.
 */
interface LocalUserDataPurger {

    suspend fun purge(userId: String?)
}
