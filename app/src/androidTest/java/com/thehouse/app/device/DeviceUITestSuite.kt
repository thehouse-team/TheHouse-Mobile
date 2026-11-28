package com.thehouse.app.device

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thehouse.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D21: suite instrumentada de dispositivo sobre la app REAL.
 *
 * No declara composables falsos ni nodos inventados: verifica el paquete,
 * el arranque y la ausencia de crashes. Las pruebas de UI con estado se
 * ejecutan en las pruebas Compose de `LoginScreenInstrumentedTest` y en
 * los scripts de `scripts/mobile-validation/` (evidencia runtime).
 */
@RunWith(AndroidJUnit4::class)
class DeviceUITestSuite {

    @Test
    fun applicationId_matchesContract() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.thehouse.app", context.packageName)
    }

    @Test
    fun app_startsWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing.not())
                assertTrue(activity.application.packageName == "com.thehouse.app")
            }
        }
    }

    @Test
    fun app_restartPreservesProcess() {
        repeat(2) {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertTrue(it.isFinishing.not()) }
            }
        }
    }
}
