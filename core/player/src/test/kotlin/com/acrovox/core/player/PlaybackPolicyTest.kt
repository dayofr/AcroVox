package com.acrovox.core.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackPolicyTest {
    private val now = 10_000_000_000L
    private val hour = 3_600_000L

    @Test
    fun startPosition_rewindsAfterPause() {
        assertThat(PlaybackPolicy.startPosition(600_000, now - 10_000, now, hour, 0)).isEqualTo(600_000)
        assertThat(PlaybackPolicy.startPosition(600_000, now - 120_000, now, hour, 0)).isEqualTo(597_000)
        assertThat(PlaybackPolicy.startPosition(600_000, now - 2 * hour, now, hour, 0)).isEqualTo(590_000)
        assertThat(PlaybackPolicy.startPosition(2_000, now - 2 * hour, now, hour, 0)).isEqualTo(0)
    }

    @Test
    fun startPosition_restartsFinishedEpisode_andSkipsIntro() {
        assertThat(PlaybackPolicy.startPosition(hour - 10_000, now, now, hour, 0)).isEqualTo(0)
        assertThat(PlaybackPolicy.startPosition(0, null, now, hour, 45_000)).isEqualTo(45_000)
        assertThat(PlaybackPolicy.startPosition(600_000, now, now, hour, 45_000)).isEqualTo(600_000)
    }

    @Test
    fun finished_nearEndOrInOutro() {
        assertThat(PlaybackPolicy.isFinished(hour - 20_000, hour)).isTrue()
        assertThat(PlaybackPolicy.isFinished(hour - 60_000, hour)).isFalse()
        assertThat(PlaybackPolicy.isFinished(hour - 60_000, hour, skipOutroMs = 90_000)).isTrue()
        assertThat(PlaybackPolicy.isFinished(500, null)).isFalse()
    }

    @Test
    fun inOutro() {
        assertThat(PlaybackPolicy.inOutro(hour - 50_000, hour, 60_000)).isTrue()
        assertThat(PlaybackPolicy.inOutro(hour - 70_000, hour, 60_000)).isFalse()
        assertThat(PlaybackPolicy.inOutro(hour, hour, 0)).isFalse()
    }
}
