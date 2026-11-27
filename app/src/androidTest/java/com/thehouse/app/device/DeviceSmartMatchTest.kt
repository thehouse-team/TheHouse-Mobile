package com.thehouse.app.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thehouse.app.presentation.smartmatch.SmartMatchScreen
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D21: Smart Match — verifica que la pantalla real se componga y exponga su
 * título accesible sin navegación inventada. El contenido dependiente de red
 * se valida en tests JVM con MockWebServer.
 */
@RunWith(AndroidJUnit4::class)
class DeviceSmartMatchTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smartMatchScreen_rendersAccessibleTitle() {
        val factory = com.thehouse.app.presentation.U2ViewModelFactory(
            com.thehouse.app.di.AppContainer(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            ),
        )
        composeRule.setContent {
            SmartMatchScreen(
                factory = factory,
                onRequireLogin = {},
                onConfigurePreferences = {},
                onRoomClick = {},
                onFavoriteClick = {},
                onContactClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Título Smart Match").assertIsDisplayed()
        assertNotNull(factory)
    }
}
