package com.acrovox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.refresh.RefreshRepository
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.repository.EpisodeSort
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.dao.FeedWithNewCount
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.model.DownloadState
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

data class HomeUiState(
    val loading: Boolean = true,
    val subscriptions: List<FeedWithNewCount> = emptyList(),
    val resume: EpisodeWithFeed? = null,
    val latest: List<EpisodeWithFeed> = emptyList(),
    val queuedIds: Set<Long> = emptySet(),
    val sort: EpisodeSort = EpisodeSort.NEWEST_FIRST,
    val isRefreshing: Boolean = false,
    val downloads: Map<Long, DownloadState> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    subscriptions: SubscriptionRepository,
    private val episodes: EpisodeRepository,
    private val refresher: RefreshRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {
    private val sort = MutableStateFlow(EpisodeSort.NEWEST_FIRST)

    val uiState: StateFlow<HomeUiState> = combine(
        subscriptions.observeSubscriptions(),
        episodes.observeResume(),
        sort.flatMapLatest { s -> episodes.observeLatest(s) },
        episodes.observeQueuedIds(),
        combine(sort, refresher.isRefreshing, downloadManager.observeStates(), ::Triple)
    ) { feeds, resume, latest, queued, (currentSort, refreshing, downloads) ->
        HomeUiState(false, feeds, resume, latest, queued, currentSort, refreshing, downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        viewModelScope.launch { refresher.refreshAll() }
    }

    fun toggleSort() {
        sort.value = if (sort.value == EpisodeSort.NEWEST_FIRST) EpisodeSort.OLDEST_FIRST else EpisodeSort.NEWEST_FIRST
    }

    fun toggleDownload(episodeId: Long) {
        viewModelScope.launch { downloadManager.toggle(episodeId) }
    }

    fun toggleQueue(episodeId: Long) {
        viewModelScope.launch {
            if (episodeId in uiState.value.queuedIds) {
                episodes.removeFromQueue(listOf(episodeId))
            } else {
                episodes.addToQueue(listOf(episodeId))
            }
        }
    }
}
