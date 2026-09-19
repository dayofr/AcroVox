package com.acrovox.feature.podcast.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.data.repository.PreviewEpisode
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing

@Composable
fun PodcastPreviewScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastPreviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = Spacing.xs)) {
            Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
        }
        when (val s = state) {
            PreviewUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.brand)
            }
            is PreviewUiState.Error -> Column(Modifier.padding(Spacing.screenMargin)) {
                Text(
                    "Impossible d'ouvrir ce flux",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                TextButton(onClick = viewModel::retry) { Text("Réessayer", color = colors.brand) }
            }
            is PreviewUiState.Loaded -> PreviewContent(
                state = s,
                onSubscribe = viewModel::subscribe,
                bottomPadding = contentPadding.calculateBottomPadding()
            )
        }
    }
}

@Composable
private fun PreviewContent(
    state: PreviewUiState.Loaded,
    onSubscribe: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val colors = AcroVoxTheme.colors
    val preview = state.preview
    val description = remember(preview.description) {
        preview.description?.let { AnnotatedString.fromHtml(it).text.trim() }
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.screenMargin,
            end = Spacing.screenMargin,
            bottom =
            bottomPadding + Spacing.gutter
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(preview.imageUrl, contentDescription = null, modifier = Modifier.size(112.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        preview.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    preview.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        "${preview.episodeCount} épisodes",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
        item { SubscribeButton(state.isSubscribed, state.isSubscribing, onSubscribe) }
        if (!description.isNullOrEmpty()) {
            item {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item {
            Text(
                "Derniers épisodes",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
        items(preview.latestEpisodes) { episode -> PreviewEpisodeRow(episode) }
    }
}

@Composable
private fun SubscribeButton(isSubscribed: Boolean, isSubscribing: Boolean, onSubscribe: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val modifier = Modifier.fillMaxWidth().height(48.dp)
    if (isSubscribed) {
        OutlinedButton(
            onClick = {
            },
            enabled = false,
            shape = AcroVoxShape.Pill,
            border = BorderStroke(
                1.dp,
                colors.outlineSubtle
            ),
            modifier = modifier
        ) {
            Icon(AcroVoxIcons.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Text("  Abonné", color = colors.textPrimary)
        }
    } else {
        Button(
            onClick = onSubscribe,
            enabled = !isSubscribing,
            shape = AcroVoxShape.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
            modifier = modifier
        ) {
            if (isSubscribing) {
                CircularProgressIndicator(color = colors.onBrand, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text("S'abonner", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PreviewEpisodeRow(episode: PreviewEpisode) {
    val colors = AcroVoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            episode.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val meta = listOfNotNull(
            episode.pubDate?.let {
                formatRelativeDate(it)
            },
            formatDuration(episode.durationMs)
        ).joinToString(" • ")
        Text(meta, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        HorizontalDivider(Modifier.padding(top = Spacing.sm), color = colors.outlineSubtle)
    }
}
