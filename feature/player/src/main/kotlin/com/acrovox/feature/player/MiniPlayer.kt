package com.acrovox.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.component.AcroVoxProgressBar
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.PlayButtonSize
import com.acrovox.core.designsystem.component.PlayPauseButton
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.player.PlayerState

/** Mini-lecteur flottant : pilule de 64 dp au-dessus de la barre de navigation. */
@Composable
fun MiniPlayer(
    state: PlayerState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AcroVoxTheme.colors
    Surface(
        onClick = onOpen,
        shape = AcroVoxShape.Card,
        color = colors.surfaceFloating,
        border = BorderStroke(1.dp, colors.outlineSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(12.dp, AcroVoxShape.Card, clip = false)
    ) {
        Box {
            Row(
                Modifier.padding(horizontal = Spacing.sm).padding(end = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    state.artworkUrl,
                    contentDescription = null,
                    shape = AcroVoxShape.ArtworkSmall,
                    modifier = Modifier.padding(vertical = 8.dp).size(48.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        state.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onSkipBack) {
                    Icon(AcroVoxIcons.Replay10, contentDescription = "Reculer", tint = colors.textPrimary)
                }
                Box(contentAlignment = Alignment.Center) {
                    PlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause, size = PlayButtonSize.Medium)
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            color = colors.onBrand,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            AcroVoxProgressBar(
                progress = { state.progress },
                height = 2.dp,
                trackColor = colors.outlineSubtle.copy(alpha = 0f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = Spacing.gutter)
            )
        }
    }
}
