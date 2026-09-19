package com.acrovox.feature.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.opml.OpmlRepository
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.dao.FeedWithNewCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SubscriptionSort(val label: String) {
    ALPHABETICAL("A à Z"),
    LATEST_EPISODE("Dernier épisode"),
    TO_SORT("À trier")
}

data class LibraryUiState(
    val subscriptions: List<FeedWithNewCount> = emptyList(),
    val sort: SubscriptionSort = SubscriptionSort.ALPHABETICAL,
    val grid: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    subscriptions: SubscriptionRepository,
    private val opml: OpmlRepository
) : ViewModel() {
    private val sort = savedState.getStateFlow(KEY_SORT, SubscriptionSort.ALPHABETICAL)
    private val grid = savedState.getStateFlow(KEY_GRID, true)

    val uiState: StateFlow<LibraryUiState> = combine(subscriptions.observeSubscriptions(), sort, grid) { feeds, s, g ->
        LibraryUiState(sorted(feeds, s), s, g)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    /** Message ponctuel (résultat d'export), effacé une fois affiché. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setSort(value: SubscriptionSort) {
        savedState[KEY_SORT] = value
    }

    fun toggleGrid() {
        savedState[KEY_GRID] = !grid.value
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                val count = opml.export(uri)
                if (count == 1) "1 podcast exporté" else "$count podcasts exportés"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Export impossible : ${e.message}"
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun sorted(feeds: List<FeedWithNewCount>, sort: SubscriptionSort) = when (sort) {
        SubscriptionSort.ALPHABETICAL -> feeds.sortedBy { it.feed.title.lowercase() }
        SubscriptionSort.LATEST_EPISODE -> feeds.sortedByDescending { it.lastPubDate ?: 0 }
        SubscriptionSort.TO_SORT -> feeds.sortedWith(
            compareByDescending<FeedWithNewCount> {
                it.newCount
            }.thenBy { it.feed.title.lowercase() }
        )
    }

    private companion object {
        const val KEY_SORT = "sort"
        const val KEY_GRID = "grid"
    }
}
