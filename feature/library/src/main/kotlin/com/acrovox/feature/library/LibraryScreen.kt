package com.acrovox.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.HeadlineLargeMobile
import com.acrovox.core.designsystem.theme.Spacing

private val OpmlMimeTypes = arrayOf("text/x-opml", "text/xml", "application/xml", "application/octet-stream", "*/*")
private const val EXPORT_FILE_NAME = "acrovox-abonnements.opml"

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    onImportOpml: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportOpml(it.toString()) }
    }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-opml")) { uri ->
            uri?.let(viewModel::export)
        }
    val colors = AcroVoxTheme.colors
    Box(modifier.fillMaxSize().padding(contentPadding)) {
        Column(Modifier.padding(vertical = Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                "Bibliothèque",
                style = HeadlineLargeMobile,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = Spacing.screenMargin)
            )
            Text(
                "Importer et exporter",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = Spacing.screenMargin).padding(top = Spacing.lg)
            )
            LibraryAction(
                icon = AcroVoxIcons.Download,
                title = "Importer des abonnements",
                subtitle = "Fichier OPML exporté d'AntennaPod ou d'une autre app",
                onClick = { importLauncher.launch(OpmlMimeTypes) }
            )
            LibraryAction(
                icon = AcroVoxIcons.Share,
                title = "Exporter mes abonnements",
                subtitle = "Fichier OPML lisible par toutes les apps de podcast",
                onClick = { exportLauncher.launch(EXPORT_FILE_NAME) }
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LibraryAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, contentDescription = null, tint = colors.brand) },
        colors = ListItemDefaults.colors(
            containerColor = colors.canvas,
            headlineColor = colors.textPrimary,
            supportingColor = colors.textSecondary
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
