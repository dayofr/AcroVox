package com.acrovox.feature.library.episodes

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.designsystem.component.EpisodeCard
import com.acrovox.core.designsystem.format.formatDuration
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.feature.library.navigation.EpisodeListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Keep
enum class EpisodeListKind(val title: String, val empty: String) {
    FAVORITES("Favoris", "Aucun favori. Ajoutez-en depuis la page d'un épisode."),
    HISTORY("Historique", "L'historique se remplit quand vous écoutez des épisodes.")
}

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodes: EpisodeRepository
) : ViewModel() {
    val kind = savedStateHandle.toRoute<EpisodeListRoute>().kind

    val items: StateFlow<List<EpisodeWithFeed>?> = when (kind) {
        EpisodeListKind.FAVORITES -> episodes.observeFavorites()
        EpisodeListKind.HISTORY -> episodes.observeHistory()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearHistory() = viewModelScope.launch { episodes.clearHistory() }
}

@Composable
fun EpisodeListScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeListViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text(
                viewModel.kind.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.kind == EpisodeListKind.HISTORY && !items.isNullOrEmpty()) {
                TextButton(onClick = viewModel::clearHistory) { Text("Effacer", color = colors.brand) }
            }
        }
        val list = items ?: return@Column
        if (list.isEmpty()) {
            Text(
                viewModel.kind.empty,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(Spacing.screenMargin)
            )
            return@Column
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
            items(list, key = { it.episode.id }) { (episode, feed) ->
                EpisodeCard(
                    title = episode.title,
                    podcastTitle = feed.title,
                    dateLabel = formatRelativeDate(episode.pubDate),
                    durationLabel = formatDuration(episode.durationMs),
                    artworkUrl = episode.imageUrl ?: feed.imageUrl,
                    onClick = { onOpenEpisode(episode.id) },
                    onPlayClick = { onPlay(episode.id) }
                )
            }
        }
    }
}
