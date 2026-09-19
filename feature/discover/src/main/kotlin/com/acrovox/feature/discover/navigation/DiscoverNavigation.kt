package com.acrovox.feature.discover.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.discover.DiscoverScreen
import kotlinx.serialization.Serializable

@Serializable
data object DiscoverRoute

fun NavGraphBuilder.discoverScreen(contentPadding: PaddingValues, onOpenFeed: (String) -> Unit) {
    composable<DiscoverRoute> { DiscoverScreen(contentPadding, onOpenFeed) }
}
