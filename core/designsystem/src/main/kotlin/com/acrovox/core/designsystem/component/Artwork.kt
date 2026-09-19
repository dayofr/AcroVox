package com.acrovox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme

/** Pochette de podcast ou d'épisode. Sans image, affiche une icône sur fond neutre. */
@Composable
fun Artwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = AcroVoxShape.Artwork
) {
    val framed = modifier
        .clip(shape)
        .border(1.dp, AcroVoxTheme.colors.outlineSubtle, shape)
    if (url == null) {
        ArtworkPlaceholder(framed)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            loading = { ArtworkPlaceholder(Modifier.fillMaxSize()) },
            error = { ArtworkPlaceholder(Modifier.fillMaxSize()) },
            modifier = framed
        )
    }
}

@Composable
private fun ArtworkPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AcroVoxIcons.Podcast,
            contentDescription = null,
            tint = AcroVoxTheme.colors.textMuted,
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}
