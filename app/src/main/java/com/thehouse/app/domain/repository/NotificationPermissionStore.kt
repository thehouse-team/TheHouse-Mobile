package com.thehouse.app.domain.repository

/**
 * Persistencia de la decisión de permiso de notificaciones.
 *
 * Causa raíz corregida: `askedOnce` era rememberSaveable, por lo que al morir
 * el proceso se perdía y una denegación permanente parecía una primera negación;
 * Android ya no muestra el diálogo y el usuario tenía que ir a Ajustes manualmente.
 */
interface NotificationPermissionStore {
    suspend fun wasAsked(): Boolean
    suspend fun markAsked()
}
