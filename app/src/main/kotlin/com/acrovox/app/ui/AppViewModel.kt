package com.acrovox.app.ui

import androidx.lifecycle.ViewModel
import com.acrovox.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** État global de l'interface : le lecteur, partagé par tous les écrans. */
@HiltViewModel
class AppViewModel @Inject constructor(val player: PlayerController) : ViewModel()
