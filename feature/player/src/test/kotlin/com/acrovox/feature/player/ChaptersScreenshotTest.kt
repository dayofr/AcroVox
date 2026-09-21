package com.acrovox.feature.player

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.acrovox.core.database.entity.ChapterEntity
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Référence visuelle de l'onglet Chapitres.
 *
 * Enregistrer : `./gradlew :feature:player:recordRoborazziDebug`
 * Vérifier : `./gradlew :feature:player:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ChaptersScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val chapters = listOf(
        ChapterEntity(id = 1, episodeId = 1, startMs = 0, title = "Introduction"),
        ChapterEntity(
            id = 2,
            episodeId = 1,
            startMs = 95_000,
            title = "L'invité de la semaine : parcours et coulisses"
        ),
        ChapterEntity(id = 3, episodeId = 1, startMs = 754_000, title = "Questions des auditeurs")
    )

    @Test
    fun chaptersDark() = capture("chapters_dark", dark = true)

    @Test
    fun chaptersLight() = capture("chapters_light", dark = false)

    private fun capture(name: String, dark: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AcroVoxTheme(darkTheme = dark) {
                ChaptersList(
                    chapters = chapters,
                    positionMs = 120_000,
                    durationMs = 2_450_000,
                    onSeek = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
