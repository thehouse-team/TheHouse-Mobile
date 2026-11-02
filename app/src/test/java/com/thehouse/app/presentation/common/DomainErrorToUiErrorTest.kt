package com.thehouse.app.presentation.common

import com.thehouse.app.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainErrorToUiErrorTest {

    @Test
    fun `maps every domain error to a safe ui error`() {
        assertEquals(UiError.Network, DomainError.Network.toUiError())
        assertEquals(UiError.Timeout, DomainError.Timeout.toUiError())
        assertEquals(UiError.Unauthorized, DomainError.Unauthorized.toUiError())
        assertEquals(UiError.NotFound, DomainError.NotFound.toUiError())
        assertEquals(UiError.Conflict, DomainError.Conflict.toUiError())
        assertEquals(UiError.RateLimited, DomainError.RateLimited.toUiError())
        assertEquals(UiError.Server, DomainError.Server.toUiError())
        assertEquals(UiError.Unknown, DomainError.Unknown.toUiError())
    }
}
