package com.acrovox.core.download

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class DownloadSettings(
    /** Télécharger seulement en Wi-Fi ; hors Wi-Fi, l'utilisateur choisit à chaque demande. */
    val wifiOnly: Boolean = true,
    /** Pas de streaming : seuls les épisodes téléchargés se lisent. */
    val downloadedOnly: Boolean = false
)

private val Context.downloadStore: DataStore<Preferences> by preferencesDataStore("downloads")

@Singleton
class DownloadSettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val downloadedOnly = booleanPreferencesKey("downloaded_only")
    }

    val settings: Flow<DownloadSettings> = context.downloadStore.data.map { p ->
        val defaults = DownloadSettings()
        DownloadSettings(
            wifiOnly = p[Keys.wifiOnly] ?: defaults.wifiOnly,
            downloadedOnly = p[Keys.downloadedOnly] ?: defaults.downloadedOnly
        )
    }

    suspend fun current(): DownloadSettings = settings.first()

    suspend fun setWifiOnly(value: Boolean) {
        context.downloadStore.edit { it[Keys.wifiOnly] = value }
    }

    suspend fun setDownloadedOnly(value: Boolean) {
        context.downloadStore.edit { it[Keys.downloadedOnly] = value }
    }
}
