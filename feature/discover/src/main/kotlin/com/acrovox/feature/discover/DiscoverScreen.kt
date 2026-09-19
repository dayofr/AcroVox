package com.acrovox.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.designsystem.component.AcroVoxSearchField
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.PodcastSearchResult

@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues,
    onOpenFeed: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoverContent(
        query = query,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onOpenFeed = onOpenFeed,
        contentPadding = contentPadding,
        modifier = modifier
    )
}

@Composable
internal fun DiscoverContent(
    query: String,
    state: DiscoverUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenFeed: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(
            Modifier.padding(horizontal = Spacing.screenMargin).padding(top = Spacing.gutter, bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Explorer", style = HeadlineLargeMobile, color = colors.textPrimary)
            AcroVoxSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Nom du podcast ou adresse du flux"
            )
        }
        val listPadding = PaddingValues(
            start = Spacing.screenMargin,
            end = Spacing.screenMargin,
            bottom = contentPadding.calculateBottomPadding() + Spacing.gutter
        )
        when (state) {
            DiscoverUiState.Idle -> Hint(
                "Cherchez un podcast par son nom, ou collez l'adresse de son flux ou de sa page web."
            )
            is DiscoverUiState.Url -> OpenUrlCard(state.url, onClick = { onOpenFeed(state.url) })
            DiscoverUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.brand)
            }
            is DiscoverUiState.Error -> Column(Modifier.padding(horizontal = Spacing.screenMargin)) {
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onRetry) { Text("Réessayer", color = colors.brand) }
            }
            is DiscoverUiState.Results -> if (state.results.isEmpty()) {
                Hint("Aucun podcast trouvé.")
            } else {
                LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(state.results, key = { it.id }) { result ->
                        SearchResultRow(result, onClick = { result.feedUrl?.let(onOpenFeed) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AcroVoxTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = Spacing.screenMargin)
    )
}

@Composable
private fun OpenUrlCard(url: String, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Surface(
        onClick = onClick,
        shape = AcroVoxShape.Artwork,
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.outlineActive),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin)
    ) {
        Row(
            Modifier.padding(Spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AcroVoxIcons.Link, contentDescription = null, tint = colors.brand)
            Column(Modifier.weight(1f)) {
                Text("Ouvrir ce flux", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: PodcastSearchResult, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val available = result.feedUrl != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = available, onClick = onClick)
            .alpha(if (available) 1f else 0.5f)
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            result.artworkUrl,
            contentDescription = null,
            shape = AcroVoxShape.ArtworkSmall,
            modifier = Modifier.size(56.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val details = if (available) {
                listOfNotNull(result.author, result.episodeCount?.let { "$it épisodes" }).joinToString(" • ")
            } else {
                "Flux non publié : collez l'adresse de sa page ou de son flux"
            }
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
