package com.acrovox.feature.home.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acrovox.feature.home.HomeActions
import com.acrovox.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(contentPadding: PaddingValues, actions: HomeActions) {
    composable<HomeRoute> { HomeScreen(contentPadding, actions) }
}
