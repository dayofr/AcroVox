package com.acrovox.feature.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.DownloadButton
import com.acrovox.core.designsystem.component.DurationBadge
import com.acrovox.core.designsystem.component.EqualizerIndicator
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.DownloadState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueScreen(
    contentPadding: PaddingValues,
    onOpenEpisode: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    // Copie locale pendant le glisser-déposer ; enregistrée au relâchement.
    var items by remember { mutableStateOf(state.queue) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.queue) { if (!dragging) items = state.queue }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply { add(to.index - HEADER_ITEMS, removeAt(from.index - HEADER_ITEMS)) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = Spacing.screenMargin,
            end = Spacing.screenMargin,
            top = Spacing.gutter,
            bottom = contentPadding.calculateBottomPadding() + Spacing.gutter
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("File de lecture", style = HeadlineLargeMobile, color = colors.textPrimary)
                    val total = formatDuration(state.remainingMs)
                    val count = if (items.size == 1) "1 épisode" else "${items.size} épisodes"
                    Text(
                        listOfNotNull(
                            count,
                            total?.let {
                                "$it au total"
                            }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                if (items.isNotEmpty()) {
                    TextButton(onClick = viewModel::shuffle) { Text("Aléatoire", color = colors.textSecondary) }
                    TextButton(onClick = { confirmClear = true }) { Text("Effacer tout", color = colors.brand) }
                }
            }
        }
        item(key = "continuous") {
            ContinuousPlaybackRow(state.continuousPlayback, viewModel::setContinuousPlayback)
        }
        if (!state.loading && items.isEmpty()) {
            item(key = "empty") {
                Text(
                    "La file est vide. Gardez des épisodes depuis la boîte de réception ou ajoutez-les depuis un podcast.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }
        }
        items(items, key = { it.episode.id }) { item ->
            ReorderableItem(reorderState, key = item.episode.id) { isDragging ->
                QueueRow(
                    item = item,
                    isCurrent = item.episode.id == state.player.episodeId,
                    isPlaying = item.episode.id == state.player.episodeId && state.player.isPlaying,
                    isDragging = isDragging,
                    onClick = { viewModel.play(item.episode.id) },
                    onOpen = { onOpenEpisode(item.episode.id) },
                    onRemove = { viewModel.remove(item.episode.id) },
                    download = state.downloads[item.episode.id] ?: DownloadState.None,
                    onDownload = { viewModel.toggleDownload(item.episode.id) },
                    handle = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = { dragging = true },
                                onDragStopped = {
                                    dragging = false
                                    viewModel.reorder(items.map { it.episode.id })
                                }
                            )
                        ) { Icon(AcroVoxIcons.DragHandle, contentDescription = "Déplacer", tint = colors.textMuted) }
                    }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Vider la file ?") },
            text = { Text("Les ${items.size} épisodes quittent la file. Ils restent gardés dans leur podcast.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text("Vider", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annuler") } }
        )
    }
}

/** Nombre d'éléments de la liste avant les épisodes (en-tête, lecture continue). */
private const val HEADER_ITEMS = 2

@Composable
private fun ContinuousPlaybackRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AcroVoxTheme.colors
    Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Lecture continue", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                "Enchaîner sur l'épisode suivant de la file.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.brand)
        )
    }
}

@Composable
private fun QueueRow(
    item: EpisodeWithFeed,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    download: DownloadState,
    onDownload: () -> Unit,
    handle: @Composable () -> Unit
) {
    val colors = AcroVoxTheme.colors
    val (episode, feed) = item
    Surface(
        shape = AcroVoxShape.Artwork,
        color = if (isCurrent) colors.surfaceFloating else colors.surfaceCard,
        border = BorderStroke(1.dp, if (isCurrent) colors.outlineActive else colors.outlineSubtle),
        modifier = Modifier.fillMaxWidth().then(
            if (isDragging) Modifier.shadow(16.dp, AcroVoxShape.Artwork) else Modifier
        )
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onOpen, onLongClickLabel = "Détails de l'épisode")
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(56.dp)) {
                Artwork(
                    episode.imageUrl ?: feed.imageUrl,
                    contentDescription = null,
                    shape = AcroVoxShape.ArtworkSmall,
                    modifier = Modifier.size(56.dp)
                )
                formatDuration(episode.durationMs)?.let {
                    DurationBadge(
                        it.replace(" min", "m"),
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (isCurrent) EqualizerIndicator(animate = isPlaying)
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    feed.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DownloadButton(download, onClick = onDownload)
            IconButton(onClick = onRemove) {
                Icon(
                    AcroVoxIcons.RemoveFromQueue,
                    contentDescription = "Retirer de la file",
                    tint = colors.textSecondary
                )
            }
            handle()
        }
    }
}
