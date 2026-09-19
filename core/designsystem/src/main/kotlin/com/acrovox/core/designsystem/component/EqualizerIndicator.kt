package com.acrovox.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.theme.AcroVoxTheme

private val BarDurationsMs = intArrayOf(520, 380, 610, 450)

/** Décalages de phase : dès la première frame, les barres ont des hauteurs différentes. */
private val BarOffsetsMs = intArrayOf(260, 380, 90, 330)
private const val MIN_FRACTION = 0.25f

/** Égaliseur animé : signale l'épisode en cours dans une liste. Figé si [animate] est faux. */
@Composable
fun EqualizerIndicator(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    color: Color = AcroVoxTheme.colors.brand
) {
    val fractions: List<State<Float>> = if (animate) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        BarDurationsMs.mapIndexed { index, duration ->
            transition.animateFloat(
                initialValue = MIN_FRACTION,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(BarOffsetsMs[index], StartOffsetType.FastForward)
                ),
                label = "bar"
            )
        }
    } else {
        remember { listOf(0.5f, 1f, 0.35f, 0.75f).map { mutableFloatStateOf(it) } }
    }
    Canvas(modifier.size(width = 18.dp, height = 16.dp)) {
        val barWidth = 3.dp.toPx()
        val gap = (size.width - barWidth * fractions.size) / (fractions.size - 1)
        fractions.forEachIndexed { index, fraction ->
            val height = size.height * fraction.value
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
