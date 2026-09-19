package com.acrovox.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.acrovox.feature.discover.navigation.discoverScreen
import com.acrovox.feature.home.navigation.HomeRoute
import com.acrovox.feature.home.navigation.homeScreen
import com.acrovox.feature.inbox.navigation.inboxScreen
import com.acrovox.feature.library.navigation.OpmlImportRoute
import com.acrovox.feature.library.navigation.libraryScreen
import com.acrovox.feature.library.navigation.opmlImportScreen
import com.acrovox.feature.podcast.navigation.PodcastPreviewRoute
import com.acrovox.feature.podcast.navigation.podcastPreviewScreen
import com.acrovox.feature.queue.navigation.queueScreen

@Composable
fun AcroVoxNavHost(navController: NavHostController, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(contentPadding)
        inboxScreen(contentPadding)
        queueScreen(contentPadding)
        discoverScreen(contentPadding, onOpenFeed = { navController.navigate(PodcastPreviewRoute(it)) })
        libraryScreen(contentPadding, onImportOpml = { navController.navigate(OpmlImportRoute(it)) })
        opmlImportScreen(contentPadding, onClose = navController::popBackStack)
        podcastPreviewScreen(contentPadding, onBack = navController::popBackStack)
    }
}
