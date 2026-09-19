package com.acrovox.feature.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.component.ScreenPlaceholder

@Composable
fun LibraryScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = "Bibliothèque",
        message = "Abonnements, téléchargements, historique et réglages.",
        contentPadding = contentPadding,
        modifier = modifier
    )
}
