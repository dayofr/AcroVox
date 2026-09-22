package com.acrovox.feature.settings.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acrovox.core.data.backup.BackupRepository
import com.acrovox.core.data.backup.StagedBackup
import com.acrovox.core.data.refresh.RefreshSettings
import com.acrovox.core.data.refresh.RefreshSettingsRepository
import com.acrovox.core.data.refresh.RefreshWorker
import com.acrovox.core.data.settings.PlaybackSettings
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.acrovox.core.data.settings.ThemeMode
import com.acrovox.core.data.settings.ThemeRepository
import com.acrovox.core.data.settings.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val playback: PlaybackSettings = PlaybackSettings(),
    val refresh: RefreshSettings = RefreshSettings(),
    val theme: ThemeSettings = ThemeSettings(),
    val appVersion: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playback: PlaybackSettingsRepository,
    private val refresh: RefreshSettingsRepository,
    private val themes: ThemeRepository,
    private val backup: BackupRepository
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        playback.settings,
        refresh.settings,
        themes.settings
    ) { playback, refresh, theme ->
        SettingsUiState(playback, refresh, theme, appVersion())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSkipBack(seconds: Int) = launch { playback.setSkipBack(seconds) }

    fun setSkipForward(seconds: Int) = launch { playback.setSkipForward(seconds) }

    fun setContinuous(value: Boolean) = launch { playback.setContinuousPlayback(value) }

    fun setSkipSilence(value: Boolean) = launch { playback.setSkipSilence(value) }

    fun setVolumeNormalization(value: Boolean) = launch { playback.setVolumeNormalization(value) }

    fun setPlayedThreshold(seconds: Int) = launch { playback.setPlayedThreshold(seconds) }

    fun setRefreshInterval(hours: Long) = launch {
        refresh.setIntervalHours(hours)
        RefreshWorker.schedule(context, hours)
    }

    fun refreshNow() = launch { RefreshWorker.runNow(context) }

    fun setThemeMode(mode: ThemeMode) = launch { themes.setMode(mode) }

    fun setDynamicColor(value: Boolean) = launch { themes.setDynamicColor(value) }

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage

    private val _pendingImport = MutableStateFlow<StagedBackup?>(null)
    val pendingImport: StateFlow<StagedBackup?> = _pendingImport

    fun exportBackup(uri: Uri) = launch {
        _backupMessage.value = null
        try {
            backup.export(uri)
            _backupMessage.value = "Sauvegarde exportée."
        } catch (e: Exception) {
            _backupMessage.value = e.message ?: "Échec de l'export."
        }
    }

    fun prepareImport(uri: Uri) = launch {
        _backupMessage.value = null
        try {
            _pendingImport.value = backup.stageImport(uri)
        } catch (e: Exception) {
            _backupMessage.value = e.message ?: "Fichier invalide."
        }
    }

    fun confirmImport() = launch { _pendingImport.value?.let { backup.import(it) } }

    fun cancelImport() = launch {
        _pendingImport.value?.let { backup.discard(it) }
        _pendingImport.value = null
    }

    fun backupMessageShown() {
        _backupMessage.value = null
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    private fun appVersion(): String = try {
        val packageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        "${packageInfo.versionName} ($versionCode)"
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
}
