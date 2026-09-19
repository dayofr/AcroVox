package com.acrovox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme

/** Durée posée sur une pochette : capsule noire translucide. */
@Composable
fun DurationBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .background(AcroVoxTheme.colors.scrim, AcroVoxShape.Pill)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** Pastille d'état (« Téléchargé », « En streaming »…). */
@Composable
fun StatusBadge(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AcroVoxTheme.colors.accent
) {
    Row(
        modifier = modifier
            .background(AcroVoxTheme.colors.surfaceFloating, AcroVoxShape.Pill)
            .border(1.dp, tint.copy(alpha = 0.2f), AcroVoxShape.Pill)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = AcroVoxTheme.colors.textPrimary)
    }
}

@Composable
fun DownloadedBadge(modifier: Modifier = Modifier) {
    StatusBadge(text = "Téléchargé", icon = AcroVoxIcons.Check, modifier = modifier)
}
