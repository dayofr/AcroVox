package com.acrovox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Couleurs de marque, identiques dans les deux thèmes.
internal val Coral = Color(0xFFFF5722)
internal val Violet = Color(0xFF7C4DFF)
internal val PeachGlow = Color(0xFFFFAB91)

/** Jetons Material 3 du thème sombre (pulse_audio/DESIGN.md). */
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFFFF5722),
    onPrimaryContainer = Color(0xFF541200),
    inversePrimary = Color(0xFFB02F00),
    secondary = Color(0xFFCDBDFF),
    onSecondary = Color(0xFF370096),
    secondaryContainer = Color(0xFF5203D5),
    onSecondaryContainer = Color(0xFFC0ACFF),
    tertiary = Color(0xFFFFB59E),
    onTertiary = Color(0xFF54200F),
    tertiaryContainer = Color(0xFFC97E66),
    onTertiaryContainer = Color(0xFF4C1A09),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF323539),
    onSurfaceVariant = Color(0xFFE4BEB4),
    surfaceTint = Color(0xFFFFB5A0),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    outline = Color(0xFFAB8980),
    outlineVariant = Color(0xFF5B4039),
    surfaceBright = Color(0xFF36393E),
    surfaceDim = Color(0xFF111418),
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2E),
    surfaceContainerHighest = Color(0xFF323539)
)

/** Jetons Material 3 du thème clair (pulse_audio_light/DESIGN.md). */
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB02F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFF5722),
    onPrimaryContainer = Color(0xFF541200),
    inversePrimary = Color(0xFFFFB5A0),
    secondary = Color(0xFF545F73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E0F8),
    onSecondaryContainer = Color(0xFF586377),
    tertiary = Color(0xFF565D63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF6F757C),
    onTertiaryContainer = Color(0xFFFCFCFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E2E8),
    onSurfaceVariant = Color(0xFF5B4039),
    surfaceTint = Color(0xFFB02F00),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFEFF0F6),
    outline = Color(0xFF907067),
    outlineVariant = Color(0xFFE4BEB4),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD8DADF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF3),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8)
)

/**
 * Jetons propres à Pulse Audio, absents de Material 3 :
 * accents de marque, niveaux de surface et contours décrits en prose dans DESIGN.md.
 */
@Immutable
data class AcroVoxColors(
    /** Corail : lecture, progression, FAB. Contenu blanc par-dessus. */
    val brand: Color,
    val onBrand: Color,
    /** Violet : chapitres, badges, indicateur de navigation. */
    val accent: Color,
    val peach: Color,
    /** Niveau 0 : fond d'écran. */
    val canvas: Color,
    /** Niveau 1 : cartes et listes au repos. */
    val surfaceCard: Color,
    /** Niveau 2 : mini-lecteur, barre de navigation, menus. */
    val surfaceFloating: Color,
    /** Niveau 3 : dialogues, feuilles modales. */
    val surfaceModal: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Contour discret des cartes et pilules. */
    val outlineSubtle: Color,
    /** Contour des contrôles actifs ou focalisés. */
    val outlineActive: Color,
    /** Fond translucide des badges de durée posés sur une pochette. */
    val scrim: Color
)

internal val DarkAcroVoxColors = AcroVoxColors(
    brand = Coral,
    onBrand = Color.White,
    accent = Violet,
    peach = PeachGlow,
    canvas = Color(0xFF111418),
    surfaceCard = Color(0xFF181C22),
    surfaceFloating = Color(0xFF20252D),
    surfaceModal = Color(0xFF2A303A),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF4B5563),
    outlineSubtle = Color.White.copy(alpha = 0.08f),
    outlineActive = Coral.copy(alpha = 0.40f),
    scrim = Color.Black.copy(alpha = 0.60f)
)

internal val LightAcroVoxColors = AcroVoxColors(
    brand = Coral,
    onBrand = Color.White,
    accent = Violet,
    peach = PeachGlow,
    canvas = Color(0xFFF8F9FA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceFloating = Color(0xFFFFFFFF),
    surfaceModal = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111418),
    textSecondary = Color(0xFF64748B),
    textMuted = Color(0xFF94A3B8),
    outlineSubtle = Color(0xFFE2E8F0),
    outlineActive = Coral.copy(alpha = 0.40f),
    scrim = Color.Black.copy(alpha = 0.60f)
)

internal val LocalAcroVoxColors = staticCompositionLocalOf { DarkAcroVoxColors }
