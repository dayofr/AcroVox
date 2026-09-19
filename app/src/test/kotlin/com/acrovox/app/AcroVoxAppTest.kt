package com.acrovox.app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.acrovox.app.navigation.TopLevelDestination
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Lance la vraie MainActivity avec le graphe Hilt de l'application. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AcroVoxAppTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsOnHome() {
        tab(TopLevelDestination.HOME).assertIsSelected()
        composeRule.onNodeWithText("Vos podcasts suivis et les derniers épisodes.").assertExists()
    }

    @Test
    fun eachTabShowsItsScreen() {
        val texts = mapOf(
            TopLevelDestination.INBOX to "Boîte de réception",
            TopLevelDestination.QUEUE to "File de lecture",
            TopLevelDestination.DISCOVER to "Nom du podcast ou adresse du flux",
            TopLevelDestination.LIBRARY to "Abonnements, téléchargements, historique et réglages.",
            TopLevelDestination.HOME to "Vos podcasts suivis et les derniers épisodes."
        )
        texts.forEach { (destination, text) ->
            tab(destination).performClick()
            tab(destination).assertIsSelected()
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    private fun tab(destination: TopLevelDestination): SemanticsNodeInteraction =
        composeRule.onNode(hasText(destination.label) and isSelectable())
}
