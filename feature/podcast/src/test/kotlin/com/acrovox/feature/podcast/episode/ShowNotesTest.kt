package com.acrovox.feature.podcast.episode

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShowNotesTest {
    private val regex = Regex("""(?<![\d:])(?:(\d{1,2}):)?([0-5]?\d):([0-5]\d)(?![\d:])""")

    private fun positions(text: String) = regex.findAll(text).map { parseTimestampMs(it) }.toList()

    @Test
    fun findsChapterTimestamps() {
        assertThat(
            positions("00:00 Intro\n12:34 Le sujet\n1:02:03 Fin")
        ).containsExactly(0L, 754_000L, 3_723_000L).inOrder()
    }

    @Test
    fun ignoresDatesAndLongNumbers() {
        assertThat(positions("Enregistré le 12/09/2026 à 99:99, tél. 01:23:45:67")).isEmpty()
    }
}
