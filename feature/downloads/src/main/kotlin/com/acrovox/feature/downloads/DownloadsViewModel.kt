package com.acrovox.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.database.entity.DownloadWithEpisode
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.download.DownloadSettings
import com.acrovox.core.download.DownloadSettingsRepository
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilter(val label: String) { ALL("Tous"), UNPLAYED("Non écoutés") }

enum class DownloadSort(val label: String) { DATE("Par date"), SIZE("Par taille") }

data class DownloadsUiState(
    val loading: Boolean = true,
    /** En attente ou en cours. */
    val active: List<DownloadWithEpisode> = emptyList(),
    /** Terminés, filtre et tri appliqués. */
    val completed: List<DownloadWithEpisode> = emptyList(),
    val completedCount: Int = 0,
    val unplayedCount: Int = 0,
    val failed: List<DownloadWithEpisode> = emptyList(),
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    val onWifi: Boolean = false,
    val settings: DownloadSettings = DownloadSettings(),
    val filter: DownloadFilter = DownloadFilter.ALL,
    val sort: DownloadSort = DownloadSort.DATE
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloads: DownloadManager,
    private val settingsRepository: DownloadSettingsRepository
) : ViewModel() {
    private val filter = MutableStateFlow(DownloadFilter.ALL)
    private val sort = MutableStateFlow(DownloadSort.DATE)

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloads.observeDownloads(),
        downloads.observeUsedBytes(),
        downloads.unmetered,
        settingsRepository.settings,
        combine(filter, sort, ::Pair)
    ) { all, used, wifi, settings, (currentFilter, currentSort) ->
        val done = all.filter { it.download.status == DownloadStatus.COMPLETED }
        val unplayed = done.filter { it.episode.episode.state != EpisodeState.PLAYED }
        val shown = if (currentFilter == DownloadFilter.UNPLAYED) unplayed else done
        DownloadsUiState(
            loading = false,
            active = all.filter { it.download.status in ActiveStatuses },
            completed = when (currentSort) {
                DownloadSort.DATE -> shown
                DownloadSort.SIZE -> shown.sortedByDescending { it.download.totalBytes ?: 0 }
            },
            completedCount = done.size,
            unplayedCount = unplayed.size,
            failed = all.filter { it.download.status == DownloadStatus.FAILED },
            usedBytes = used,
            freeBytes = downloads.freeBytes(),
            onWifi = wifi,
            settings = settings,
            filter = currentFilter,
            sort = currentSort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun setFilter(value: DownloadFilter) {
        filter.value = value
    }

    fun toggleSort() {
        sort.value = if (sort.value == DownloadSort.DATE) DownloadSort.SIZE else DownloadSort.DATE
    }

    fun setWifiOnly(value: Boolean) = viewModelScope.launch { settingsRepository.setWifiOnly(value) }

    fun setDownloadedOnly(value: Boolean) = viewModelScope.launch { settingsRepository.setDownloadedOnly(value) }

    fun downloadNow(episodeId: Long) = viewModelScope.launch { downloads.downloadNow(listOf(episodeId)) }

    fun retry(episodeId: Long) = viewModelScope.launch { downloads.request(listOf(episodeId)) }

    fun cancel(episodeId: Long) = viewModelScope.launch { downloads.cancel(listOf(episodeId)) }

    fun delete(episodeId: Long) = viewModelScope.launch { downloads.delete(listOf(episodeId)) }

    private companion object {
        val ActiveStatuses = setOf(DownloadStatus.QUEUED, DownloadStatus.WAITING_FOR_WIFI, DownloadStatus.RUNNING)
    }
}
