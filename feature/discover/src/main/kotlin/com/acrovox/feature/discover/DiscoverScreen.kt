package com.acrovox.feature.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.component.ScreenPlaceholder

@Composable
fun DiscoverScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = "Explorer",
        message = "Rechercher et découvrir des podcasts.",
        contentPadding = contentPadding,
        modifier = modifier
    )
}
