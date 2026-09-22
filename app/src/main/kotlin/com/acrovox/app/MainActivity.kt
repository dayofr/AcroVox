package com.acrovox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.app.ui.AcroVoxApp
import com.acrovox.core.data.settings.ThemeRepository
import com.acrovox.core.data.settings.ThemeSettings
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themes: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by themes.settings.collectAsStateWithLifecycle(initialValue = ThemeSettings())
            AcroVoxTheme(darkTheme = theme.isDark(isSystemInDarkTheme()), dynamicColor = theme.dynamicColor) {
                AcroVoxApp()
            }
        }
    }
}
