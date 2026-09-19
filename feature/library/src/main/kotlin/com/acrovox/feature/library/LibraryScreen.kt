package com.acrovox.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.database.dao.FeedWithNewCount
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.feature.library.episodes.EpisodeListKind

private val OpmlMimeTypes = arrayOf("text/x-opml", "text/xml", "application/xml", "application/octet-stream", "*/*")
private const val EXPORT_FILE_NAME = "acrovox-abonnements.opml"

data class LibraryActions(
    val onOpenPodcast: (Long) -> Unit = {},
    val onOpenEpisodeList: (EpisodeListKind) -> Unit = {},
    val onImportOpml: (String) -> Unit = {}
)

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { actions.onImportOpml(it.toString()) }
    }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-opml")) { uri ->
            uri?.let(viewModel::export)
        }
    val colors = AcroVoxTheme.colors
    Box(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(
                start = Spacing.screenMargin,
                end = Spacing.screenMargin,
                top = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.gutter
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            fullWidth { Text("Bibliothèque", style = HeadlineLargeMobile, color = colors.textPrimary) }
            fullWidth {
                Column {
                    LibraryAction(AcroVoxIcons.Favorite, "Favoris", null) {
                        actions.onOpenEpisodeList(EpisodeListKind.FAVORITES)
                    }
                    LibraryAction(AcroVoxIcons.Queue, "Historique", null) {
                        actions.onOpenEpisodeList(EpisodeListKind.HISTORY)
                    }
                }
            }
            fullWidth {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Abonnements (${state.subscriptions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::toggleGrid) {
                        Icon(
                            if (state.grid) AcroVoxIcons.Queue else AcroVoxIcons.Library,
                            contentDescription = if (state.grid) "Afficher en liste" else "Afficher en grille",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
            fullWidth {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(SubscriptionSort.entries) { sort ->
                        AcroVoxFilterChip(sort.label, selected = state.sort == sort, onClick = {
                            viewModel.setSort(sort)
                        })
                    }
                }
            }
            if (state.grid) {
                items(state.subscriptions, key = {
                    it.feed.id
                }) { item -> GridTile(item, onClick = { actions.onOpenPodcast(item.feed.id) }) }
            } else {
                items(state.subscriptions, key = { it.feed.id }, span = { GridItemSpan(maxLineSpan) }) { item ->
                    ListRow(item, onClick = { actions.onOpenPodcast(item.feed.id) })
                }
            }
            fullWidth {
                Column(Modifier.padding(top = Spacing.lg)) {
                    Text(
                        "Importer et exporter",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    LibraryAction(
                        AcroVoxIcons.Download,
                        "Importer des abonnements",
                        "Fichier OPML exporté d'AntennaPod ou d'une autre app"
                    ) { importLauncher.launch(OpmlMimeTypes) }
                    LibraryAction(
                        AcroVoxIcons.Share,
                        "Exporter mes abonnements",
                        "Fichier OPML lisible par toutes les apps de podcast"
                    ) { exportLauncher.launch(EXPORT_FILE_NAME) }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun GridTile(item: FeedWithNewCount, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(Modifier.clickable(onClick = onClick)) {
        Box {
            Artwork(item.feed.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            if (item.newCount > 0) NewBadge(item.newCount, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Text(
            item.feed.title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

@Composable
private fun ListRow(item: FeedWithNewCount, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            item.feed.imageUrl,
            contentDescription = null,
            shape = AcroVoxShape.ArtworkSmall,
            modifier = Modifier.size(56.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.feed.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val details = listOfNotNull(
                item.feed.author,
                item.lastPubDate?.let {
                    "Dernier : ${formatRelativeDate(it)}"
                }
            ).joinToString(" • ")
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.newCount > 0) NewBadge(item.newCount)
    }
}

@Composable
private fun NewBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = AcroVoxTheme.colors
    Text(
        count.coerceAtMost(99).toString(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onBrand,
        modifier = modifier
            .background(colors.brand, CircleShape)
            .border(2.dp, colors.canvas, CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun LibraryAction(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = colors.brand) },
        colors = ListItemDefaults.colors(
            containerColor = colors.canvas,
            headlineColor = colors.textPrimary,
            supportingColor = colors.textSecondary
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
