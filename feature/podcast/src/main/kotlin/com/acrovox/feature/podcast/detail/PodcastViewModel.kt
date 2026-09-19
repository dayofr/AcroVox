package com.acrovox.feature.podcast.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.repository.EpisodeFilter
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.feature.podcast.navigation.PodcastRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PodcastUiState(
    val feed: FeedEntity? = null,
    val episodes: List<EpisodeEntity> = emptyList(),
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val queuedIds: Set<Long> = emptySet(),
    /** Vrai une fois le podcast supprimé : l'écran se ferme. */
    val unsubscribed: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subscriptions: SubscriptionRepository,
    private val episodes: EpisodeRepository
) : ViewModel() {
    private val feedId = savedStateHandle.toRoute<PodcastRoute>().feedId
    private val filter = MutableStateFlow(EpisodeFilter.ALL)
    private val unsubscribed = MutableStateFlow(false)

    val uiState: StateFlow<PodcastUiState> = combine(
        subscriptions.observeFeed(feedId),
        filter.flatMapLatest { episodes.observeByFeed(feedId, it) },
        filter,
        episodes.observeQueuedIds(),
        unsubscribed
    ) { feed, list, currentFilter, queued, gone ->
        PodcastUiState(feed, list, currentFilter, queued, gone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PodcastUiState())

    fun setFilter(value: EpisodeFilter) {
        filter.value = value
    }

    fun toggleQueue(episodeId: Long) = viewModelScope.launch {
        if (episodeId in
            uiState.value.queuedIds
        ) {
            episodes.removeFromQueue(listOf(episodeId))
        } else {
            episodes.addToQueue(listOf(episodeId))
        }
    }

    fun ignore(episodeId: Long) = viewModelScope.launch { episodes.ignore(listOf(episodeId)) }

    fun restore(episodeId: Long) = viewModelScope.launch { episodes.restore(listOf(episodeId)) }

    fun updateSettings(transform: (FeedEntity) -> FeedEntity) = viewModelScope.launch {
        uiState.value.feed?.let { subscriptions.updateSettings(transform(it)) }
    }

    fun unsubscribe() = viewModelScope.launch {
        subscriptions.unsubscribe(feedId)
        unsubscribed.value = true
    }
}
