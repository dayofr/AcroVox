package com.acrovox.feature.library.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.library.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryScreen(contentPadding: PaddingValues) {
    composable<LibraryRoute> { LibraryScreen(contentPadding) }
}
