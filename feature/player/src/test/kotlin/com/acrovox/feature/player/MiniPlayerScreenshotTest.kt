package com.acrovox.feature.player

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.player.PlayerState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Référence visuelle du mini-lecteur.
 *
 * Enregistrer : `./gradlew :feature:player:recordRoborazziDebug`
 * Vérifier : `./gradlew :feature:player:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class MiniPlayerScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test fun miniPlayerDark() = capture("mini_player_dark", dark = true)

    @Test fun miniPlayerLight() = capture("mini_player_light", dark = false)

    private fun capture(name: String, dark: Boolean) {
        val state = PlayerState(
            episodeId = 1L,
            title = "Épisode 42 : les ondes claires",
            podcastTitle = "Pulse Audio",
            isPlaying = true,
            positionMs = 754_000,
            durationMs = 2_450_000
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AcroVoxTheme(darkTheme = dark) {
                MiniPlayer(
                    state = state,
                    onOpen = {},
                    onPlayPause = {},
                    onSkipBack = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
