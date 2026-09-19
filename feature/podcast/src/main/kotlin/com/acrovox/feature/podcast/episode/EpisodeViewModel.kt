package com.acrovox.feature.podcast.episode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.model.DownloadState
import com.acrovox.feature.podcast.navigation.EpisodeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EpisodeUiState(
    val item: EpisodeWithFeed? = null,
    val queued: Boolean = false,
    val download: DownloadState = DownloadState.None
)

@HiltViewModel
class EpisodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodes: EpisodeRepository,
    private val downloads: DownloadManager
) : ViewModel() {
    private val episodeId = savedStateHandle.toRoute<EpisodeRoute>().episodeId

    val uiState: StateFlow<EpisodeUiState> = combine(
        episodes.observeEpisode(episodeId),
        episodes.observeQueuedIds(),
        downloads.observeState(episodeId)
    ) { item, queued, download -> EpisodeUiState(item, episodeId in queued, download) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EpisodeUiState())

    fun toggleQueue() = viewModelScope.launch {
        if (uiState.value.queued) {
            episodes.removeFromQueue(
                listOf(episodeId)
            )
        } else {
            episodes.addToQueue(listOf(episodeId))
        }
    }

    fun toggleDownload() = viewModelScope.launch { downloads.toggle(episodeId) }

    fun ignore() = viewModelScope.launch { episodes.ignore(listOf(episodeId)) }

    fun restore() = viewModelScope.launch { episodes.restore(listOf(episodeId)) }

    fun markPlayed() = viewModelScope.launch { episodes.markPlayed(listOf(episodeId)) }

    fun toggleFavorite() = viewModelScope.launch {
        uiState.value.item?.let { episodes.setFavorite(episodeId, !it.episode.isFavorite) }
    }
}
