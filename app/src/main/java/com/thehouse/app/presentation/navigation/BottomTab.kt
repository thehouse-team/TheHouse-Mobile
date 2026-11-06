package com.thehouse.app.presentation.navigation

/**
 * Pestañas de la navegación principal (D4, prototipo 07-inicio).
 *
 * - Guest puede abrir Inicio, Explorar y Smart Match.
 * - Favoritos abre su tab pero muestra muro de acceso (requiere identidad).
 * - La definición vive en Presentation (solo UI); la política de acceso
 *   para acciones protegidas vive en [com.thehouse.app.domain.auth.AccessPolicy].
 *
 * Sin backend: Explorar/Smart Match/FAvoritos no consultan endpoints.
 */
enum class BottomTab(
    val route: String,
    val label: String,
    /** Guest puede ver el contenido completo de esta tab. */
    val guestCanViewContent: Boolean,
    /** Si es falso, Guest ve el muro de acceso con botón Iniciar sesión. */
    val requiresAuthForContent: Boolean,
) {
    Inicio(
        route = "inicio",
        label = "Inicio",
        guestCanViewContent = true,
        requiresAuthForContent = false,
    ),
    Explorar(
        route = "explorar",
        label = "Explorar",
        guestCanViewContent = true,
        requiresAuthForContent = false,
    ),
    SmartMatch(
        route = "smart_match",
        label = "Smart Match",
        guestCanViewContent = true,
        requiresAuthForContent = false,
    ),
    Favoritos(
        route = "favoritos",
        label = "Favoritos",
        guestCanViewContent = false,
        requiresAuthForContent = true,
    ),
}

/**
 * Estado mínimo de la bottom navigation (testeable sin Compose).
 *
 * - [selected] es la pestaña resaltada.
 * - [onTabSelected] actualiza el destino sin duplicar entradas (la UI
 *   usa `launchSingleTop` + `popUpTo` + `saveState`/`restoreState`).
 */
data class TabNavigationState(
    val selected: BottomTab = BottomTab.Inicio,
) {
    fun onTabSelected(tab: BottomTab): TabNavigationState = copy(selected = tab)

    companion object {
        fun fromRoute(route: String?): BottomTab =
            BottomTab.values().firstOrNull { it.route == route } ?: BottomTab.Inicio
    }
}
