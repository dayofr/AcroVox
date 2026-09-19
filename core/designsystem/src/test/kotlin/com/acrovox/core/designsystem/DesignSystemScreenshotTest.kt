package com.acrovox.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.acrovox.core.designsystem.component.ComponentsCatalog
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.ThemeShowcase
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Références visuelles du design system.
 *
 * Enregistrer : `./gradlew :core:designsystem:recordRoborazziDebug`
 * Vérifier : `./gradlew :core:designsystem:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1000dp-xhdpi")
class DesignSystemScreenshotTest {
    private companion object {
        const val ANIMATION_SNAPSHOT_MS = 300L
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test fun themeDark() = capture("theme_dark", dark = true) { ThemeShowcase(Modifier.fillMaxWidth()) }

    @Test fun themeLight() = capture("theme_light", dark = false) { ThemeShowcase(Modifier.fillMaxWidth()) }

    @Test fun componentsDark() = capture("components_dark", dark = true) { ComponentsCatalog(Modifier.fillMaxWidth()) }

    @Test fun componentsLight() = capture("components_light", dark = false) {
        ComponentsCatalog(Modifier.fillMaxWidth())
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Horloge figée puis avancée : les animations infinies (égaliseur) sont capturées en cours.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AcroVoxTheme(darkTheme = dark, content = content) }
        composeRule.mainClock.advanceTimeBy(ANIMATION_SNAPSHOT_MS)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
