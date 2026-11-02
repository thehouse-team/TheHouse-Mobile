package com.thehouse.app.data.error

import com.thehouse.app.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test

class DataErrorMapperTest {

    @Test
    fun `maps technical errors to domain errors`() {
        assertEquals(DomainError.Network, DataError.Network.toDomainError())
        assertEquals(DomainError.Timeout, DataError.Timeout.toDomainError())
        assertEquals(DomainError.Unknown, DataError.Serialization.toDomainError())
        assertEquals(DomainError.Unknown, DataError.Local.toDomainError())
        assertEquals(DomainError.Unknown, DataError.Unknown().toDomainError())
    }

    @Test
    fun `maps http status codes to semantic domain errors`() {
        assertEquals(DomainError.Unauthorized, DataError.Http(401).toDomainError())
        assertEquals(DomainError.Unauthorized, DataError.Http(403).toDomainError())
        assertEquals(DomainError.NotFound, DataError.Http(404).toDomainError())
        assertEquals(DomainError.Timeout, DataError.Http(408).toDomainError())
        assertEquals(DomainError.Conflict, DataError.Http(409).toDomainError())
        assertEquals(DomainError.RateLimited, DataError.Http(429).toDomainError())
        assertEquals(DomainError.Server, DataError.Http(500).toDomainError())
        assertEquals(DomainError.Server, DataError.Http(599).toDomainError())
        assertEquals(DomainError.Unknown, DataError.Http(418).toDomainError())
    }
}
