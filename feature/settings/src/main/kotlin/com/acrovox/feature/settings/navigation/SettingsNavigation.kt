package com.acrovox.feature.settings.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.settings.settings.SettingsScreen
import com.acrovox.feature.settings.sync.SyncScreen
import kotlinx.serialization.Serializable

@Serializable
data object SyncRoute

@Serializable
data object SettingsRoute

fun NavGraphBuilder.syncScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<SyncRoute> { SyncScreen(contentPadding, onBack) }
}

fun NavGraphBuilder.settingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(contentPadding, onBack) }
}
