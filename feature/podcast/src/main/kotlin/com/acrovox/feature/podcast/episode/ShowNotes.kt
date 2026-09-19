package com.acrovox.feature.podcast.episode

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml

/** « 12:34 » ou « 1:02:03 », hors d'un nombre plus long. */
private val timestamp = Regex("""(?<![\d:])(?:(\d{1,2}):)?([0-5]?\d):([0-5]\d)(?![\d:])""")

/** Position en millisecondes d'un horodatage des notes d'épisode. */
internal fun parseTimestampMs(match: MatchResult): Long {
    val (h, m, s) = match.destructured
    return ((h.toLongOrNull() ?: 0) * 3600 + m.toLong() * 60 + s.toLong()) * 1000
}

/**
 * Notes d'épisode : HTML rendu, liens cliquables (ouverts par le navigateur),
 * horodatages cliquables pour se placer dans l'épisode.
 */
internal fun showNotes(html: String, linkColor: Color, onTimestamp: (Long) -> Unit): AnnotatedString {
    val styles = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.SemiBold))
    val base = AnnotatedString.fromHtml(
        html.replace("\n", "<br>").takeIf {
            !html.contains('<')
        } ?: html,
        linkStyles = styles
    )
    return buildAnnotatedString {
        append(base)
        timestamp.findAll(base.text).forEach { match ->
            val position = parseTimestampMs(match)
            addLink(
                LinkAnnotation.Clickable(tag = "t$position", styles = styles) { onTimestamp(position) },
                match.range.first,
                match.range.last + 1
            )
        }
    }
}
