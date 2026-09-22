package com.acrovox.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PlaybackSettings(
    /** Vitesse globale ; un podcast peut la remplacer. */
    val speed: Float = 1f,
    val skipBackSeconds: Int = 10,
    val skipForwardSeconds: Int = 30,
    val skipSilence: Boolean = false,
    /** Enchaîner sur l'épisode suivant de la file. */
    val continuousPlayback: Boolean = true,
    /** Normalisation du volume (LoudnessEnhancer). */
    val volumeNormalization: Boolean = false,
    /** Reste à écouter en dessous duquel l'épisode compte comme écouté. */
    val playedThresholdSeconds: Int = 30
)

private val Context.playbackStore: DataStore<Preferences> by preferencesDataStore("playback")

@Singleton
class PlaybackSettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val speed = floatPreferencesKey("speed")
        val skipBack = intPreferencesKey("skip_back")
        val skipForward = intPreferencesKey("skip_forward")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val continuous = booleanPreferencesKey("continuous")
        val volumeNormalization = booleanPreferencesKey("volume_normalization")
        val playedThreshold = intPreferencesKey("played_threshold")
    }

    val settings: Flow<PlaybackSettings> = context.playbackStore.data.map { p ->
        val defaults = PlaybackSettings()
        PlaybackSettings(
            speed = p[Keys.speed] ?: defaults.speed,
            skipBackSeconds = p[Keys.skipBack] ?: defaults.skipBackSeconds,
            skipForwardSeconds = p[Keys.skipForward] ?: defaults.skipForwardSeconds,
            skipSilence = p[Keys.skipSilence] ?: defaults.skipSilence,
            continuousPlayback = p[Keys.continuous] ?: defaults.continuousPlayback,
            volumeNormalization = p[Keys.volumeNormalization] ?: defaults.volumeNormalization,
            playedThresholdSeconds = p[Keys.playedThreshold] ?: defaults.playedThresholdSeconds
        )
    }

    suspend fun current(): PlaybackSettings = settings.first()

    suspend fun setSpeed(value: Float) {
        context.playbackStore.edit { it[Keys.speed] = value }
    }

    suspend fun setSkipBack(seconds: Int) {
        context.playbackStore.edit { it[Keys.skipBack] = seconds }
    }

    suspend fun setSkipForward(seconds: Int) {
        context.playbackStore.edit { it[Keys.skipForward] = seconds }
    }

    suspend fun setSkipSilence(value: Boolean) {
        context.playbackStore.edit { it[Keys.skipSilence] = value }
    }

    suspend fun setContinuousPlayback(value: Boolean) {
        context.playbackStore.edit { it[Keys.continuous] = value }
    }

    suspend fun setVolumeNormalization(value: Boolean) {
        context.playbackStore.edit { it[Keys.volumeNormalization] = value }
    }

    suspend fun setPlayedThreshold(seconds: Int) {
        context.playbackStore.edit { it[Keys.playedThreshold] = seconds }
    }
}
