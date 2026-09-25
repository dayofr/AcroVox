package com.acrovox.core.download

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Délai avant de supprimer le fichier d'un épisode écouté. */
enum class CleanupDelay(val delayMs: Long?) {
    NEVER(null),
    IMMEDIATELY(0),
    ONE_DAY(DAY_MS),
    ONE_WEEK(7 * DAY_MS)
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

data class DownloadSettings(
    /** Télécharger seulement en Wi-Fi ; hors Wi-Fi, l'utilisateur choisit à chaque demande. */
    val wifiOnly: Boolean = true,
    /** Pas de streaming : seuls les épisodes téléchargés se lisent. */
    val downloadedOnly: Boolean = false,
    /**
     * Suppression des fichiers écoutés ; les favoris sont toujours gardés.
     * Défaut « jamais » : les téléchargements sont manuels, on ne supprime
     * rien sans action volontaire (opt-in dans l'écran Téléchargements).
     */
    val deleteAfterPlayed: CleanupDelay = CleanupDelay.NEVER
)

private val Context.downloadStore: DataStore<Preferences> by preferencesDataStore("downloads")

@Singleton
class DownloadSettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val downloadedOnly = booleanPreferencesKey("downloaded_only")
        val deleteAfterPlayed = stringPreferencesKey("delete_after_played")
    }

    val settings: Flow<DownloadSettings> = context.downloadStore.data.map { p ->
        val defaults = DownloadSettings()
        DownloadSettings(
            wifiOnly = p[Keys.wifiOnly] ?: defaults.wifiOnly,
            downloadedOnly = p[Keys.downloadedOnly] ?: defaults.downloadedOnly,
            deleteAfterPlayed = p[Keys.deleteAfterPlayed]
                ?.let { name -> CleanupDelay.entries.firstOrNull { it.name == name } }
                ?: defaults.deleteAfterPlayed
        )
    }

    suspend fun current(): DownloadSettings = settings.first()

    suspend fun setWifiOnly(value: Boolean) {
        context.downloadStore.edit { it[Keys.wifiOnly] = value }
    }

    suspend fun setDeleteAfterPlayed(value: CleanupDelay) {
        context.downloadStore.edit { it[Keys.deleteAfterPlayed] = value.name }
    }

    suspend fun setDownloadedOnly(value: Boolean) {
        context.downloadStore.edit { it[Keys.downloadedOnly] = value }
    }
}
