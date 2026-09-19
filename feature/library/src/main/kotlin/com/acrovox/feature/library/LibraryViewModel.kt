package com.acrovox.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.opml.OpmlRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(private val opml: OpmlRepository) : ViewModel() {
    /** Message ponctuel (résultat d'export), effacé une fois affiché. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
}
