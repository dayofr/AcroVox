package com.acrovox.feature.inbox

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.component.ScreenPlaceholder

@Composable
fun InboxScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenPlaceholder(
        title = "Boîte de réception",
        message = "Les nouveaux épisodes à trier : garder ou ignorer.",
        contentPadding = contentPadding,
        modifier = modifier
    )
}
