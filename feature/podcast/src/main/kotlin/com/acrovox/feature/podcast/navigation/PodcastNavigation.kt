package com.acrovox.feature.podcast.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.podcast.detail.PodcastScreen
import com.acrovox.feature.podcast.episode.EpisodeScreen
import com.acrovox.feature.podcast.preview.PodcastPreviewScreen
import kotlinx.serialization.Serializable

/** Aperçu d'un flux avant abonnement. [feedUrl] peut être une page web qui annonce son flux. */
@Serializable
data class PodcastPreviewRoute(val feedUrl: String)

/** Page d'un podcast suivi. */
@Serializable
data class PodcastRoute(val feedId: Long)

@Serializable
data class EpisodeRoute(val episodeId: Long)

fun NavGraphBuilder.podcastPreviewScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPodcast: (Long) -> Unit
) {
    composable<PodcastPreviewRoute> { PodcastPreviewScreen(contentPadding, onBack, onOpenPodcast) }
}

fun NavGraphBuilder.podcastScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit
) {
    composable<PodcastRoute> { PodcastScreen(contentPadding, onBack, onOpenEpisode, onPlay) }
}

fun NavGraphBuilder.episodeScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPodcast: (Long) -> Unit,
    onPlay: (episodeId: Long, positionMs: Long?) -> Unit
) {
    composable<EpisodeRoute> { EpisodeScreen(contentPadding, onBack, onOpenPodcast, onPlay) }
}
