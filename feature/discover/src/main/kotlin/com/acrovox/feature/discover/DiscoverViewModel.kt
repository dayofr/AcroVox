package com.acrovox.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.repository.SearchRepository
import com.acrovox.core.model.PodcastSearchResult
import com.acrovox.core.model.looksLikeUrl
import com.acrovox.core.model.normalizeFeedUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DiscoverUiState {
    data object Idle : DiscoverUiState

    /** La saisie est une adresse : proposer de l'ouvrir directement. */
    data class Url(val url: String) : DiscoverUiState

    data object Loading : DiscoverUiState

    data class Results(val results: List<PodcastSearchResult>) : DiscoverUiState

    data class Error(val message: String) : DiscoverUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(private val searchRepository: SearchRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Incrémenté par « Réessayer » pour relancer la même recherche. */
    private val retries = MutableStateFlow(0)

    val uiState: StateFlow<DiscoverUiState> = combine(
        _query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
        retries
    ) { query, _ -> query }
        .flatMapLatest { query ->
            when {
                looksLikeUrl(
                    query
                ) -> flowOf(normalizeFeedUrl(query)?.let(DiscoverUiState::Url) ?: DiscoverUiState.Idle)
                query.length < MIN_QUERY_LENGTH -> flowOf(DiscoverUiState.Idle)
                else -> search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiscoverUiState.Idle)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun retry() {
        retries.value++
    }

    private fun search(query: String) = flow {
        emit(DiscoverUiState.Loading)
        val state = try {
            DiscoverUiState.Results(searchRepository.search(query))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DiscoverUiState.Error(e.message ?: "Recherche impossible")
        }
        emit(state)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_QUERY_LENGTH = 2
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
