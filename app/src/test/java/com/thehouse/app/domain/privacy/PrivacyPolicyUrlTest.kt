package com.thehouse.app.domain.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * NV-15: la URL del aviso de privacidad solo se abre si es HTTPS absoluta,
 * con host y sin valores inventados.
 */
class PrivacyPolicyUrlTest {

    @Test
    fun `missing values normalize to null`() {
        assertNull(PrivacyPolicyUrl.normalize(null))
        assertNull(PrivacyPolicyUrl.normalize(""))
        assertNull(PrivacyPolicyUrl.normalize("   "))
    }

    @Test
    fun `non https schemes are rejected`() {
        assertNull(PrivacyPolicyUrl.normalize("http://example.com/privacidad"))
        assertNull(PrivacyPolicyUrl.normalize("ftp://example.com/privacidad"))
        assertNull(PrivacyPolicyUrl.normalize("javascript:alert(1)"))
        assertNull(PrivacyPolicyUrl.normalize("intent://example.com/privacidad"))
    }

    @Test
    fun `relative or malformed values are rejected`() {
        assertNull(PrivacyPolicyUrl.normalize("/privacidad"))
        assertNull(PrivacyPolicyUrl.normalize("#privacidad"))
        assertNull(PrivacyPolicyUrl.normalize("https://"))
        assertNull(PrivacyPolicyUrl.normalize("https:///privacidad"))
        assertNull(PrivacyPolicyUrl.normalize("not a url"))
    }

    @Test
    fun `valid absolute https url is trimmed and preserved`() {
        assertEquals(
            "https://example.com/privacidad",
            PrivacyPolicyUrl.normalize("  https://example.com/privacidad  "),
        )
        assertEquals(
            "https://example.com/privacidad?lang=es",
            PrivacyPolicyUrl.normalize("https://example.com/privacidad?lang=es"),
        )
    }
}
