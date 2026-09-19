package com.acrovox.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.acrovox.feature.discover.navigation.discoverScreen
import com.acrovox.feature.home.HomeActions
import com.acrovox.feature.home.navigation.HomeRoute
import com.acrovox.feature.home.navigation.homeScreen
import com.acrovox.feature.inbox.navigation.inboxScreen
import com.acrovox.feature.library.navigation.OpmlImportRoute
import com.acrovox.feature.library.navigation.libraryScreen
import com.acrovox.feature.library.navigation.opmlImportScreen
import com.acrovox.feature.podcast.navigation.EpisodeRoute
import com.acrovox.feature.podcast.navigation.PodcastPreviewRoute
import com.acrovox.feature.podcast.navigation.PodcastRoute
import com.acrovox.feature.podcast.navigation.episodeScreen
import com.acrovox.feature.podcast.navigation.podcastPreviewScreen
import com.acrovox.feature.podcast.navigation.podcastScreen
import com.acrovox.feature.queue.navigation.queueScreen

@Composable
fun AcroVoxNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onSelectTab: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(
            contentPadding,
            HomeActions(
                onOpenPodcast = { navController.navigate(PodcastRoute(it)) },
                onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
                onSeeAllSubscriptions = { onSelectTab(TopLevelDestination.LIBRARY) },
                onExplore = { onSelectTab(TopLevelDestination.DISCOVER) }
            )
        )
        inboxScreen(contentPadding)
        queueScreen(contentPadding)
        discoverScreen(contentPadding, onOpenFeed = { navController.navigate(PodcastPreviewRoute(it)) })
        libraryScreen(contentPadding, onImportOpml = { navController.navigate(OpmlImportRoute(it)) })
        opmlImportScreen(contentPadding, onClose = navController::popBackStack)
        podcastPreviewScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenPodcast = { navController.navigate(PodcastRoute(it)) }
        )
        podcastScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
            // Lecture : PULSE-13.
            onPlay = {}
        )
        episodeScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenPodcast = { navController.navigate(PodcastRoute(it)) },
            onPlay = { _, _ -> }
        )
    }
}
