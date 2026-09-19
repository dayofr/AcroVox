package com.acrovox.feature.library.opml

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.designsystem.component.AcroVoxProgressBar
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing

@Composable
fun OpmlImportScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpmlImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, enabled = state !is OpmlImportUiState.Importing) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text("Importer des abonnements", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        when (val s = state) {
            OpmlImportUiState.Reading -> Centered { CircularProgressIndicator(color = colors.brand) }
            is OpmlImportUiState.ReadError -> Message(s.message)
            is OpmlImportUiState.Selecting -> Selection(
                s,
                viewModel::toggle,
                viewModel::toggleAll,
                viewModel::setInboxLatest,
                viewModel::import
            )
            is OpmlImportUiState.Importing -> Column(
                Modifier.padding(Spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    "Abonnement en cours : ${s.done} / ${s.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                AcroVoxProgressBar(progress = { s.done.toFloat() / s.total })
            }
            is OpmlImportUiState.Done -> Summary(s, onClose)
        }
    }
}

@Composable
private fun Selection(
    state: OpmlImportUiState.Selecting,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onInboxLatest: (Boolean) -> Unit,
    onImport: () -> Unit
) {
    val colors = AcroVoxTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = Spacing.screenMargin), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.outlines.size} podcasts dans le fichier",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onToggleAll) {
                Text(
                    if (state.selected.size ==
                        state.outlines.size
                    ) {
                        "Tout décocher"
                    } else {
                        "Tout cocher"
                    },
                    color = colors.brand
                )
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.outlines, key = { it.xmlUrl }) { outline ->
                CheckRow(outline.title, outline.xmlUrl, checked = outline.xmlUrl in state.selected, onToggle = {
                    onToggle(outline.xmlUrl)
                })
            }
        }
        CheckRow(
            title = "Mettre le dernier épisode de chaque podcast dans la boîte",
            subtitle = "Sinon, rien n'arrive dans la boîte avant le prochain épisode.",
            checked = state.inboxLatest,
            onToggle = { onInboxLatest(!state.inboxLatest) }
        )
        Button(
            onClick = onImport,
            enabled = state.selected.isNotEmpty(),
            shape = AcroVoxShape.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenMargin).height(48.dp)
        ) { Text("Importer ${state.selected.size} podcasts", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun CheckRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = {
            onToggle()
        }, colors = CheckboxDefaults.colors(checkedColor = colors.brand))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Summary(state: OpmlImportUiState.Done, onClose: () -> Unit) {
    val colors = AcroVoxTheme.colors
    val result = state.result
    LazyColumn(
        contentPadding = PaddingValues(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                "${result.subscribed} podcasts ajoutés",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            if (result.alreadySubscribed > 0) {
                Text(
                    "${result.alreadySubscribed} déjà suivis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
        if (result.failures.isNotEmpty()) {
            item {
                Text(
                    "${result.failures.size} en échec",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(result.failures) { (title, reason) ->
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        item {
            Button(
                onClick = onClose,
                shape = AcroVoxShape.Pill,
                colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.gutter).height(48.dp)
            ) { Text("Terminé") }
        }
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
