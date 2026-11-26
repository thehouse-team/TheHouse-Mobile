package com.thehouse.app.perf

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Formato y habilitación de la instrumentación PERF (sección 9 del hardening).
 *
 * Formato exigido: `PERF <operation> durationMs=<n>` sin PII.
 */
class PerfTest {

    private val lines = mutableListOf<String>()
    private val previousEnabled = Perf.enabled
    private val previousLogger = Perf.logger

    @Before
    fun setUp() {
        lines.clear()
        Perf.enabled = true
        Perf.logger = { message -> lines += message }
    }

    @After
    fun tearDown() {
        Perf.enabled = previousEnabled
        Perf.logger = previousLogger
    }

    @Test
    fun `trace emite PERF operation durationMs con duracion no negativa`() {
        val result = Perf.trace("network.properties.get") { 42 }

        assertEquals(42, result)
        assertEquals(1, lines.size)
        val line = lines.first()
        assertTrue("formato inesperado: $line", PERF_FORMAT.matches(line))
        assertTrue(line.startsWith("PERF network.properties.get durationMs="))
    }

    @Test
    fun `traceSuspend emite una linea y devuelve el valor`() = runTest {
        val result = Perf.traceSuspend("smartmatch.ranking") { listOf(1, 2, 3) }

        assertEquals(listOf(1, 2, 3), result)
        assertEquals(1, lines.size)
        assertTrue("formato inesperado: ${lines.first()}", PERF_FORMAT.matches(lines.first()))
    }

    @Test
    fun `since mide desde el inicio proporcionado`() {
        val startNanos = System.nanoTime() - 2_000_000L

        Perf.since("image.load.property_card", startNanos)

        assertEquals(1, lines.size)
        val line = lines.first()
        assertTrue("formato inesperado: $line", PERF_FORMAT.matches(line))
        assertTrue(line.startsWith("PERF image.load.property_card durationMs="))
        val durationMs = line.substringAfterLast('=').toLong()
        assertTrue("duración negativa: $durationMs", durationMs >= 2)
    }

    @Test
    fun `deshabilitado no emite ni ejecuta logging`() {
        Perf.enabled = false

        val result = Perf.trace("state.update") { "valor" }
        Perf.since("image.load.property_card", System.nanoTime())

        assertEquals("valor", result)
        assertTrue(lines.isEmpty())
    }

    private companion object {
        val PERF_FORMAT = Regex("""^PERF \S+ durationMs=\d+$""")
    }
}
