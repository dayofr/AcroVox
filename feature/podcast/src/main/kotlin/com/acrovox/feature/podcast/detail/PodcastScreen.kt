package com.acrovox.feature.podcast.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.data.repository.EpisodeFilter
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.EpisodeActionButton
import com.acrovox.core.designsystem.component.EpisodeCard
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.format.htmlToPlainText
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.EpisodeState

private val filterLabels = listOf(
    EpisodeFilter.ALL to "Tous",
    EpisodeFilter.UNPLAYED to "Non écoutés",
    EpisodeFilter.INBOX to "Dans la boîte",
    EpisodeFilter.PLAYED to "Écoutés",
    EpisodeFilter.IGNORED to "Ignorés"
)

@Composable
fun PodcastScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.unsubscribed) { if (state.unsubscribed) onBack() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var confirmUnsubscribe by rememberSaveable { mutableStateOf(false) }
    val colors = AcroVoxTheme.colors
    val feed = state.feed

    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        TopBar(onBack, onSettings = { showSettings = true }, onUnsubscribe = { confirmUnsubscribe = true })
        if (feed == null) return@Column
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { Header(feed) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(filterLabels) { (filter, label) ->
                        AcroVoxFilterChip(label, selected = state.filter == filter, onClick = {
                            viewModel.setFilter(filter)
                        })
                    }
                }
            }
            if (state.episodes.isEmpty()) {
                item {
                    Text(
                        "Aucun épisode dans ce filtre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.screenMargin)
                    )
                }
            }
            items(state.episodes, key = { it.id }) { episode ->
                val queued = episode.id in state.queuedIds
                val ignored = episode.state == EpisodeState.IGNORED
                val description = remember(episode.id) { htmlToPlainText(episode.description) }
                EpisodeCard(
                    title = episode.title,
                    podcastTitle = stateLabel(episode.state),
                    dateLabel = formatRelativeDate(episode.pubDate),
                    description = description,
                    durationLabel = formatDuration(episode.durationMs),
                    artworkUrl = episode.imageUrl ?: feed.imageUrl,
                    progress = episode.durationMs?.takeIf { episode.state == EpisodeState.IN_PROGRESS && it > 0 }
                        ?.let { episode.positionMs.toFloat() / it },
                    onClick = { onOpenEpisode(episode.id) },
                    onPlayClick = { onPlay(episode.id) },
                    actions = {
                        if (ignored) {
                            EpisodeActionButton(AcroVoxIcons.AddToQueue, "Reprendre cet épisode", onClick = {
                                viewModel.restore(episode.id)
                            })
                        } else {
                            EpisodeActionButton(
                                if (queued) AcroVoxIcons.RemoveFromQueue else AcroVoxIcons.AddToQueue,
                                if (queued) "Retirer de la file" else "Ajouter à la file",
                                onClick = { viewModel.toggleQueue(episode.id) }
                            )
                            EpisodeActionButton(AcroVoxIcons.Ignore, "Ignorer", onClick = {
                                viewModel.ignore(episode.id)
                            })
                        }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.screenMargin).animateItem()
                )
            }
        }
    }

    if (showSettings && feed != null) {
        SettingsSheet(feed, onUpdate = viewModel::updateSettings, onDismiss = { showSettings = false })
    }
    if (confirmUnsubscribe && feed != null) {
        AlertDialog(
            onDismissRequest = { confirmUnsubscribe = false },
            title = { Text("Se désabonner ?") },
            text = { Text("« ${feed.title} » et ses épisodes seront retirés de l'app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnsubscribe = false
                    viewModel.unsubscribe()
                }) { Text("Se désabonner", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmUnsubscribe = false }) { Text("Annuler") } }
        )
    }
}

private fun stateLabel(state: EpisodeState) = when (state) {
    EpisodeState.NEW -> "Dans la boîte"
    EpisodeState.AVAILABLE -> "Au catalogue"
    EpisodeState.UNPLAYED -> "Gardé"
    EpisodeState.IN_PROGRESS -> "En cours"
    EpisodeState.PLAYED -> "Écouté"
    EpisodeState.IGNORED -> "Ignoré"
}

@Composable
private fun TopBar(onBack: () -> Unit, onSettings: () -> Unit, onUnsubscribe: () -> Unit) {
    val colors = AcroVoxTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
        }
        Row(Modifier.weight(1f)) {}
        IconButton(onClick = onSettings) {
            Icon(AcroVoxIcons.Settings, contentDescription = "Réglages du podcast", tint = colors.textPrimary)
        }
        IconButton(onClick = {
            menu = true
        }) { Icon(AcroVoxIcons.More, contentDescription = "Plus", tint = colors.textPrimary) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Se désabonner") }, onClick = {
                menu = false
                onUnsubscribe()
            })
        }
    }
}

@Composable
private fun Header(feed: FeedEntity) {
    val colors = AcroVoxTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    val description = remember(feed.description) { htmlToPlainText(feed.description) }
    Column(
        Modifier.padding(horizontal = Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(feed.imageUrl, contentDescription = null, modifier = Modifier.size(112.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    feed.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                feed.author?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
                }
            }
        }
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Moins" else "Plus", color = colors.brand)
            }
        }
    }
}

private val speeds = listOf<Float?>(null, 0.8f, 1f, 1.2f, 1.5f, 1.8f, 2f)
private val skipSteps = listOf(0L, 10L, 15L, 30L, 45L, 60L, 90L, 120L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(feed: FeedEntity, onUpdate: ((FeedEntity) -> FeedEntity) -> Unit, onDismiss: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onUpdate { it.copy(notifyNewEpisodes = true) }
        }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceModal) {
        Column(
            Modifier.padding(horizontal = Spacing.screenMargin).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                "Réglages de « ${feed.title} »",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            SettingChips("Vitesse de lecture", speeds, feed.playbackSpeed, label = {
                it?.let { s -> "${s}x".replace(".0x", "x") }
                    ?: "Globale"
            }) { speed ->
                onUpdate { it.copy(playbackSpeed = speed) }
            }
            SettingChips("Sauter l'introduction", skipSteps, feed.skipIntroMs / 1000, label = {
                if (it ==
                    0L
                ) {
                    "Non"
                } else {
                    "$it s"
                }
            }) { s ->
                onUpdate { it.copy(skipIntroMs = s * 1000) }
            }
            SettingChips("Sauter la fin", skipSteps, feed.skipOutroMs / 1000, label = {
                if (it ==
                    0L
                ) {
                    "Non"
                } else {
                    "$it s"
                }
            }) { s ->
                onUpdate { it.copy(skipOutroMs = s * 1000) }
            }
            HorizontalDivider(color = colors.outlineSubtle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notifier les nouveaux épisodes",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        "Une notification après le rafraîchissement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = feed.notifyNewEpisodes,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdate { it.copy(notifyNewEpisodes = enabled) }
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.brand)
                )
            }
        }
    }
}

@Composable
private fun <T> SettingChips(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AcroVoxTheme.colors.textPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(options) { option ->
                AcroVoxFilterChip(label(option), selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}
