package com.acrovox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** État global de l'interface : le lecteur et le nombre d'épisodes à trier. */
@HiltViewModel
class AppViewModel @Inject constructor(val player: PlayerController, episodes: EpisodeRepository) : ViewModel() {
    val inboxCount: StateFlow<Int> = episodes.observeInboxCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
