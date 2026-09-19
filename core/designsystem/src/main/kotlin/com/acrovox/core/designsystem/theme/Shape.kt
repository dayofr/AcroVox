package com.acrovox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val AcroVoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

/** Formes nommées par usage, pour ne pas deviner le bon niveau Material. */
object AcroVoxShape {
    /** Pochettes : squircle 16 dp. */
    val Artwork = RoundedCornerShape(16.dp)

    /** Petites vignettes (mini-lecteur, listes denses). */
    val ArtworkSmall = RoundedCornerShape(12.dp)

    /** Cartes d'épisode et feuilles. */
    val Card = RoundedCornerShape(24.dp)

    /** Contrôles interactifs : chips, CTA, mini-lecteur, champs. */
    val Pill = RoundedCornerShape(percent = 50)
}
