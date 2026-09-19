package com.acrovox.feature.discover

import app.cash.turbine.test
import com.acrovox.core.data.repository.SearchRepository
import com.acrovox.core.model.PodcastSearchResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val result = PodcastSearchResult(1, "Underscore_", "Micode", null, "https://example.org/feed", "Tech", 128)
    private var calls = 0
    private var fail = false
    private val repository = object : SearchRepository {
        override suspend fun search(term: String): List<PodcastSearchResult> {
            calls++
            if (fail) error("Hors ligne")
            return listOf(result)
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun typing_debouncesThenSearches() = runTest(dispatcher) {
        val viewModel = DiscoverViewModel(repository)
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Idle)
            viewModel.onQueryChange("u")
            viewModel.onQueryChange("und")
            viewModel.onQueryChange("underscore")
            advanceTimeBy(500)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Loading)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Results(listOf(result)))
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun url_isOfferedWithoutSearching() = runTest(dispatcher) {
        val viewModel = DiscoverViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("feeds.acast.com/public/shows/x")
            advanceTimeBy(500)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Url("https://feeds.acast.com/public/shows/x"))
            assertThat(calls).isEqualTo(0)
        }
    }

    @Test
    fun error_thenRetry() = runTest(dispatcher) {
        fail = true
        val viewModel = DiscoverViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("underscore")
            advanceTimeBy(500)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Loading)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Error("Hors ligne"))
            fail = false
            viewModel.retry()
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Loading)
            assertThat(awaitItem()).isEqualTo(DiscoverUiState.Results(listOf(result)))
        }
    }
}
