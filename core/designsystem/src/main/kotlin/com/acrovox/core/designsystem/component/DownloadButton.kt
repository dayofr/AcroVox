package com.acrovox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.model.DownloadState

/**
 * Bouton de téléchargement d'un épisode. Son action dépend de l'état :
 * télécharger, annuler (en attente ou en cours), supprimer (téléchargé), réessayer (échec).
 */
@Composable
fun DownloadButton(state: DownloadState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AcroVoxTheme.colors
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        when (state) {
            DownloadState.None -> Icon(
                AcroVoxIcons.Download,
                contentDescription = "Télécharger",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            is DownloadState.Queued -> Icon(
                if (state.waitingForWifi) AcroVoxIcons.WaitingForWifi else AcroVoxIcons.Pending,
                contentDescription = if (state.waitingForWifi) {
                    "En attente du Wi-Fi, annuler"
                } else {
                    "En attente, annuler"
                },
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            is DownloadState.Running -> Box(contentAlignment = Alignment.Center) {
                val progress = state.progress
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = colors.brand,
                        trackColor = colors.outlineSubtle
                    )
                } else {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = colors.brand)
                }
                Icon(
                    AcroVoxIcons.Stop,
                    contentDescription = "Annuler le téléchargement",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
            DownloadState.Completed -> Icon(
                AcroVoxIcons.Downloaded,
                contentDescription = "Téléchargé, supprimer le fichier",
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            is DownloadState.Failed -> Icon(
                AcroVoxIcons.Error,
                contentDescription = "Échec du téléchargement, réessayer",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
