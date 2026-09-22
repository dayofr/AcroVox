package com.acrovox.core.data.refresh

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RefreshSettings(val intervalHours: Long = 1)

private val Context.refreshStore: DataStore<Preferences> by preferencesDataStore("refresh")

@Singleton
class RefreshSettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val intervalHours = longPreferencesKey("interval_hours")
    }

    val settings: Flow<RefreshSettings> = context.refreshStore.data.map { p ->
        RefreshSettings(intervalHours = p[Keys.intervalHours] ?: 1)
    }

    suspend fun current(): RefreshSettings = settings.first()

    suspend fun setIntervalHours(value: Long) {
        context.refreshStore.edit { it[Keys.intervalHours] = value }
    }
}
