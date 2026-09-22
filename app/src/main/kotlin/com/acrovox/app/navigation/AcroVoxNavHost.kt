package com.acrovox.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.acrovox.feature.discover.navigation.discoverScreen
import com.acrovox.feature.downloads.navigation.DownloadsRoute
import com.acrovox.feature.downloads.navigation.downloadsScreen
import com.acrovox.feature.home.HomeActions
import com.acrovox.feature.home.navigation.HomeRoute
import com.acrovox.feature.home.navigation.homeScreen
import com.acrovox.feature.inbox.navigation.inboxScreen
import com.acrovox.feature.library.LibraryActions
import com.acrovox.feature.library.navigation.AntennaPodImportRoute
import com.acrovox.feature.library.navigation.EpisodeListRoute
import com.acrovox.feature.library.navigation.OpmlImportRoute
import com.acrovox.feature.library.navigation.antennaPodImportScreen
import com.acrovox.feature.library.navigation.episodeListScreen
import com.acrovox.feature.library.navigation.libraryScreen
import com.acrovox.feature.library.navigation.opmlImportScreen
import com.acrovox.feature.player.navigation.playerScreen
import com.acrovox.feature.podcast.navigation.EpisodeRoute
import com.acrovox.feature.podcast.navigation.PodcastPreviewRoute
import com.acrovox.feature.podcast.navigation.PodcastRoute
import com.acrovox.feature.podcast.navigation.episodeScreen
import com.acrovox.feature.podcast.navigation.podcastPreviewScreen
import com.acrovox.feature.podcast.navigation.podcastScreen
import com.acrovox.feature.queue.navigation.queueScreen
import com.acrovox.feature.settings.navigation.SettingsRoute
import com.acrovox.feature.settings.navigation.SyncRoute
import com.acrovox.feature.settings.navigation.settingsScreen
import com.acrovox.feature.settings.navigation.syncScreen

@Composable
fun AcroVoxNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onSelectTab: (TopLevelDestination) -> Unit,
    onPlay: (episodeId: Long, positionMs: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(
            contentPadding,
            HomeActions(
                onOpenPodcast = { navController.navigate(PodcastRoute(it)) },
                onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
                onPlay = { onPlay(it, null) },
                onSeeAllSubscriptions = { onSelectTab(TopLevelDestination.LIBRARY) },
                onExplore = { onSelectTab(TopLevelDestination.DISCOVER) }
            )
        )
        inboxScreen(contentPadding, onOpenEpisode = {
            navController.navigate(EpisodeRoute(it))
        }, onPlay = { onPlay(it, null) })
        queueScreen(contentPadding, onOpenEpisode = { navController.navigate(EpisodeRoute(it)) })
        discoverScreen(contentPadding, onOpenFeed = { navController.navigate(PodcastPreviewRoute(it)) })
        libraryScreen(
            contentPadding,
            LibraryActions(
                onOpenPodcast = { navController.navigate(PodcastRoute(it)) },
                onOpenEpisodeList = { navController.navigate(EpisodeListRoute(it)) },
                onImportOpml = { navController.navigate(OpmlImportRoute(it)) },
                onImportAntennaPod = { navController.navigate(AntennaPodImportRoute(it)) },
                onOpenDownloads = { navController.navigate(DownloadsRoute) },
                onOpenSync = { navController.navigate(SyncRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) }
            )
        )
        settingsScreen(contentPadding, onBack = navController::popBackStack)
        syncScreen(contentPadding, onBack = navController::popBackStack)
        downloadsScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
            onPlay = { onPlay(it, null) }
        )
        episodeListScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
            onPlay = { onPlay(it, null) }
        )
        opmlImportScreen(contentPadding, onClose = navController::popBackStack)
        antennaPodImportScreen(contentPadding, onClose = navController::popBackStack)
        podcastPreviewScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenPodcast = { navController.navigate(PodcastRoute(it)) }
        )
        podcastScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenEpisode = { navController.navigate(EpisodeRoute(it)) },
            onPlay = { onPlay(it, null) }
        )
        playerScreen(
            onClose = navController::popBackStack,
            onOpenEpisode = { navController.navigate(EpisodeRoute(it)) }
        )
        episodeScreen(
            contentPadding,
            onBack = navController::popBackStack,
            onOpenPodcast = { navController.navigate(PodcastRoute(it)) },
            onPlay = onPlay
        )
    }
}
