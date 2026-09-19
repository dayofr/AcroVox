package com.acrovox.feature.settings.sync

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.sync.GpodderAuthException
import com.acrovox.core.sync.SyncAccount
import com.acrovox.core.sync.SyncAccountRepository
import com.acrovox.core.sync.SyncRepository
import com.acrovox.core.sync.SyncResult
import com.acrovox.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncUiState(
    val loading: Boolean = true,
    val account: SyncAccount? = null,
    val pendingActions: Int = 0,
    val busy: Boolean = false,
    /** Erreur de connexion, ou résultat du dernier envoi manuel. */
    val message: String? = null
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val repository: SyncRepository,
    private val scheduler: SyncScheduler,
    accounts: SyncAccountRepository
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SyncUiState> = combine(
        accounts.account,
        repository.observePendingActions(),
        busy,
        message
    ) { account, pending, isBusy, msg -> SyncUiState(false, account, pending, isBusy, msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun connect(server: String, username: String, password: String) = run {
        repository.connect(server, username, password, "AcroVox (${Build.MODEL})")
        scheduler.syncNow()
        null
    }

    fun syncNow() = run {
        when (val result = repository.sync()) {
            is SyncResult.Success -> buildString {
                append("Envoyé : ${result.actionsSent} action${if (result.actionsSent > 1) "s" else ""}")
                if (result.subscriptionsAdded + result.subscriptionsRemoved > 0) {
                    append(", abonnements +${result.subscriptionsAdded} −${result.subscriptionsRemoved}")
                }
            }
            is SyncResult.Failed -> result.message
            SyncResult.NotConnected -> "Aucun compte connecté"
        }
    }

    fun disconnect() = run {
        scheduler.stop()
        repository.disconnect()
        null
    }

    fun clearMessage() {
        message.value = null
    }

    private fun run(block: suspend () -> String?) {
        if (busy.value) return
        busy.value = true
        message.value = null
        viewModelScope.launch {
            message.value = try {
                block()
            } catch (e: GpodderAuthException) {
                "Identifiants refusés par le serveur"
            } catch (e: UnknownHostException) {
                "Serveur introuvable : vérifiez l'adresse"
            } catch (e: IOException) {
                e.message ?: "Erreur réseau"
            } finally {
                busy.value = false
            }
        }
    }
}
