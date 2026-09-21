package com.acrovox.feature.library.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.library.LibraryActions
import com.acrovox.feature.library.LibraryScreen
import com.acrovox.feature.library.antennapod.AntennaPodImportScreen
import com.acrovox.feature.library.episodes.EpisodeListKind
import com.acrovox.feature.library.episodes.EpisodeListScreen
import com.acrovox.feature.library.opml.OpmlImportScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/** Import d'un fichier OPML choisi par l'utilisateur ([uri] : `content://`). */
@Serializable
data class OpmlImportRoute(val uri: String)

/** Migration depuis une sauvegarde AntennaPod ([uri] : `content://`, base SQLite). */
@Serializable
data class AntennaPodImportRoute(val uri: String)

/** Favoris ou historique. */
@Serializable
data class EpisodeListRoute(val kind: EpisodeListKind)

fun NavGraphBuilder.libraryScreen(contentPadding: PaddingValues, actions: LibraryActions) {
    composable<LibraryRoute> { LibraryScreen(contentPadding, actions) }
}

fun NavGraphBuilder.opmlImportScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    composable<OpmlImportRoute> { OpmlImportScreen(contentPadding, onClose) }
}

fun NavGraphBuilder.antennaPodImportScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    composable<AntennaPodImportRoute> { AntennaPodImportScreen(contentPadding, onClose) }
}

fun NavGraphBuilder.episodeListScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit
) {
    composable<EpisodeListRoute> { EpisodeListScreen(contentPadding, onBack, onOpenEpisode, onPlay) }
}
