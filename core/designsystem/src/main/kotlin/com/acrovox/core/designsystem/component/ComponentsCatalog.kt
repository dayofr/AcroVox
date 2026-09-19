package com.acrovox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.model.DownloadState

/**
 * Planche des composants partagés, dans des états figés.
 * Sert aux préviews et aux tests de capture d'écran.
 */
@Composable
fun ComponentsCatalog(modifier: Modifier = Modifier) {
    val colors = AcroVoxTheme.colors
    Column(
        modifier = modifier
            .background(colors.canvas)
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.gutter)
    ) {
        AcroVoxSearchField(query = "", onQueryChange = {}, placeholder = "Rechercher un podcast")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AcroVoxFilterChip("Tendances", selected = true, onClick = {})
            AcroVoxFilterChip("Nouveautés", selected = false, onClick = {})
            AcroVoxFilterChip("Tech & IA", selected = false, onClick = {})
        }
        EpisodeCard(
            title = "Taïwan, semi-conducteurs et tensions géopolitiques",
            podcastTitle = "L'Heure du Monde",
            dateLabel = "Il y a 2 h",
            description = "Analyse des enjeux stratégiques maritimes et technologiques " +
                "qui redéfinissent l'équilibre mondial.",
            durationLabel = "28 min",
            onClick = {},
            onPlayClick = {},
            badges = { DownloadedBadge() },
            actions = {
                EpisodeActionButton(AcroVoxIcons.AddToQueue, "Ajouter à la file", onClick = {})
                EpisodeActionButton(AcroVoxIcons.Ignore, "Ignorer", onClick = {})
            }
        )
        EpisodeCard(
            title = "IA & Futur du travail",
            podcastTitle = "Underscore_",
            dateLabel = "Hier",
            durationLabel = "52 min",
            isPlaying = true,
            progress = 0.65f,
            onClick = {},
            onPlayClick = {},
            actions = { DownloadButton(DownloadState.Running(0.4f), onClick = {}) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DownloadButton(DownloadState.None, onClick = {})
            DownloadButton(DownloadState.Queued(waitingForWifi = true), onClick = {})
            DownloadButton(DownloadState.Queued(waitingForWifi = false), onClick = {})
            DownloadButton(DownloadState.Running(null), onClick = {})
            DownloadButton(DownloadState.Completed, onClick = {})
            DownloadButton(DownloadState.Failed("404"), onClick = {})
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayPauseButton(isPlaying = false, onClick = {})
            PlayPauseButton(isPlaying = true, onClick = {}, size = PlayButtonSize.Medium)
            PlayPauseButton(isPlaying = false, onClick = {}, size = PlayButtonSize.Small)
            EqualizerIndicator(animate = false)
            DurationBadge("1h 18m")
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("34:10 / 52:10", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            AcroVoxProgressBar(progress = { 0.65f })
        }
    }
}

@Preview(name = "Sombre", widthDp = 400)
@Composable
private fun ComponentsCatalogDarkPreview() {
    AcroVoxTheme(darkTheme = true) { ComponentsCatalog(Modifier.fillMaxWidth()) }
}

@Preview(name = "Clair", widthDp = 400)
@Composable
private fun ComponentsCatalogLightPreview() {
    AcroVoxTheme(darkTheme = false) { ComponentsCatalog(Modifier.fillMaxWidth()) }
}
