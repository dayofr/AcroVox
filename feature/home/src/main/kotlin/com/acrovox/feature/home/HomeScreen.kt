package com.acrovox.feature.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.data.repository.EpisodeSort
import com.acrovox.core.database.dao.FeedWithNewCount
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.designsystem.component.AcroVoxProgressBar
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.DownloadButton
import com.acrovox.core.designsystem.component.EpisodeActionButton
import com.acrovox.core.designsystem.component.EpisodeCard
import com.acrovox.core.designsystem.component.PlayButtonSize
import com.acrovox.core.designsystem.component.PlayPauseButton
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.DownloadState
import java.time.LocalTime

/** Actions de l'accueil vers le reste de l'app. */
data class HomeActions(
    val onOpenPodcast: (feedId: Long) -> Unit = {},
    val onOpenEpisode: (episodeId: Long) -> Unit = {},
    val onPlay: (episodeId: Long) -> Unit = {},
    val onSeeAllSubscriptions: () -> Unit = {},
    val onExplore: () -> Unit = {}
)

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        actions = actions,
        onRefresh = viewModel::refresh,
        onToggleSort = viewModel::toggleSort,
        onToggleQueue = viewModel::toggleQueue,
        onToggleDownload = viewModel::toggleDownload,
        contentPadding = contentPadding,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    onRefresh: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleQueue: (Long) -> Unit,
    contentPadding: PaddingValues,
    onToggleDownload: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis()
) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxSize()
        ) {
            item { Header() }
            if (!state.loading && state.subscriptions.isEmpty()) {
                item { EmptyState(actions.onExplore) }
                return@LazyColumn
            }
            if (state.subscriptions.isNotEmpty()) {
                item { SubscriptionShelf(state.subscriptions, actions.onOpenPodcast, actions.onSeeAllSubscriptions) }
            }
            state.resume?.let { resume ->
                item {
                    ResumeCard(resume, onPlay = {
                        actions.onPlay(resume.episode.id)
                    }, onClick = { actions.onOpenEpisode(resume.episode.id) })
                }
            }
            item { LatestHeader(state.sort, onToggleSort) }
            items(state.latest, key = { it.episode.id }) { item ->
                val episode = item.episode
                val queued = episode.id in state.queuedIds
                EpisodeCard(
                    title = episode.title,
                    podcastTitle = item.feed.title,
                    dateLabel = formatRelativeDate(episode.pubDate, now),
                    durationLabel = formatDuration(episode.durationMs)?.replace(" h ", "h "),
                    artworkUrl = episode.imageUrl ?: item.feed.imageUrl,
                    onClick = { actions.onOpenEpisode(episode.id) },
                    onPlayClick = { actions.onPlay(episode.id) },
                    actions = {
                        EpisodeActionButton(
                            icon = if (queued) AcroVoxIcons.RemoveFromQueue else AcroVoxIcons.AddToQueue,
                            contentDescription = if (queued) "Retirer de la file" else "Ajouter à la file",
                            onClick = { onToggleQueue(episode.id) }
                        )
                        DownloadButton(
                            state.downloads[episode.id] ?: DownloadState.None,
                            onClick = { onToggleDownload(episode.id) }
                        )
                        EpisodeActionButton(AcroVoxIcons.Share, "Partager", onClick = { context.shareEpisode(item) })
                    },
                    modifier = Modifier.padding(horizontal = Spacing.screenMargin).animateItem()
                )
            }
        }
    }
}

@Composable
private fun Header() {
    val greeting = when (LocalTime.now().hour) {
        in 5..17 -> "Bonjour"
        else -> "Bonsoir"
    }
    Column(Modifier.padding(horizontal = Spacing.screenMargin).padding(top = Spacing.gutter)) {
        Text(greeting, style = MaterialTheme.typography.labelSmall, color = AcroVoxTheme.colors.textSecondary)
        Text("Podcasts", style = MaterialTheme.typography.headlineSmall, color = AcroVoxTheme.colors.textPrimary)
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = AcroVoxTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun SubscriptionShelf(feeds: List<FeedWithNewCount>, onOpen: (Long) -> Unit, onSeeAll: () -> Unit) {
    val colors = AcroVoxTheme.colors
    // Podcasts avec des épisodes à trier en premier.
    val ordered = feeds.sortedByDescending { it.newCount }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionTitle("Vos podcasts suivis") {
            TextButton(onClick = onSeeAll) { Text("Voir tout", color = colors.brand) }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter)
        ) {
            items(ordered, key = { it.feed.id }) { item -> ShelfItem(item, onClick = { onOpen(item.feed.id) }) }
        }
    }
}

@Composable
private fun ShelfItem(item: FeedWithNewCount, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val hasNew = item.newCount > 0
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp)) {
            val ring = if (hasNew) {
                Brush.linearGradient(
                    listOf(colors.brand, colors.accent)
                )
            } else {
                Brush.linearGradient(listOf(colors.outlineSubtle, colors.outlineSubtle))
            }
            Box(Modifier.size(64.dp).border(2.5.dp, ring, CircleShape).padding(4.dp)) {
                Artwork(
                    item.feed.imageUrl,
                    contentDescription = null,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (hasNew) {
                Text(
                    item.newCount.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onBrand,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(colors.brand, CircleShape)
                        .border(2.dp, colors.canvas, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            item.feed.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (hasNew) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}

@Composable
private fun ResumeCard(item: EpisodeWithFeed, onPlay: () -> Unit, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val episode = item.episode
    val duration = episode.durationMs
    val progress = if (duration != null && duration > 0) episode.positionMs.toFloat() / duration else 0f
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionTitle("Reprendre l'écoute")
        Surface(
            onClick = onClick,
            shape = AcroVoxShape.Card,
            color = colors.surfaceFloating,
            border = BorderStroke(1.dp, colors.outlineSubtle),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin)
        ) {
            Column(Modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(
                        episode.imageUrl ?: item.feed.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.feed.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.brand,
                            maxLines = 1
                        )
                        Text(
                            episode.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (duration != null) {
                            Text(
                                "${formatDuration(duration - episode.positionMs)} restantes",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    PlayPauseButton(isPlaying = false, onClick = onPlay, size = PlayButtonSize.Medium)
                }
                AcroVoxProgressBar(progress = { progress })
            }
        }
    }
}

@Composable
private fun LatestHeader(sort: EpisodeSort, onToggleSort: () -> Unit) {
    val colors = AcroVoxTheme.colors
    SectionTitle("Derniers épisodes", Modifier.padding(top = Spacing.sm)) {
        Surface(
            onClick = onToggleSort,
            shape = AcroVoxShape.Pill,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (sort == EpisodeSort.NEWEST_FIRST) "Récents d'abord" else "Anciens d'abord",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Icon(
                    AcroVoxIcons.ExpandMore,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onExplore: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(Spacing.screenMargin).padding(top = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(AcroVoxIcons.Podcast, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(56.dp))
        Text("Aucun podcast suivi", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(
            "Cherchez un podcast ou importez vos abonnements depuis la Bibliothèque.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.sm))
        Button(
            onClick = onExplore,
            shape = AcroVoxShape.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand)
        ) { Text("Trouver un podcast") }
    }
}

private fun android.content.Context.shareEpisode(item: EpisodeWithFeed) {
    val episode = item.episode
    val text = listOfNotNull(
        "${item.feed.title} : ${episode.title}",
        episode.link ?: episode.mediaUrl
    ).joinToString("\n")
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, "Partager l'épisode"))
}
