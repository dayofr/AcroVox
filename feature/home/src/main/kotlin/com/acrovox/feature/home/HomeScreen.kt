package com.acrovox.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.component.ScreenPlaceholder

@Composable
fun HomeScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = "Accueil",
        message = "Vos podcasts suivis et les derniers épisodes.",
        contentPadding = contentPadding,
        modifier = modifier
    )
}
