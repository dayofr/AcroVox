package com.acrovox.feature.library.antennapod

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.acrovox.core.data.antennapod.AntennaPodBackup
import com.acrovox.core.data.antennapod.AntennaPodImporter
import com.acrovox.core.data.antennapod.AntennaPodSummary
import com.acrovox.feature.library.navigation.AntennaPodImportRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AntennaPodImportUiState {
    data object Reading : AntennaPodImportUiState

    data class ReadError(val message: String) : AntennaPodImportUiState

    data class Confirm(val feeds: Int, val episodes: Int, val queued: Int) : AntennaPodImportUiState

    data class Importing(val done: Int, val total: Int) : AntennaPodImportUiState

    data class Done(val result: AntennaPodSummary) : AntennaPodImportUiState
}

@HiltViewModel
class AntennaPodImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val importer: AntennaPodImporter
) : ViewModel() {
    private val uri = Uri.parse(savedStateHandle.toRoute<AntennaPodImportRoute>().uri)
    private var backupFile: File? = null

    private val _state = MutableStateFlow<AntennaPodImportUiState>(AntennaPodImportUiState.Reading)
    val state: StateFlow<AntennaPodImportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("antennapod", ".db", context.cacheDir).also { target ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { input.copyTo(it) }
                        } ?: throw IllegalArgumentException("Fichier illisible")
                    }
                }
                backupFile = file
                val backup = withContext(Dispatchers.IO) { AntennaPodBackup.read(file.absolutePath) }
                AntennaPodImportUiState.Confirm(backup.feeds.size, backup.items.size, backup.queueItemIds.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AntennaPodImportUiState.ReadError(e.message ?: "Sauvegarde illisible")
            }
        }
    }

    fun import() {
        val file = backupFile ?: return
        _state.value = AntennaPodImportUiState.Importing(0, 1)
        viewModelScope.launch {
            _state.value = try {
                var total = 1
                val result = importer.import(file.absolutePath) { progress ->
                    total = progress.total
                    _state.update {
                        if (it is AntennaPodImportUiState.Importing) {
                            it.copy(done = progress.done, total = total)
                        } else {
                            it
                        }
                    }
                }
                AntennaPodImportUiState.Done(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AntennaPodImportUiState.ReadError(e.message ?: "Import impossible")
            } finally {
                file.delete()
                backupFile = null
            }
        }
    }
}
