package com.acrovox.feature.inbox

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.component.EpisodeActionButton
import com.acrovox.core.designsystem.component.EpisodeCard
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.format.htmlToPlainText
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing

@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmIgnoreRest by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(undo) {
        val action = undo ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(action.message, actionLabel = "Annuler", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undo(action) else viewModel.undoShown(action)
    }

    Box(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        InboxContent(
            state = state,
            bottomPadding = contentPadding.calculateBottomPadding(),
            onRefresh = viewModel::refresh,
            onFilter = viewModel::setFeedFilter,
            onKeep = { viewModel.keep(listOf(it)) },
            onIgnore = { viewModel.ignore(listOf(it)) },
            onOpen = onOpenEpisode,
            onPlay = onPlay,
            onToggleSelection = viewModel::toggleSelection,
            onIgnoreRest = { confirmIgnoreRest = true },
            selectionBar = {
                SelectionBar(
                    count = state.selection.size,
                    onClose = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onKeep = { viewModel.keep(state.selection.toList()) },
                    onIgnore = { viewModel.ignore(state.selection.toList()) },
                    onPlayed = { viewModel.markPlayed(state.selection.toList()) }
                )
            }
        )
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding())
        )
    }

    if (confirmIgnoreRest) {
        val count = state.items.size
        val scope = state.feedFilter?.let { id -> state.feeds.firstOrNull { it.first.id == id }?.first?.title }
        AlertDialog(
            onDismissRequest = { confirmIgnoreRest = false },
            title = { Text(if (count == 1) "Ignorer 1 épisode ?" else "Ignorer $count épisodes ?") },
            text = {
                Text(
                    listOfNotNull(
                        scope?.let { "Épisodes de « $it » encore dans la boîte." },
                        "Ils sortent de la boîte, restent visibles dans le filtre « Ignorés » de leur podcast, " +
                            "et sont signalés comme ignorés au serveur de synchronisation."
                    ).joinToString("\n\n")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmIgnoreRest = false
                    viewModel.ignoreRest()
                }) { Text("Ignorer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmIgnoreRest = false }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InboxContent(
    state: InboxUiState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRefresh: () -> Unit,
    onFilter: (Long?) -> Unit,
    onKeep: (Long) -> Unit,
    onIgnore: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onIgnoreRest: () -> Unit,
    selectionBar: @Composable () -> Unit
) {
    val colors = AcroVoxTheme.colors
    Column(Modifier.fillMaxSize()) {
        if (state.selecting) {
            selectionBar()
        } else {
            Row(
                Modifier.padding(horizontal = Spacing.screenMargin).padding(top = Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Boîte de réception", style = HeadlineLargeMobile, color = colors.textPrimary)
                    Text(
                        when (state.total) {
                            0 -> "Rien à trier"
                            1 -> "1 épisode à trier"
                            else -> "${state.total} épisodes à trier"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                if (state.items.isNotEmpty()) {
                    TextButton(onClick = onIgnoreRest) { Text("Ignorer le reste", color = colors.brand) }
                }
            }
        }
        if (state.feeds.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    AcroVoxFilterChip("Tous (${state.total})", selected = state.feedFilter == null, onClick = {
                        onFilter(null)
                    })
                }
                items(state.feeds, key = { it.first.id }) { (feed, count) ->
                    AcroVoxFilterChip("${feed.title} ($count)", selected = state.feedFilter == feed.id, onClick = {
                        onFilter(feed.id)
                    })
                }
            }
        }
        PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screenMargin,
                    end = Spacing.screenMargin,
                    top = Spacing.sm,
                    bottom =
                    bottomPadding + Spacing.gutter
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!state.loading && state.items.isEmpty()) {
                    item { EmptyInbox() }
                }
                items(state.items, key = { it.episode.id }) { item ->
                    SwipeableInboxItem(
                        item = item,
                        selected = item.episode.id in state.selection,
                        selecting = state.selecting,
                        onKeep = { onKeep(item.episode.id) },
                        onIgnore = { onIgnore(item.episode.id) },
                        onOpen = { onOpen(item.episode.id) },
                        onPlay = { onPlay(item.episode.id) },
                        onToggleSelection = { onToggleSelection(item.episode.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableInboxItem(
    item: EpisodeWithFeed,
    selected: Boolean,
    selecting: Boolean,
    onKeep: () -> Unit,
    onIgnore: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (episode, feed) = item
    val swipe = rememberSwipeToDismissBoxState()
    val description = remember(episode.id) { htmlToPlainText(episode.description) }
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onKeep()
                SwipeToDismissBoxValue.EndToStart -> onIgnore()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = { SwipeBackground(swipe.dismissDirection) },
        modifier = modifier
    ) {
        EpisodeCard(
            title = episode.title,
            podcastTitle = feed.title,
            dateLabel = formatRelativeDate(episode.pubDate),
            description = description,
            durationLabel = formatDuration(episode.durationMs),
            artworkUrl = episode.imageUrl ?: feed.imageUrl,
            selected = selected,
            onClick = if (selecting) onToggleSelection else onOpen,
            onLongClick = onToggleSelection,
            onPlayClick = onPlay,
            actions = {
                EpisodeActionButton(AcroVoxIcons.Ignore, "Ignorer", onClick = onIgnore)
                EpisodeActionButton(AcroVoxIcons.AddToQueue, "Garder dans la file", onClick = onKeep)
            }
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val colors = AcroVoxTheme.colors
    val (color, icon, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Quad(
            colors.brand,
            AcroVoxIcons.AddToQueue,
            "Garder",
            Alignment.CenterStart
        )
        SwipeToDismissBoxValue.EndToStart -> Quad(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            AcroVoxIcons.Ignore,
            "Ignorer",
            Alignment.CenterEnd
        )
        SwipeToDismissBoxValue.Settled -> return
    }
    Box(
        Modifier.fillMaxSize().background(color, AcroVoxShape.Artwork).padding(horizontal = Spacing.lg),
        contentAlignment = alignment
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val tint = if (direction == SwipeToDismissBoxValue.StartToEnd) colors.onBrand else colors.textPrimary
            Icon(icon, contentDescription = null, tint = tint)
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

private data class Quad(
    val color: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
    val label: String,
    val alignment: Alignment
)

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onKeep: () -> Unit,
    onIgnore: () -> Unit,
    onPlayed: () -> Unit
) {
    val colors = AcroVoxTheme.colors
    Row(
        Modifier.fillMaxWidth().background(
            colors.surfaceFloating
        ).padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(AcroVoxIcons.Close, contentDescription = "Terminer la sélection", tint = colors.textPrimary)
        }
        Text(
            "$count sélectionné${if (count > 1) "s" else ""}",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSelectAll) { Text("Tout", color = colors.textSecondary) }
        IconButton(onClick = onKeep) {
            Icon(AcroVoxIcons.AddToQueue, contentDescription = "Garder la sélection", tint = colors.brand)
        }
        IconButton(onClick = onIgnore) {
            Icon(AcroVoxIcons.Ignore, contentDescription = "Ignorer la sélection", tint = colors.textPrimary)
        }
        IconButton(onClick = onPlayed) {
            Icon(AcroVoxIcons.Check, contentDescription = "Marquer écouté", tint = colors.textPrimary)
        }
    }
}

@Composable
private fun EmptyInbox() {
    val colors = AcroVoxTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(AcroVoxIcons.Inbox, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(56.dp))
        Text("Tout est trié", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(
            "Les nouveaux épisodes arriveront ici au prochain rafraîchissement. Tirez vers le bas pour vérifier maintenant.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
