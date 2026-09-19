package com.acrovox.feature.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {
    @Test
    fun speeds() {
        assertThat(formatSpeed(1f)).isEqualTo("1x")
        assertThat(formatSpeed(1.2f)).isEqualTo("1.2x")
        assertThat(formatSpeed(1.75f)).isEqualTo("1.75x")
    }

    @Test
    fun clock() {
        assertThat(formatClock(0)).isEqualTo("0:00")
        assertThat(formatClock(245_000)).isEqualTo("4:05")
        assertThat(formatClock(3_723_000)).isEqualTo("1:02:03")
    }
}
