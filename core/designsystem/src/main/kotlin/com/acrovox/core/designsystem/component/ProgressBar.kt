package com.acrovox.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.theme.AcroVoxTheme

/**
 * Barre de progression fine aux extrémités arrondies.
 *
 * [progress] est un lambda : la position change souvent pendant la lecture,
 * seul le dessin est invalidé, pas la composition.
 */
@Composable
fun AcroVoxProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = AcroVoxTheme.colors.brand,
    trackColor: Color = AcroVoxTheme.colors.outlineSubtle
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f) }
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = radius)
        val fraction = progress().coerceIn(0f, 1f)
        if (fraction > 0f) {
            drawRoundRect(color, size = Size(size.width * fraction, size.height), cornerRadius = radius)
        }
    }
}
