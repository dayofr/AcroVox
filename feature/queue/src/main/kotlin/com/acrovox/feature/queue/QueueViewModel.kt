package com.acrovox.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.player.PlayerController
import com.acrovox.core.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QueueUiState(
    val loading: Boolean = true,
    val queue: List<EpisodeWithFeed> = emptyList(),
    val player: PlayerState = PlayerState(),
    val continuousPlayback: Boolean = true
) {
    /** Durée restante de la file, épisodes commencés comptés pour ce qu'il en reste. */
    val remainingMs: Long get() = queue.sumOf { (episode, _) ->
        ((episode.durationMs ?: 0) - episode.positionMs).coerceAtLeast(0)
    }
}

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val episodes: EpisodeRepository,
    private val settings: PlaybackSettingsRepository,
    val player: PlayerController
) : ViewModel() {
    val uiState: StateFlow<QueueUiState> = combine(
        episodes.observeQueue(),
        player.state,
        settings.settings
    ) { queue, playerState, s -> QueueUiState(false, queue, playerState, s.continuousPlayback) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueueUiState())

    fun play(episodeId: Long) = player.play(episodeId)

    fun remove(episodeId: Long) = viewModelScope.launch { episodes.removeFromQueue(listOf(episodeId)) }

    fun reorder(episodeIds: List<Long>) = viewModelScope.launch { episodes.reorderQueue(episodeIds) }

    fun shuffle() = viewModelScope.launch { episodes.shuffleQueue() }

    fun clear() = viewModelScope.launch { episodes.clearQueue() }

    fun setContinuousPlayback(value: Boolean) = viewModelScope.launch { settings.setContinuousPlayback(value) }
}
