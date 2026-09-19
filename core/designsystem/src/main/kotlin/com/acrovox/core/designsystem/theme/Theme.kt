package com.acrovox.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Thème AcroVox, bâti sur le design system Pulse Audio.
 *
 * @param dynamicColor couleurs Material You du fond d'écran (Android 12+).
 * Les accents de marque ([AcroVoxColors.brand]) restent corail.
 */
@Composable
fun AcroVoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val acroVoxColors = if (darkTheme) DarkAcroVoxColors else LightAcroVoxColors

    CompositionLocalProvider(LocalAcroVoxColors provides acroVoxColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AcroVoxTypography,
            shapes = AcroVoxShapes,
            content = content
        )
    }
}

object AcroVoxTheme {
    val colors: AcroVoxColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAcroVoxColors.current
}
