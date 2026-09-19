package com.acrovox.feature.podcast.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.podcast.preview.PodcastPreviewScreen
import kotlinx.serialization.Serializable

/** Aperçu d'un flux avant abonnement. [feedUrl] peut être une page web qui annonce son flux. */
@Serializable
data class PodcastPreviewRoute(val feedUrl: String)

fun NavGraphBuilder.podcastPreviewScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<PodcastPreviewRoute> { PodcastPreviewScreen(contentPadding, onBack) }
}
