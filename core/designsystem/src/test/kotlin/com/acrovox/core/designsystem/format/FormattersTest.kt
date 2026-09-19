package com.acrovox.core.designsystem.format

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun durations() {
        assertEquals(null, formatDuration(null))
        assertEquals("1 min", formatDuration(10_000))
        assertEquals("48 min", formatDuration(48 * 60_000L))
        assertEquals("1 h 18 min", formatDuration(78 * 60_000L))
        assertEquals("2 h", formatDuration(120 * 60_000L))
    }

    @Test
    fun relativeDates() {
        val now = Instant.parse("2026-09-19T12:00:00Z").toEpochMilli()
        fun at(iso: String) = formatRelativeDate(Instant.parse(iso).toEpochMilli(), now, ZoneOffset.UTC)
        assertEquals("À l'instant", at("2026-09-19T11:59:40Z"))
        assertEquals("Il y a 5 min", at("2026-09-19T11:55:00Z"))
        assertEquals("Il y a 2 h", at("2026-09-19T10:00:00Z"))
        assertEquals("Hier", at("2026-09-18T23:00:00Z"))
        assertEquals("Il y a 3 j", at("2026-09-16T08:00:00Z"))
        assertEquals("2 sept.", at("2026-09-02T08:00:00Z"))
        assertEquals("2 sept. 2025", at("2025-09-02T08:00:00Z"))
    }
}
