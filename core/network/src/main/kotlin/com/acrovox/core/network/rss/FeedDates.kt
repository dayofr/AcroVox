package com.acrovox.core.network.rss

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Dates des flux. Les éditeurs s'écartent souvent de la RFC 822 : jour de la semaine absent
 * ou faux, secondes absentes, fuseaux en abréviation, mois en toutes lettres, ISO 8601.
 */
internal object FeedDates {
    private val zoneAbbreviations = mapOf(
        "GMT" to "+0000", "UT" to "+0000", "UTC" to "+0000", "Z" to "+0000",
        "EST" to "-0500", "EDT" to "-0400", "CST" to "-0600", "CDT" to "-0500",
        "MST" to "-0700", "MDT" to "-0600", "PST" to "-0800", "PDT" to "-0700",
        "CET" to "+0100", "CEST" to "+0200", "BST" to "+0100", "MET" to "+0100", "MEST" to "+0200"
    )

    private val trailingZone = Regex("""\s([A-Za-z]{1,4})$""")
    private val leadingDayName = Regex("""^[A-Za-z]+,?\s+""")
    private val colonOffset = Regex("""([+-]\d{2}):(\d{2})$""")

    /** « 27 Jul 2026 05:00[:00] +0000 », mois abrégé ou complet, casse libre. */
    private val rfc822: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .parseLenient()
        .appendPattern("d ")
        .appendPattern("[MMMM][MMM]")
        .appendPattern(" yyyy H:mm[:ss]")
        .optionalStart().appendLiteral(' ').appendOffset("+HHMM", "+0000").optionalEnd()
        .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
        .toFormatter(Locale.US)

    private val isoWithoutZone: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd['T'][' ']HH:mm[:ss]")
        .toFormatter(Locale.US)

    fun parse(raw: String?): Long? {
        val value = raw?.trim()?.replace(Regex("""\s+"""), " ")
        if (value.isNullOrEmpty()) return null
        return parseIso(value) ?: parseRfc822(value)
    }

    private fun parseIso(value: String): Long? {
        if (value.length < 10 || value[4] != '-') return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value, isoWithoutZone).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun parseRfc822(value: String): Long? {
        var normalized = value.replace(leadingDayName, "").replace(",", "")
        trailingZone.find(normalized)?.let { match ->
            val offset = zoneAbbreviations[match.groupValues[1].uppercase()]
            normalized = normalized.removeRange(match.range) + if (offset != null) " $offset" else ""
        }
        normalized = normalized.replace(colonOffset, "$1$2").replace(" -0000", " +0000")
        return try {
            OffsetDateTime.parse(normalized, rfc822).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

/** `itunes:duration` : « 1245 », « 1245.5 », « 27:25 », « 1:02:03 ». */
internal fun parseDurationMs(raw: String?): Long? {
    val value = raw?.trim()
    if (value.isNullOrEmpty()) return null
    val parts = value.split(':')
    if (parts.size > 3) return null
    val numbers = parts.map { it.trim().toDoubleOrNull() ?: return null }
    if (numbers.any { it < 0 }) return null
    val seconds = numbers.fold(0.0) { acc, n -> acc * 60 + n }
    return (seconds * 1000).toLong().takeIf { it > 0 }
}
