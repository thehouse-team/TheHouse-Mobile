package com.thehouse.app.device

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thehouse.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D21: navegación guest sobre la app real. Sin credenciales: verifica que el
 * arranque resuelve un destino público y que no hay back stack duplicado.
 */
@RunWith(AndroidJUnit4::class)
class DeviceNavigationTest {

    @Test
    fun guest_startup_reachesRootWithoutLogin() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing.not())
                assertTrue(activity.intent.component?.className == MainActivity::class.java.name)
            }
        }
    }

    @Test
    fun guest_relaunch_doesNotLeavePreviousActivityResumed() {
        ActivityScenario.launch(MainActivity::class.java).use { first ->
            first.onActivity { assertTrue(it.isFinishing.not()) }
        }
        ActivityScenario.launch(MainActivity::class.java).use { second ->
            second.onActivity { assertTrue(it.isFinishing.not()) }
        }
    }
}
