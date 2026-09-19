package com.acrovox.feature.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.database.entity.DownloadWithEpisode
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.component.AcroVoxProgressBar
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.PlayButtonSize
import com.acrovox.core.designsystem.component.PlayPauseButton
import com.acrovox.core.designsystem.format.formatBytes
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeState

@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text("Téléchargements", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        LazyColumn(
            contentPadding = PaddingValues(
                Spacing.screenMargin,
                Spacing.sm,
                Spacing.screenMargin,
                contentPadding.calculateBottomPadding() + Spacing.gutter
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { StorageCard(state) }
            item {
                SettingsCard(
                    state,
                    onWifiOnly = viewModel::setWifiOnly,
                    onDownloadedOnly = viewModel::setDownloadedOnly
                )
            }
            if (state.active.isNotEmpty()) {
                item { SectionTitle("En cours") }
                items(state.active, key = { "a" + it.download.episodeId }) { item ->
                    ActiveRow(
                        item,
                        onWifi = state.onWifi,
                        onOpen = { onOpenEpisode(item.download.episodeId) },
                        onDownloadNow = { viewModel.downloadNow(item.download.episodeId) },
                        onCancel = { viewModel.cancel(item.download.episodeId) }
                    )
                }
            }
            if (state.failed.isNotEmpty()) {
                item { SectionTitle("Échecs") }
                items(state.failed, key = { "f" + it.download.episodeId }) { item ->
                    FailedRow(
                        item,
                        onOpen = { onOpenEpisode(item.download.episodeId) },
                        onRetry = { viewModel.retry(item.download.episodeId) },
                        onRemove = { viewModel.cancel(item.download.episodeId) }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AcroVoxFilterChip(
                        "${DownloadFilter.ALL.label} ${state.completedCount}",
                        selected = state.filter == DownloadFilter.ALL,
                        onClick = { viewModel.setFilter(DownloadFilter.ALL) }
                    )
                    AcroVoxFilterChip(
                        "${DownloadFilter.UNPLAYED.label} ${state.unplayedCount}",
                        selected = state.filter == DownloadFilter.UNPLAYED,
                        onClick = { viewModel.setFilter(DownloadFilter.UNPLAYED) }
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Prêts hors connexion", Modifier.weight(1f))
                    TextButton(onClick = viewModel::toggleSort) { Text(state.sort.label, color = colors.brand) }
                }
            }
            if (!state.loading && state.completed.isEmpty()) {
                item {
                    Text(
                        "Aucun épisode téléchargé. Utilisez le bouton Télécharger d'un épisode : " +
                            "rien n'est téléchargé automatiquement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
            items(state.completed, key = { "c" + it.download.episodeId }) { item ->
                CompletedRow(
                    item,
                    onOpen = { onOpenEpisode(item.download.episodeId) },
                    onPlay = { onPlay(item.download.episodeId) },
                    onDelete = { viewModel.delete(item.download.episodeId) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = AcroVoxTheme.colors.textSecondary,
        modifier = modifier.padding(top = Spacing.sm)
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = AcroVoxTheme.colors
    Surface(
        shape = AcroVoxShape.Artwork,
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.outlineSubtle),
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun StorageCard(state: DownloadsUiState) {
    val colors = AcroVoxTheme.colors
    Card {
        Column(Modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("ESPACE DE STOCKAGE", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Text(
                "${formatBytes(state.usedBytes)} utilisés",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            val total = state.usedBytes + state.freeBytes
            AcroVoxProgressBar(progress = { if (total > 0) state.usedBytes.toFloat() / total else 0f }, height = 6.dp)
            Text(
                "${state.completedCount} épisode${if (state.completedCount > 1) "s" else ""} • " +
                    "${formatBytes(state.freeBytes)} libres",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SettingsCard(state: DownloadsUiState, onWifiOnly: (Boolean) -> Unit, onDownloadedOnly: (Boolean) -> Unit) {
    Card {
        Column(Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm)) {
            SettingRow(
                title = "Lecture hors connexion uniquement",
                description = "Pas de streaming : seuls les épisodes téléchargés se lisent.",
                checked = state.settings.downloadedOnly,
                onChange = onDownloadedOnly
            )
            SettingRow(
                title = "Télécharger en Wi-Fi uniquement",
                description = "Hors Wi-Fi, AcroVox demande s'il faut télécharger tout de suite ou attendre le Wi-Fi.",
                checked = state.settings.wifiOnly,
                onChange = onWifiOnly
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AcroVoxTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.brand)
        )
    }
}

@Composable
private fun EpisodeRow(
    item: DownloadWithEpisode,
    onOpen: () -> Unit,
    subtitle: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    val colors = AcroVoxTheme.colors
    val (episode, feed) = item.episode
    Card {
        Row(
            Modifier.clickable(onClick = onOpen).padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(
                episode.imageUrl ?: feed.imageUrl,
                contentDescription = null,
                shape = AcroVoxShape.ArtworkSmall,
                modifier = Modifier.size(56.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    feed.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle()
            }
            trailing()
        }
    }
}

@Composable
private fun ActiveRow(
    item: DownloadWithEpisode,
    onWifi: Boolean,
    onOpen: () -> Unit,
    onDownloadNow: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = AcroVoxTheme.colors
    val download = item.download
    val waitingForWifi = download.status == DownloadStatus.WAITING_FOR_WIFI && !onWifi
    EpisodeRow(
        item,
        onOpen = onOpen,
        subtitle = {
            val label = when {
                download.status == DownloadStatus.RUNNING -> {
                    val total = download.totalBytes
                    if (total != null && total > 0) {
                        "${formatBytes(download.bytesDownloaded)} sur ${formatBytes(total)}"
                    } else {
                        formatBytes(download.bytesDownloaded)
                    }
                }
                waitingForWifi -> "En attente du Wi-Fi"
                else -> "En attente"
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.accent)
            val total = download.totalBytes
            if (download.status == DownloadStatus.RUNNING && total != null && total > 0) {
                AcroVoxProgressBar(
                    progress = { download.bytesDownloaded.toFloat() / total },
                    height = 3.dp,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                if (waitingForWifi) {
                    TextButton(onClick = onDownloadNow) { Text("Maintenant", color = colors.brand) }
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        AcroVoxIcons.Close,
                        contentDescription = "Annuler le téléchargement",
                        tint = colors.textSecondary
                    )
                }
            }
        }
    )
}

@Composable
private fun FailedRow(item: DownloadWithEpisode, onOpen: () -> Unit, onRetry: () -> Unit, onRemove: () -> Unit) {
    val colors = AcroVoxTheme.colors
    EpisodeRow(
        item,
        onOpen = onOpen,
        subtitle = {
            Text(
                item.download.error ?: "Échec du téléchargement",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2
            )
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onRetry) { Text("Réessayer", color = colors.brand) }
                IconButton(onClick = onRemove) {
                    Icon(AcroVoxIcons.Close, contentDescription = "Retirer", tint = colors.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun CompletedRow(item: DownloadWithEpisode, onOpen: () -> Unit, onPlay: () -> Unit, onDelete: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val episode = item.episode.episode
    EpisodeRow(
        item,
        onOpen = onOpen,
        subtitle = {
            val parts = listOfNotNull(
                item.download.totalBytes?.let(::formatBytes),
                formatDuration(episode.durationMs),
                item.download.completedAt?.let { "téléchargé ${formatRelativeDate(it).lowercase()}" }
            )
            Text(
                parts.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when (episode.state) {
                    EpisodeState.PLAYED -> "Écouté"
                    EpisodeState.IN_PROGRESS -> "En cours d'écoute"
                    else -> "Non écouté"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (episode.state == EpisodeState.PLAYED) colors.textMuted else colors.accent
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(AcroVoxIcons.Delete, contentDescription = "Supprimer le fichier", tint = colors.textSecondary)
                }
                PlayPauseButton(isPlaying = false, onClick = onPlay, size = PlayButtonSize.Small)
            }
        }
    )
}
