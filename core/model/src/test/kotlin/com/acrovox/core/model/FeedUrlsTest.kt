package com.acrovox.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedUrlsTest {
    @Test
    fun normalize() {
        assertThat(normalizeFeedUrl("https://example.org/feed")).isEqualTo("https://example.org/feed")
        assertThat(normalizeFeedUrl("  example.org/feed.xml ")).isEqualTo("https://example.org/feed.xml")
        assertThat(normalizeFeedUrl("feed://example.org/rss")).isEqualTo("https://example.org/rss")
        assertThat(normalizeFeedUrl("itpc://example.org/rss")).isEqualTo("https://example.org/rss")
        assertThat(normalizeFeedUrl("feed:https://example.org/rss")).isEqualTo("https://example.org/rss")
        assertThat(normalizeFeedUrl("http://localhost:8080/feed")).isEqualTo("http://localhost:8080/feed")
        assertThat(normalizeFeedUrl("affaires sensibles")).isNull()
        assertThat(normalizeFeedUrl("underscore")).isNull()
    }

    @Test
    fun looksLikeUrl() {
        assertThat(looksLikeUrl("feeds.acast.com/public/shows/x")).isTrue()
        assertThat(looksLikeUrl("https://example.org")).isTrue()
        assertThat(looksLikeUrl("affaires sensibles")).isFalse()
        assertThat(looksLikeUrl("underscore")).isFalse()
    }
}
