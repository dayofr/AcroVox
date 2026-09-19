package com.acrovox.app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.acrovox.app.navigation.TopLevelDestination
import com.acrovox.app.ui.AcroVoxApp
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AcroVoxAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent { AcroVoxTheme { AcroVoxApp() } }
    }

    @Test
    fun startsOnHome() {
        tab(TopLevelDestination.HOME).assertIsSelected()
        composeRule.onNodeWithText("Vos podcasts suivis et les derniers épisodes.").assertExists()
    }

    @Test
    fun eachTabShowsItsScreen() {
        val titles = mapOf(
            TopLevelDestination.INBOX to "Boîte de réception",
            TopLevelDestination.QUEUE to "File de lecture",
            TopLevelDestination.DISCOVER to "Rechercher et découvrir des podcasts.",
            TopLevelDestination.LIBRARY to "Abonnements, téléchargements, historique et réglages.",
            TopLevelDestination.HOME to "Vos podcasts suivis et les derniers épisodes."
        )
        titles.forEach { (destination, text) ->
            tab(destination).performClick()
            tab(destination).assertIsSelected()
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    private fun tab(destination: TopLevelDestination): SemanticsNodeInteraction =
        composeRule.onNode(hasText(destination.label) and isSelectable())
}
