package com.acrovox.feature.library.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.library.LibraryScreen
import com.acrovox.feature.library.opml.OpmlImportScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/** Import d'un fichier OPML choisi par l'utilisateur ([uri] : `content://`). */
@Serializable
data class OpmlImportRoute(val uri: String)

fun NavGraphBuilder.libraryScreen(contentPadding: PaddingValues, onImportOpml: (String) -> Unit) {
    composable<LibraryRoute> { LibraryScreen(contentPadding, onImportOpml) }
}

fun NavGraphBuilder.opmlImportScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    composable<OpmlImportRoute> { OpmlImportScreen(contentPadding, onClose) }
}
