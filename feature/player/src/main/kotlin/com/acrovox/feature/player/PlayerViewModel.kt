package com.acrovox.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.player.PlayerController
import com.acrovox.core.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerScreenState(
    val player: PlayerState = PlayerState(),
    val episode: EpisodeWithFeed? = null,
    val queued: Boolean = false,
    val skipSilence: Boolean = false,
    val skipBackSeconds: Int = 10,
    val skipForwardSeconds: Int = 30
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val controller: PlayerController,
    private val episodes: EpisodeRepository,
    private val settings: PlaybackSettingsRepository
) : ViewModel() {
    private val episode = controller.state.map { it.episodeId }.distinctUntilChanged()
        .flatMapLatest { id -> id?.let(episodes::observeEpisode) ?: flowOf(null) }

    val uiState: StateFlow<PlayerScreenState> = combine(
        controller.state,
        episode,
        episodes.observeQueuedIds(),
        settings.settings
    ) { player, item, queued, s ->
        PlayerScreenState(
            player,
            item,
            player.episodeId in queued,
            s.skipSilence,
            s.skipBackSeconds,
            s.skipForwardSeconds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerScreenState(controller.state.value))

    fun toggleFavorite() = viewModelScope.launch {
        uiState.value.episode?.let { episodes.setFavorite(it.episode.id, !it.episode.isFavorite) }
    }

    fun toggleQueue() = viewModelScope.launch {
        val id = uiState.value.player.episodeId ?: return@launch
        if (uiState.value.queued) episodes.removeFromQueue(listOf(id)) else episodes.addToQueue(listOf(id))
    }

    fun setSkipSilence(value: Boolean) = viewModelScope.launch { settings.setSkipSilence(value) }
}
