package com.acrovox.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing

private val CardShape = AcroVoxShape.Artwork

/**
 * Carte d'épisode des listes (accueil, boîte de réception, page podcast).
 *
 * @param progress avancement de 0 à 1 si l'épisode est commencé, sinon null.
 * @param badges pastilles à gauche de la barre d'actions (téléchargé, streaming…).
 * @param actions boutons à droite, avant le bouton lecture.
 */
@Composable
fun EpisodeCard(
    title: String,
    podcastTitle: String,
    dateLabel: String,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    durationLabel: String? = null,
    artworkUrl: String? = null,
    isPlaying: Boolean = false,
    progress: Float? = null,
    badges: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val colors = AcroVoxTheme.colors
    val type = MaterialTheme.typography
    Surface(
        shape = CardShape,
        color = if (selected) colors.surfaceFloating else colors.surfaceCard,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.outlineSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(64.dp)) {
                Artwork(
                    url = artworkUrl,
                    contentDescription = null,
                    shape = AcroVoxShape.ArtworkSmall,
                    modifier = Modifier.size(64.dp)
                )
                if (selected) {
                    Box(
                        Modifier.size(64.dp).background(colors.scrim, AcroVoxShape.ArtworkSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AcroVoxIcons.Check,
                            contentDescription = "Sélectionné",
                            tint = colors.brand,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                if (durationLabel != null) {
                    DurationBadge(durationLabel, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        podcastTitle,
                        style = type.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(dateLabel, style = type.labelSmall, color = colors.textMuted, maxLines = 1)
                }
                Text(
                    title,
                    style = type.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (description != null) {
                    Text(
                        description,
                        style = type.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
                if (progress != null) {
                    AcroVoxProgressBar(progress = {
                        progress
                    }, height = 3.dp, modifier = Modifier.padding(top = Spacing.sm))
                }
                HorizontalDivider(Modifier.padding(top = Spacing.md), color = colors.outlineSubtle)
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), content = badges)
                    actions()
                    EpisodePlayButton(isPlaying = isPlaying, onClick = onPlayClick)
                }
            }
        }
    }
}

/** Bouton d'action secondaire des cartes (file, partage, ignorer…). */
@Composable
fun EpisodeActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = AcroVoxTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EpisodePlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Box(
            Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                EqualizerIndicator()
            } else {
                Icon(
                    AcroVoxIcons.Play,
                    contentDescription = "Lire",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
