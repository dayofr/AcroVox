package com.acrovox.feature.podcast.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.DownloadButton
import com.acrovox.core.designsystem.component.PlayButtonSize
import com.acrovox.core.designsystem.component.PlayPauseButton
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.DownloadState
import com.acrovox.core.model.EpisodeState

@Composable
fun EpisodeScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPodcast: (Long) -> Unit,
    onPlay: (episodeId: Long, positionMs: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        IconButton(onClick = onBack) {
            Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
        }
        val item = state.item ?: return@Column
        val (episode, feed) = item
        val notes = remember(episode.description, colors.brand) {
            episode.description?.let { showNotes(it, colors.brand) { position -> onPlay(episode.id, position) } }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(episode.imageUrl ?: feed.imageUrl, contentDescription = null, modifier = Modifier.size(96.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        feed.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.brand,
                        modifier = Modifier.clickable { onOpenPodcast(feed.id) }
                    )
                    Text(
                        listOfNotNull(
                            formatRelativeDate(episode.pubDate),
                            formatDuration(episode.durationMs)
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            Text(episode.title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                PlayPauseButton(isPlaying = false, onClick = { onPlay(episode.id, null) }, size = PlayButtonSize.Medium)
                Row(Modifier.weight(1f)) {}
                DownloadButton(state.download, onClick = viewModel::toggleDownload)
                if (episode.state == EpisodeState.IGNORED) {
                    TextButton(onClick = viewModel::restore) { Text("Reprendre", color = colors.brand) }
                } else {
                    IconButton(onClick = viewModel::toggleQueue) {
                        Icon(
                            if (state.queued) AcroVoxIcons.RemoveFromQueue else AcroVoxIcons.AddToQueue,
                            contentDescription = if (state.queued) "Retirer de la file" else "Ajouter à la file",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = viewModel::ignore) {
                        Icon(AcroVoxIcons.Ignore, contentDescription = "Ignorer", tint = colors.textSecondary)
                    }
                }
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (episode.isFavorite) AcroVoxIcons.Favorite else AcroVoxIcons.FavoriteBorder,
                        contentDescription = if (episode.isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                        tint = if (episode.isFavorite) colors.brand else colors.textSecondary
                    )
                }
                if (episode.state != EpisodeState.PLAYED) {
                    IconButton(onClick = viewModel::markPlayed) {
                        Icon(
                            AcroVoxIcons.Check,
                            contentDescription = "Marquer comme écouté",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
            downloadLabel(state.download)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
            HorizontalDivider(color = colors.outlineSubtle)
            if (notes != null) {
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    "Pas de notes pour cet épisode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

private fun downloadLabel(state: DownloadState): String? = when (state) {
    DownloadState.None -> null
    is DownloadState.Queued -> if (state.waitingForWifi) {
        "Téléchargement en attente du Wi-Fi"
    } else {
        "Téléchargement en attente"
    }
    is DownloadState.Running -> state.progress?.let { "Téléchargement : ${(it * 100).toInt()} %" } ?: "Téléchargement…"
    DownloadState.Completed -> "Téléchargé : lisible hors connexion"
    is DownloadState.Failed -> "Échec du téléchargement" + (state.reason?.let { " : $it" } ?: "")
}
