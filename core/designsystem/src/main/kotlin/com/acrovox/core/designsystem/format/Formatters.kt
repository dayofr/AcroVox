package com.acrovox.core.designsystem.format

import androidx.core.text.HtmlCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val french = Locale.FRENCH
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", french)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", french)

/** « 48 min », « 1 h 18 min », « 2 h ». */
fun formatDuration(durationMs: Long?): String? {
    if (durationMs == null || durationMs <= 0) return null
    val totalMinutes = (durationMs + 30_000) / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes.coerceAtLeast(1)} min"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes min"
    }
}

/** « À l'instant », « Il y a 2 h », « Hier », « Il y a 3 j », « 12 sept. », « 12 sept. 2024 ». */
fun formatRelativeDate(
    epochMs: Long,
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val then = Instant.ofEpochMilli(epochMs)
    val now = Instant.ofEpochMilli(nowMs)
    val minutes = ChronoUnit.MINUTES.between(then, now)
    val thenDate = LocalDate.ofInstant(then, zone)
    val today = LocalDate.ofInstant(now, zone)
    val days = ChronoUnit.DAYS.between(thenDate, today)
    return when {
        minutes < 1 -> "À l'instant"
        minutes < 60 -> "Il y a $minutes min"
        days == 0L -> "Il y a ${minutes / 60} h"
        days == 1L -> "Hier"
        days < 7 -> "Il y a $days j"
        thenDate.year == today.year -> dayMonth.format(thenDate)
        else -> dayMonthYear.format(thenDate)
    }
}

/** Texte brut d'une description HTML (shownotes), espaces normalisés. Null si vide. */
fun htmlToPlainText(html: String?): String? {
    if (html.isNullOrBlank()) return null
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace('￼', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifEmpty { null }
}

/** « 850 Ko », « 45 Mo », « 1,2 Go ». */
fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(french, "%.1f Go", bytes / gb)
        bytes >= mb -> "${(bytes / mb).toLong()} Mo"
        else -> "${(bytes / kb).toLong().coerceAtLeast(1)} Ko"
    }
}
