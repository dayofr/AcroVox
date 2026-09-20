package com.acrovox.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Génère le profil de démarrage de l'app.
 *
 * Lancer sur émulateur : `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
 * puis `./gradlew :baselineprofile:collectNonMinifiedReleaseBaselineProfile`.
 * Le profil atterrit dans `app/src/release/generated/baselineProfiles/`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() = rule.collect(
        packageName = "com.acrovox.app",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun generateScrollProfile() = rule.collect(
        packageName = "com.acrovox.app",
        maxIterations = 3,
        stableIterations = 2
    ) {
        pressHome()
        startActivityAndWait()
        // Balayage vertical de l'accueil pour couvrir le rendu des listes.
        device.findObject(By.scrollable(true))?.let {
            it.scroll(Direction.DOWN, 1f)
            it.scroll(Direction.UP, 1f)
        }
    }
}
