package com.acrovox.feature.inbox.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.inbox.InboxScreen
import kotlinx.serialization.Serializable

@Serializable
data object InboxRoute

fun NavGraphBuilder.inboxScreen(contentPadding: PaddingValues, onOpenEpisode: (Long) -> Unit, onPlay: (Long) -> Unit) {
    composable<InboxRoute> { InboxScreen(contentPadding, onOpenEpisode, onPlay) }
}
