package com.acrovox.core.data.settings

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

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(val mode: ThemeMode = ThemeMode.SYSTEM, val dynamicColor: Boolean = false) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

private val Context.themeStore: DataStore<Preferences> by preferencesDataStore("theme")

@Singleton
class ThemeRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val mode = stringPreferencesKey("mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
    }

    val settings: Flow<ThemeSettings> = context.themeStore.data.map { p ->
        ThemeSettings(
            mode = p[Keys.mode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.dynamicColor] ?: false
        )
    }

    suspend fun current(): ThemeSettings = settings.first()

    suspend fun setMode(mode: ThemeMode) {
        context.themeStore.edit { it[Keys.mode] = mode.name }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.themeStore.edit { it[Keys.dynamicColor] = value }
    }
}
