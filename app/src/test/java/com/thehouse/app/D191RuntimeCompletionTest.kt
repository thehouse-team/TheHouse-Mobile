package com.thehouse.app

import com.thehouse.app.domain.model.LegalInfo
import com.thehouse.app.presentation.navigation.DrawerItemCatalog
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D19.1 — regresiones de causas verificadas en el cierre runtime.
 * Sin tests genéricos: cada caso cubre un fallo reproducible.
 */
class D191RuntimeCompletionTest {

    @Test
    fun `legal con privacyUrl null no afirma que existe contenido en la app`() {
        val info = LegalInfo(
            termsVersion = "2026-11",
            privacyVersion = "2026-11",
            landlordTermsVersion = null,
            termsUrl = null,
            privacyUrl = null,
            accountDeletionUrl = null,
            updatedAt = null,
        )
        assertEquals("El aviso de privacidad aún no ha sido publicado.", info.privacyStatus)
        assertFalse(info.privacyStatus.contains("disponible dentro de la aplicaci"))
    }

    @Test
    fun `legal con privacyUrl https permite abrir el navegador`() {
        val info = LegalInfo(
            termsVersion = "2026-11",
            privacyVersion = "2026-11",
            landlordTermsVersion = null,
            termsUrl = null,
            privacyUrl = "https://example.com/privacy",
            accountDeletionUrl = null,
            updatedAt = null,
        )
        assertEquals("Puedes abrir el aviso de privacidad en tu navegador.", info.privacyStatus)
    }

    @Test
    fun `drawer mantiene aviso de privacidad en guest y auth`() {
        assertTrue(DrawerItemCatalog.guestItems().any { it.tag == "drawer_privacy" })
        assertTrue(DrawerItemCatalog.authItems(false).any { it.tag == "drawer_privacy" })
    }

    @Test
    fun `drawer de invitado no expone rutas protegidas`() {
        val guestTags = DrawerItemCatalog.guestItems().map { it.tag }
        assertFalse(guestTags.contains("drawer_profile"))
        assertFalse(guestTags.contains("drawer_preferences"))
    }

    @Test
    fun `factory sigue soportando LandlordProfileViewModel tras el merge`() {
        val candidates = listOf(
            File("src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
            File("app/src/main/java/com/thehouse/app/presentation/U2ViewModelFactory.kt"),
        )
        val source = candidates.firstOrNull { it.exists() }?.readText()
            ?: throw AssertionError("No se encontró U2ViewModelFactory.kt")
        val createBody = source.substringAfter("override fun <T : ViewModel> create")
        assertTrue(
            createBody.contains(
                "modelClass.isAssignableFrom(" +
                    "com.thehouse.app.presentation.landlord.LandlordProfileViewModel::class.java)",
            ),
        )
        assertTrue(createBody.contains("activateLandlord = container.activateLandlord"))
    }
}
