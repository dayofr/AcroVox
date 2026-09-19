package com.acrovox.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme

enum class PlayButtonSize(val container: Dp, val icon: Dp) {
    /** Lecteur plein écran. */
    Large(56.dp, 32.dp),

    /** Mini-lecteur. */
    Medium(40.dp, 24.dp),

    /** Cartes d'épisode. */
    Small(32.dp, 20.dp)
}

/** Bouton lecture/pause corail, compressé à l'appui. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlayButtonSize = PlayButtonSize.Large
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "playScale")
    val colors = AcroVoxTheme.colors
    Box(
        modifier = modifier
            .size(size.container)
            .scale(scale)
            .clip(CircleShape)
            .background(colors.brand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.onBrand),
                role = Role.Button,
                onClickLabel = if (isPlaying) "Pause" else "Lecture",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) AcroVoxIcons.Pause else AcroVoxIcons.Play,
            contentDescription = if (isPlaying) "Pause" else "Lecture",
            tint = colors.onBrand,
            modifier = Modifier.size(size.icon)
        )
    }
}
