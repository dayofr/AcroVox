package com.acrovox.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.refresh.RefreshRepository
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.database.entity.FeedEntity
import com.acrovox.core.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val loading: Boolean = true,
    /** Épisodes affichés (filtre appliqué), du plus récent au plus ancien. */
    val items: List<EpisodeWithFeed> = emptyList(),
    /** Podcasts présents dans la boîte et nombre d'épisodes chacun. */
    val feeds: List<Pair<FeedEntity, Int>> = emptyList(),
    val total: Int = 0,
    val feedFilter: Long? = null,
    val selection: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false
) {
    val selecting: Boolean get() = selection.isNotEmpty()
}

/** Action annulable, affichée en snackbar. */
data class UndoableAction(val id: Long, val message: String, internal val undo: suspend () -> Unit)

/**
 * Tri de la boîte de réception : garder (va dans la file) ou ignorer (action gPodder `delete`).
 * Rien n'est téléchargé sans le bouton « Télécharger ».
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val episodes: EpisodeRepository,
    private val refresher: RefreshRepository,
    private val downloads: DownloadManager
) : ViewModel() {
    private val feedFilter = MutableStateFlow<Long?>(null)
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val _undo = MutableStateFlow<UndoableAction?>(null)
    val undo: StateFlow<UndoableAction?> = _undo.asStateFlow()

    val uiState: StateFlow<InboxUiState> = combine(
        episodes.observeInbox(),
        feedFilter,
        selection,
        refresher.isRefreshing
    ) { inbox, filter, selected, refreshing ->
        val feeds = inbox.groupBy {
            it.feed.id
        }.map { (_, list) -> list.first().feed to list.size }.sortedByDescending { it.second }
        // Un podcast vidé de la boîte n'est plus un filtre valable.
        val activeFilter = filter?.takeIf { id -> feeds.any { it.first.id == id } }
        val visible = if (activeFilter == null) inbox else inbox.filter { it.feed.id == activeFilter }
        val visibleIds = visible.mapTo(HashSet()) { it.episode.id }
        InboxUiState(
            false,
            visible,
            feeds,
            inbox.size,
            activeFilter,
            selected.filterTo(HashSet()) {
                it in visibleIds
            },
            refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun setFeedFilter(feedId: Long?) {
        feedFilter.value = feedId
        selection.value = emptySet()
    }

    fun refresh() {
        viewModelScope.launch { refresher.refreshAll() }
    }

    fun keep(episodeIds: List<Long>) = launchUndoable(
        if (episodeIds.size ==
            1
        ) {
            "Gardé dans la file"
        } else {
            "${episodeIds.size} épisodes gardés"
        }
    ) {
        val previous = episodes.statesOf(episodeIds)
        episodes.addToQueue(episodeIds)
        suspend { episodes.undoKeep(previous) }
    }

    /** Garder et télécharger : l'épisode va dans la file, le téléchargement est demandé. */
    fun keepAndDownload(episodeIds: List<Long>) = launchUndoable(
        if (episodeIds.size ==
            1
        ) {
            "Gardé, téléchargement demandé"
        } else {
            "${episodeIds.size} épisodes gardés et téléchargés"
        }
    ) {
        val previous = episodes.statesOf(episodeIds)
        episodes.addToQueue(episodeIds)
        downloads.request(episodeIds)
        suspend {
            downloads.cancel(episodeIds)
            episodes.undoKeep(previous)
        }
    }

    fun ignore(episodeIds: List<Long>) = launchUndoable(
        if (episodeIds.size ==
            1
        ) {
            "Épisode ignoré"
        } else {
            "${episodeIds.size} épisodes ignorés"
        }
    ) {
        val previous = episodes.statesOf(episodeIds)
        val actions = episodes.ignore(episodeIds)
        suspend { episodes.undoIgnore(previous, actions) }
    }

    /** Ignore tout ce qui reste affiché (le filtre par podcast s'applique). */
    fun ignoreRest() = ignore(uiState.value.items.map { it.episode.id })

    fun markPlayed(episodeIds: List<Long>) = launchUndoable(
        if (episodeIds.size ==
            1
        ) {
            "Marqué écouté"
        } else {
            "${episodeIds.size} épisodes marqués écoutés"
        }
    ) {
        val previous = episodes.statesOf(episodeIds)
        episodes.markPlayed(episodeIds)
        suspend {
            previous.entries.groupBy({
                it.value
            }, { it.key }).forEach { (state, ids) -> episodes.setStates(ids, state) }
        }
    }

    fun toggleSelection(episodeId: Long) {
        selection.value = selection.value.let { if (episodeId in it) it - episodeId else it + episodeId }
    }

    fun selectAll() {
        selection.value = uiState.value.items.mapTo(HashSet()) { it.episode.id }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun undo(action: UndoableAction) {
        if (_undo.value?.id == action.id) _undo.value = null
        viewModelScope.launch { action.undo() }
    }

    fun undoShown(action: UndoableAction) {
        if (_undo.value?.id == action.id) _undo.value = null
    }

    /** Exécute [block] ; la fonction qu'il renvoie annule l'action. */
    private fun launchUndoable(message: String, block: suspend () -> suspend () -> Unit) {
        selection.value = emptySet()
        viewModelScope.launch {
            val undo = block()
            _undo.value = UndoableAction(System.nanoTime(), message, undo)
        }
    }
}
