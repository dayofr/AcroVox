package com.acrovox.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.acrovox.core.designsystem.R

private fun variableFont(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Titres : émissions, épisodes du lecteur, bannières. */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, 400),
    variableFont(R.font.space_grotesk, 500),
    variableFont(R.font.space_grotesk, 600),
    variableFont(R.font.space_grotesk, 700)
)

/** Corps, métadonnées, contrôles. */
val PlusJakartaSans = FontFamily(
    variableFont(R.font.plus_jakarta_sans, 400),
    variableFont(R.font.plus_jakarta_sans, 500),
    variableFont(R.font.plus_jakarta_sans, 600),
    variableFont(R.font.plus_jakarta_sans, 700)
)

private val HeadlineTracking = (-0.02).em

/** Chiffres à chasse fixe : les durées ne sautent pas pendant la lecture. */
private const val TABULAR_FIGURES = "tnum"

private fun headline(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = HeadlineTracking
)

private fun jakarta(size: Int, lineHeight: Int, weight: FontWeight, tabular: Boolean = false) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    fontFeatureSettings = if (tabular) TABULAR_FIGURES else null
)

internal val AcroVoxTypography = Typography(
    displayLarge = headline(40, 48, FontWeight.Bold),
    displayMedium = headline(36, 44, FontWeight.Bold),
    displaySmall = headline(32, 40, FontWeight.Bold),
    headlineLarge = headline(32, 40, FontWeight.Bold),
    headlineMedium = headline(24, 32, FontWeight.SemiBold),
    headlineSmall = headline(20, 28, FontWeight.SemiBold),
    titleLarge = jakarta(18, 26, FontWeight.SemiBold),
    titleMedium = jakarta(16, 24, FontWeight.SemiBold),
    titleSmall = jakarta(14, 20, FontWeight.SemiBold),
    bodyLarge = jakarta(16, 24, FontWeight.Normal),
    bodyMedium = jakarta(14, 20, FontWeight.Normal),
    bodySmall = jakarta(12, 16, FontWeight.Normal),
    labelLarge = jakarta(14, 20, FontWeight.SemiBold),
    labelMedium = jakarta(12, 16, FontWeight.SemiBold),
    labelSmall = jakarta(11, 14, FontWeight.Medium, tabular = true)
)

/** Variante mobile de headline-lg (26/34). */
val HeadlineLargeMobile = headline(26, 34, FontWeight.Bold)
