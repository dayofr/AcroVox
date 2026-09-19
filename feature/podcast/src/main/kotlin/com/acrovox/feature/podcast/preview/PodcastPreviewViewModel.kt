package com.acrovox.feature.podcast.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.repository.FeedPreview
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.feature.podcast.navigation.PodcastPreviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PreviewUiState {
    data object Loading : PreviewUiState

    data class Error(val message: String) : PreviewUiState

    data class Loaded(val preview: FeedPreview, val isSubscribed: Boolean, val isSubscribing: Boolean) : PreviewUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PodcastPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SubscriptionRepository
) : ViewModel() {
    private val feedUrl = savedStateHandle.toRoute<PodcastPreviewRoute>().feedUrl

    private val load = MutableStateFlow<Result<FeedPreview>?>(null)
    private val subscribing = MutableStateFlow(false)

    val uiState: StateFlow<PreviewUiState> = load.flatMapLatest { result ->
        when {
            result == null -> flowOf(PreviewUiState.Loading)
            result.isFailure -> flowOf(PreviewUiState.Error(result.exceptionOrNull()?.message ?: "Flux illisible"))
            else -> {
                val preview = result.getOrThrow()
                combine(repository.observeIsSubscribed(preview.feedUrl), subscribing) { subscribed, busy ->
                    PreviewUiState.Loaded(preview, subscribed, busy)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreviewUiState.Loading)

    init {
        fetch()
    }

    fun retry() = fetch()

    fun subscribe() {
        val preview = load.value?.getOrNull() ?: return
        if (subscribing.value) return
        viewModelScope.launch {
            subscribing.value = true
            try {
                repository.subscribe(preview)
            } finally {
                subscribing.value = false
            }
        }
    }

    private fun fetch() {
        load.value = null
        viewModelScope.launch {
            load.value = try {
                Result.success(repository.preview(feedUrl))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
