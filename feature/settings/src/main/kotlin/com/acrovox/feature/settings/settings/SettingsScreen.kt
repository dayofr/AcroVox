package com.acrovox.feature.settings.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.data.settings.ThemeMode
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing

private val SkipOptions = listOf(5, 10, 15, 30, 60)
private val ThresholdOptions = listOf(10, 30, 60, 120)
private val RefreshOptions = listOf(1L, 2L, 6L, 12L, 24L)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text("Réglages", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Section("Lecture") {
                OptionRow("Reculer de", SkipOptions, state.playback.skipBackSeconds, {
                    "$it s"
                }, viewModel::setSkipBack)
                OptionRow(
                    "Avancer de",
                    SkipOptions,
                    state.playback.skipForwardSeconds,
                    { "$it s" },
                    viewModel::setSkipForward
                )
                ToggleRow(
                    "Lecture continue",
                    "Enchaîner sur l'épisode suivant de la file.",
                    state.playback.continuousPlayback,
                    viewModel::setContinuous
                )
                ToggleRow(
                    "Raccourcir les silences",
                    "Vitesse adaptative pendant les blancs.",
                    state.playback.skipSilence,
                    viewModel::setSkipSilence
                )
                ToggleRow(
                    "Normalisation du volume",
                    "Nivelle le volume entre les podcasts.",
                    state.playback.volumeNormalization,
                    viewModel::setVolumeNormalization
                )
                OptionRow(
                    "Épisode écouté quand il reste",
                    ThresholdOptions,
                    state.playback.playedThresholdSeconds,
                    { "Moins de $it s" },
                    viewModel::setPlayedThreshold
                )
            }
            Section("Actualisation") {
                OptionRow(
                    "Vérifier les nouveaux épisodes",
                    RefreshOptions,
                    state.refresh.intervalHours,
                    { if (it == 1L) "Toutes les heures" else "Toutes les $it h" },
                    viewModel::setRefreshInterval
                )
                TextButton(onClick = viewModel::refreshNow) {
                    Text("Actualiser maintenant", color = colors.brand)
                }
            }
            Section("Apparence") {
                val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                OptionRow(
                    "Thème",
                    modes,
                    state.theme.mode,
                    {
                        when (it) {
                            ThemeMode.SYSTEM -> "Système"
                            ThemeMode.LIGHT -> "Clair"
                            ThemeMode.DARK -> "Sombre"
                        }
                    },
                    viewModel::setThemeMode
                )
                ToggleRow(
                    "Material You",
                    "Couleurs issues du fond d'écran (Android 12+).",
                    state.theme.dynamicColor,
                    viewModel::setDynamicColor
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        content()
    }
}

@Composable
private fun <T> OptionRow(label: String, options: List<T>, selected: T, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    val colors = AcroVoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { option ->
                AcroVoxFilterChip(labelOf(option), selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = AcroVoxTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.brand)
        )
    }
}
