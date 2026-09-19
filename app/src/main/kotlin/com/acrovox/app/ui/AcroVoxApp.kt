package com.acrovox.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.acrovox.app.navigation.AcroVoxNavHost
import com.acrovox.app.navigation.TopLevelDestination
import com.acrovox.core.designsystem.theme.AcroVoxTheme

/**
 * Racine de l'interface : contenu, mini-lecteur flottant et barre de navigation.
 *
 * @param miniPlayer mini-lecteur posé 8 dp au-dessus de la barre ; rien tant qu'aucun
 * épisode n'est chargé (PULSE-15).
 */
@Composable
fun AcroVoxApp(
    navController: NavHostController = rememberNavController(),
    miniPlayer: (@Composable () -> Unit)? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = TopLevelDestination.entries.firstOrNull { destination ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }
    // Sur un écran secondaire (aperçu d'un podcast…), l'onglet d'origine reste sélectionné.
    var lastTopLevel by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    if (current != null) lastTopLevel = current
    Scaffold(
        containerColor = AcroVoxTheme.colors.canvas,
        // Le bas est déjà réservé par la barre de navigation, qui gère son propre encart.
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            Column {
                if (miniPlayer != null) {
                    Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) { miniPlayer() }
                }
                AcroVoxBottomBar(selected = lastTopLevel, onSelect = navController::navigateToTopLevel)
            }
        }
    ) { innerPadding ->
        AcroVoxNavHost(
            navController = navController,
            contentPadding = innerPadding,
            onSelectTab = navController::navigateToTopLevel
        )
    }
}

/** Change d'onglet en gardant l'état de chaque onglet, sans empiler les onglets. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
