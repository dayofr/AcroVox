package com.acrovox.feature.queue

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.component.ScreenPlaceholder

@Composable
fun QueueScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = "File de lecture",
        message = "Les épisodes gardés, dans l'ordre d'écoute.",
        contentPadding = contentPadding,
        modifier = modifier
    )
}
