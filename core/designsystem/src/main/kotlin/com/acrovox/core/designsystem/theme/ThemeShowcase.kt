package com.acrovox.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Planche des jetons du thème : couleurs, typographie, formes. Sert de référence visuelle. */
@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    val colors = AcroVoxTheme.colors
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Section("Marque") {
            Swatch("brand", colors.brand)
            Swatch("accent", colors.accent)
            Swatch("peach", colors.peach)
        }
        Section("Surfaces") {
            Swatch("canvas", colors.canvas)
            Swatch("card", colors.surfaceCard)
            Swatch("floating", colors.surfaceFloating)
            Swatch("modal", colors.surfaceModal)
        }
        Section("Material") {
            Swatch("primary", scheme.primary)
            Swatch("secondary", scheme.secondary)
            Swatch("tertiary", scheme.tertiary)
            Swatch("error", scheme.error)
        }
        Section("Typographie") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                val type = MaterialTheme.typography
                Text("Les Maîtres du Code", style = type.headlineLarge, color = colors.textPrimary)
                Text("L'impact des modèles génératifs", style = type.headlineSmall, color = colors.textPrimary)
                Text("Underscore_ • Épisode 142", style = type.titleMedium, color = colors.textPrimary)
                Text(
                    "Analyse des enjeux technologiques qui redéfinissent l'équilibre mondial.",
                    style = type.bodyMedium,
                    color = colors.textSecondary
                )
                Text("24:12 • -34:28 • 1.2x", style = type.labelSmall, color = colors.textSecondary)
            }
        }
        Section("Formes") {
            ShapeSample("artwork", AcroVoxShape.Artwork)
            ShapeSample("card", AcroVoxShape.Card)
            ShapeSample("pill", AcroVoxShape.Pill)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = AcroVoxTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            content()
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(
            Modifier
                .size(64.dp)
                .background(color, AcroVoxShape.ArtworkSmall)
                .border(1.dp, AcroVoxTheme.colors.outlineSubtle, AcroVoxShape.ArtworkSmall)
        )
        Text(name, style = MaterialTheme.typography.labelSmall, color = AcroVoxTheme.colors.textSecondary)
    }
}

@Composable
private fun ShapeSample(name: String, shape: Shape) {
    Box(
        Modifier
            .size(width = 96.dp, height = 56.dp)
            .background(AcroVoxTheme.colors.surfaceCard, shape)
            .border(1.dp, AcroVoxTheme.colors.outlineSubtle, shape)
            .padding(Spacing.sm)
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = AcroVoxTheme.colors.textPrimary)
    }
}

@Preview(name = "Sombre", widthDp = 400, heightDp = 900)
@Composable
private fun ThemeShowcaseDarkPreview() {
    AcroVoxTheme(darkTheme = true) { ThemeShowcase(Modifier.fillMaxWidth()) }
}

@Preview(name = "Clair", widthDp = 400, heightDp = 900)
@Composable
private fun ThemeShowcaseLightPreview() {
    AcroVoxTheme(darkTheme = false) { ThemeShowcase(Modifier.fillMaxWidth()) }
}
