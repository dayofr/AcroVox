package com.acrovox.feature.downloads.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.downloads.DownloadsScreen
import kotlinx.serialization.Serializable

@Serializable
data object DownloadsRoute

fun NavGraphBuilder.downloadsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit
) {
    composable<DownloadsRoute> { DownloadsScreen(contentPadding, onBack, onOpenEpisode, onPlay) }
}
