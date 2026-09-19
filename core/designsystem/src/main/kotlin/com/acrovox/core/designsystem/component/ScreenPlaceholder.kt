package com.acrovox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing

/** Écran pas encore construit : titre et une ligne d'explication. */
@Composable
fun ScreenPlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(title, style = HeadlineLargeMobile, color = AcroVoxTheme.colors.textPrimary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AcroVoxTheme.colors.textSecondary)
    }
}
