package com.acrovox.core.network.rss

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Instant
import org.junit.Test

class FeedDatesTest {
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun rfc822_variants() {
        val expected = ms("2026-07-27T05:00:00Z")
        listOf(
            "Mon, 27 Jul 2026 05:00:00 GMT",
            "Mon, 27 Jul 2026 05:00:00 +0000",
            "Mon, 27 Jul 2026 05:00:00 -0000",
            "Mon, 27 Jul 2026 07:00:00 +0200",
            "Mon, 27 Jul 2026 07:00:00 CEST",
            "Mon, 27 Jul 2026 01:00:00 EDT",
            "27 Jul 2026 05:00:00 GMT",
            "Mon, 27 Jul 2026 05:00 GMT",
            "Mon, 27 July 2026 05:00:00 GMT",
            "mon, 27 jul 2026 05:00:00 gmt",
            "Tue, 27 Jul 2026 05:00:00 GMT",
            "Mon,  27  Jul 2026 05:00:00 GMT",
            "Mon, 27 Jul 2026 05:00:00",
            "Mon, 27 Jul 2026 07:00:00 +02:00"
        ).forEach { assertWithMessage(it).that(FeedDates.parse(it)).isEqualTo(expected) }
    }

    @Test
    fun singleDigitDay() {
        assertThat(FeedDates.parse("Fri, 4 Sep 2026 10:00:00 GMT")).isEqualTo(ms("2026-09-04T10:00:00Z"))
    }

    @Test
    fun iso8601() {
        assertThat(FeedDates.parse("2026-07-27T07:00:00+02:00")).isEqualTo(ms("2026-07-27T05:00:00Z"))
        assertThat(FeedDates.parse("2026-07-27T05:00:00Z")).isEqualTo(ms("2026-07-27T05:00:00Z"))
        assertThat(FeedDates.parse("2026-07-27 05:00:00")).isEqualTo(ms("2026-07-27T05:00:00Z"))
    }

    @Test
    fun unreadable_isNull() {
        listOf(null, "", "   ", "hier", "32 Foo 2026 00:00:00 GMT").forEach {
            assertWithMessage(it.toString()).that(FeedDates.parse(it)).isNull()
        }
    }

    @Test
    fun durations() {
        assertThat(parseDurationMs("1245")).isEqualTo(1_245_000)
        assertThat(parseDurationMs("1245.5")).isEqualTo(1_245_500)
        assertThat(parseDurationMs("27:25")).isEqualTo((27 * 60 + 25) * 1000L)
        assertThat(parseDurationMs("00:26:35")).isEqualTo((26 * 60 + 35) * 1000L)
        assertThat(parseDurationMs("1:02:03")).isEqualTo((3600 + 2 * 60 + 3) * 1000L)
        listOf(null, "", "abc", "0", "-5", "1:2:3:4").forEach {
            assertWithMessage(it.toString()).that(parseDurationMs(it)).isNull()
        }
    }
}
