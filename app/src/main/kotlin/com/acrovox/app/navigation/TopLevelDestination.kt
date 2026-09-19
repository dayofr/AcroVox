package com.acrovox.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.feature.discover.navigation.DiscoverRoute
import com.acrovox.feature.home.navigation.HomeRoute
import com.acrovox.feature.inbox.navigation.InboxRoute
import com.acrovox.feature.library.navigation.LibraryRoute
import com.acrovox.feature.queue.navigation.QueueRoute

/** Onglets de la barre de navigation, dans l'ordre d'affichage. */
enum class TopLevelDestination(val route: Any, val icon: ImageVector, val label: String) {
    HOME(HomeRoute, AcroVoxIcons.Home, "Accueil"),
    INBOX(InboxRoute, AcroVoxIcons.Inbox, "Boîte"),
    QUEUE(QueueRoute, AcroVoxIcons.Queue, "File"),
    DISCOVER(DiscoverRoute, AcroVoxIcons.Discover, "Explorer"),
    LIBRARY(LibraryRoute, AcroVoxIcons.Library, "Bibliothèque")
}
