package com.acrovox.feature.queue.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.queue.QueueScreen
import kotlinx.serialization.Serializable

@Serializable
data object QueueRoute

fun NavGraphBuilder.queueScreen(contentPadding: PaddingValues, onOpenEpisode: (Long) -> Unit) {
    composable<QueueRoute> { QueueScreen(contentPadding, onOpenEpisode) }
}
