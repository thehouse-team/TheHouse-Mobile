package com.thehouse.app.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `maps a data model to a domain model`() {
        val mapper = Mapper<String, Int> { value -> value.length }

        assertEquals(5, mapper.map("house"))
    }
}
