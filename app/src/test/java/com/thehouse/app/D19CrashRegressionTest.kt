package com.thehouse.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * D19 hotfix — regresión del FATAL EXCEPTION real capturado en dropbox:
 *   java.lang.IllegalArgumentException: ViewModel no soportado:
 *   com.thehouse.app.presentation.landlord.LandlordProfileViewModel
 *   at U2ViewModelFactory.create(...)
 *   at LandlordDashboardDestination (vista "notLandlord")
 *
 * Antes del fix, `U2ViewModelFactory.create()` no tenía rama para
 * LandlordProfileViewModel y lanzaba IllegalArgumentException al abrir
 * "Mis alojamientos" con una cuenta sin perfil de arrendador.
 */
class D19CrashRegressionTest {

    private fun factorySource(): String {
        val candidates = listOf(
            File("src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
            File("app/src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
            File(System.getProperty("user.dir"), "src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
            File(System.getProperty("user.dir"), "app/src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
        )
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: throw AssertionError("No se encontró U2ViewModelFactory.kt para la prueba de regresión")
    }

    @Test
    fun `factory soporta LandlordProfileViewModel para evitar el crash de dashboard`() {
        val source = factorySource()
        val createBody = source.substringAfter("override fun <T : ViewModel> create")
        assertTrue(
            "U2ViewModelFactory.create debe manejar LandlordProfileViewModel " +
                "(dropbox data_app_crash 2026-11-26 18:01:49)",
            createBody.contains(
                "modelClass.isAssignableFrom(" +
                    "com.thehouse.app.presentation.landlord.LandlordProfileViewModel::class.java)",
            ),
        )
        assertTrue(
            "El ViewModel debe construirse con container.activateLandlord",
            createBody.contains("activateLandlord = container.activateLandlord"),
        )
    }
}
