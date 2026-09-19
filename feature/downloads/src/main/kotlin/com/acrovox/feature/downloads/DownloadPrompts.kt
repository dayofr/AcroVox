package com.acrovox.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.download.DownloadManager
import com.acrovox.core.download.DownloadPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadPromptViewModel @Inject constructor(private val downloads: DownloadManager) : ViewModel() {
    val prompt = downloads.prompt

    fun downloadNow(ids: List<Long>) = viewModelScope.launch { downloads.downloadNow(ids) }

    fun waitForWifi(ids: List<Long>) = viewModelScope.launch { downloads.waitForWifi(ids) }

    fun download(id: Long) = viewModelScope.launch {
        downloads.dismissPrompt()
        downloads.request(listOf(id))
    }

    fun delete(id: Long) = viewModelScope.launch { downloads.delete(listOf(id)) }

    fun dismiss() = downloads.dismissPrompt()
}

/** Questions posées par les téléchargements, affichées au-dessus de n'importe quel écran. */
@Composable
fun DownloadPrompts(viewModel: DownloadPromptViewModel = hiltViewModel()) {
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    when (val p = prompt) {
        null -> Unit
        is DownloadPrompt.ChooseNetwork -> {
            val count = p.episodeIds.size
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text("Pas de Wi-Fi") },
                text = {
                    Text(
                        (if (count == 1) "Cet épisode" else "Ces $count épisodes") +
                            " peu${if (count == 1) "t" else "vent"} être téléchargé${if (count == 1) "" else "s"} " +
                            "maintenant avec les données mobiles, ou attendre le prochain Wi-Fi."
                    )
                },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        TextButton(onClick = { viewModel.waitForWifi(p.episodeIds) }) {
                            Text("Attendre le Wi-Fi", color = colors.brand)
                        }
                        TextButton(onClick = { viewModel.downloadNow(p.episodeIds) }) {
                            Text("Télécharger maintenant", color = colors.textPrimary)
                        }
                        TextButton(onClick = viewModel::dismiss) { Text("Annuler", color = colors.textSecondary) }
                    }
                }
            )
        }
        is DownloadPrompt.NotDownloaded -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Épisode non téléchargé") },
            text = {
                Text("La lecture hors connexion uniquement est activée : téléchargez l'épisode pour l'écouter.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.download(p.episodeId) }) { Text("Télécharger", color = colors.brand) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Annuler") } }
        )
        is DownloadPrompt.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Supprimer le fichier ?") },
            text = { Text("« ${p.title} » ne sera plus disponible hors connexion. L'épisode reste dans vos listes.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(p.episodeId) }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Annuler") } }
        )
    }
}
