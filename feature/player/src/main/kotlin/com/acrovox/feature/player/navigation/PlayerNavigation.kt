package com.acrovox.feature.player.navigation

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.player.PlayerScreen
import kotlinx.serialization.Serializable

@Serializable
data object PlayerRoute

/** Lecteur plein écran, qui glisse depuis le bas. */
fun NavGraphBuilder.playerScreen(onClose: () -> Unit, onOpenEpisode: (Long) -> Unit) {
    composable<PlayerRoute>(
        enterTransition = { slideInVertically { it } },
        exitTransition = { slideOutVertically { it } },
        popExitTransition = { slideOutVertically { it } }
    ) {
        PlayerScreen(onClose, onOpenEpisode)
    }
}
