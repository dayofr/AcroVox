package com.acrovox.feature.library.opml

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.opml.OpmlImportResult
import com.acrovox.core.data.opml.OpmlOutline
import com.acrovox.core.data.opml.OpmlRepository
import com.acrovox.feature.library.navigation.OpmlImportRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OpmlImportUiState {
    data object Reading : OpmlImportUiState

    data class ReadError(val message: String) : OpmlImportUiState

    data class Selecting(val outlines: List<OpmlOutline>, val selected: Set<String>, val inboxLatest: Boolean = false) :
        OpmlImportUiState

    data class Importing(val done: Int, val total: Int) : OpmlImportUiState

    data class Done(val result: OpmlImportResult) : OpmlImportUiState
}

@HiltViewModel
class OpmlImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OpmlRepository
) : ViewModel() {
    private val uri = Uri.parse(savedStateHandle.toRoute<OpmlImportRoute>().uri)

    private val _state = MutableStateFlow<OpmlImportUiState>(OpmlImportUiState.Reading)
    val state: StateFlow<OpmlImportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val outlines = repository.read(uri)
                OpmlImportUiState.Selecting(outlines, selected = outlines.mapTo(HashSet()) { it.xmlUrl })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                OpmlImportUiState.ReadError(e.message ?: "Fichier illisible")
            }
        }
    }

    fun toggle(url: String) = updateSelecting {
        it.copy(
            selected = if (url in
                it.selected
            ) {
                it.selected - url
            } else {
                it.selected + url
            }
        )
    }

    fun toggleAll() = updateSelecting {
        it.copy(
            selected = if (it.selected.size ==
                it.outlines.size
            ) {
                emptySet()
            } else {
                it.outlines.mapTo(HashSet()) { o -> o.xmlUrl }
            }
        )
    }

    fun setInboxLatest(value: Boolean) = updateSelecting { it.copy(inboxLatest = value) }

    fun import() {
        val selecting = _state.value as? OpmlImportUiState.Selecting ?: return
        val chosen = selecting.outlines.filter { it.xmlUrl in selecting.selected }
        if (chosen.isEmpty()) return
        _state.value = OpmlImportUiState.Importing(0, chosen.size)
        viewModelScope.launch {
            val result = repository.import(chosen, selecting.inboxLatest) { done ->
                _state.update { if (it is OpmlImportUiState.Importing) it.copy(done = maxOf(it.done, done)) else it }
            }
            _state.value = OpmlImportUiState.Done(result)
        }
    }

    private fun updateSelecting(transform: (OpmlImportUiState.Selecting) -> OpmlImportUiState.Selecting) {
        _state.update { if (it is OpmlImportUiState.Selecting) transform(it) else it }
    }
}
