package com.acrovox.feature.library.antennapod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.data.antennapod.AntennaPodSummary
import com.acrovox.core.designsystem.component.AcroVoxProgressBar
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing

@Composable
fun AntennaPodImportScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AntennaPodImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, enabled = state !is AntennaPodImportUiState.Importing) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text("Migrer depuis AntennaPod", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        when (val s = state) {
            AntennaPodImportUiState.Reading -> Centered { CircularProgressIndicator(color = colors.brand) }
            is AntennaPodImportUiState.ReadError -> Message(s.message)
            is AntennaPodImportUiState.Confirm -> Confirm(s, viewModel::import)
            is AntennaPodImportUiState.Importing -> Column(
                Modifier.padding(Spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    "Podcast migré : ${s.done} / ${s.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                AcroVoxProgressBar(progress = { s.done.toFloat() / s.total.coerceAtLeast(1) })
            }
            is AntennaPodImportUiState.Done -> Summary(s.result, onClose)
        }
    }
}

@Composable
private fun Confirm(state: AntennaPodImportUiState.Confirm, onImport: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(Modifier.padding(Spacing.screenMargin), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            "${state.feeds} podcasts, ${state.episodes} épisodes, ${state.queued} dans la file",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        Text(
            "Abonnements, écoutes, positions, file et favoris seront repris. Les podcasts déjà suivis sont ignorés.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Button(
            onClick = onImport,
            shape = AcroVoxShape.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Migrer", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun Summary(result: AntennaPodSummary, onClose: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(Modifier.padding(Spacing.screenMargin), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            "${result.feedsAdded} podcasts ajoutés",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )
        if (result.feedsSkipped > 0) {
            Text(
                "${result.feedsSkipped} déjà suivis",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
        Text(
            "${result.episodesPlayed} écoutés, ${result.episodesInProgress} en cours, " +
                "${result.queued} dans la file, ${result.favorites} favoris",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Button(
            onClick = onClose,
            shape = AcroVoxShape.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.gutter).height(48.dp)
        ) { Text("Terminé") }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        content()
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(Spacing.screenMargin)
    )
}
