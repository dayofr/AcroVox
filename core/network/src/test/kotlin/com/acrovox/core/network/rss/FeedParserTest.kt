package com.acrovox.core.network.rss

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedParserTest {
    private val parser = FeedParser()

    private fun fixture(name: String): ParsedFeed =
        javaClass.getResourceAsStream("/feeds/$name.xml")!!.use { parser.parse(it) }

    private fun xml(body: String): ParsedFeed = parser.parse(body.trimIndent().byteInputStream())

    @Test
    fun realFeeds_allParseWithCompleteEpisodes() {
        val names = listOf(
            "radiofrance", "acast", "audiomeans", "audion", "simplecast", "ausha",
            "anchor", "buzzsprout", "libsyn", "megaphone", "transistor", "spreaker"
        )
        names.forEach { name ->
            val feed = fixture(name)
            assertWithMessage("$name.title").that(feed.title).isNotEmpty()
            assertWithMessage("$name.imageUrl").that(feed.imageUrl).isNotNull()
            assertWithMessage("$name.episodes").that(feed.episodes).hasSize(2)
            feed.episodes.forEach { episode ->
                assertWithMessage("$name.guid").that(episode.guid).isNotEmpty()
                assertWithMessage("$name.title").that(episode.title).isNotEmpty()
                assertWithMessage("$name.mediaUrl").that(episode.mediaUrl).startsWith("http")
                assertWithMessage("$name.pubDate").that(episode.pubDate).isNotNull()
                assertWithMessage("$name.durationMs").that(episode.durationMs).isNotNull()
            }
        }
    }

    @Test
    fun radioFrance() {
        val feed = fixture("radiofrance")
        assertThat(feed.title).isEqualTo("Affaires sensibles")
        assertThat(feed.language).startsWith("fr")
        assertThat(feed.episodes.first().mediaType).isEqualTo("audio/x-m4a")
    }

    @Test
    fun buzzsprout_podcastingTwoTags() {
        val episode = fixture("buzzsprout").episodes.first()
        assertThat(episode.chaptersUrl).isNotNull()
        assertThat(episode.transcriptUrl).isNotNull()
        assertThat(fixture("buzzsprout").podcastGuid).isNotNull()
    }

    @Test
    fun channelFields_andNestedCategories() {
        val feed = xml(
            """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                 xmlns:podcast="https://podcastindex.org/namespace/1.0">
              <channel>
                <title><![CDATA[Mon podcast & co]]></title>
                <link>https://example.org</link>
                <itunes:summary>Résumé</itunes:summary>
                <itunes:author>Alex</itunes:author>
                <image><url>https://example.org/rss.png</url></image>
                <itunes:image href="https://example.org/itunes.png"/>
                <itunes:category text="Technology"><itunes:category text="Tech News"/></itunes:category>
                <itunes:new-feed-url>https://example.org/new.xml</itunes:new-feed-url>
                <podcast:guid>abc-123</podcast:guid>
              </channel>
            </rss>
            """
        )
        assertThat(feed.title).isEqualTo("Mon podcast & co")
        assertThat(feed.description).isEqualTo("Résumé")
        assertThat(feed.author).isEqualTo("Alex")
        assertThat(feed.imageUrl).isEqualTo("https://example.org/itunes.png")
        assertThat(feed.categories).containsExactly("Technology", "Tech News").inOrder()
        assertThat(feed.newFeedUrl).isEqualTo("https://example.org/new.xml")
        assertThat(feed.podcastGuid).isEqualTo("abc-123")
    }

    @Test
    fun items_withoutEnclosure_areSkipped_andContentEncodedWins() {
        val feed = xml(
            """
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <title>T</title>
                <item><title>Article</title><guid>a</guid></item>
                <item>
                  <title>Épisode</title>
                  <description>Court</description>
                  <content:encoded><![CDATA[<p>Long &amp; riche</p>]]></content:encoded>
                  <enclosure url=" https://example.org/e.mp3 " type="audio/mpeg" length="0"/>
                </item>
              </channel>
            </rss>
            """
        )
        val episode = feed.episodes.single()
        assertThat(episode.description).isEqualTo("<p>Long &amp; riche</p>")
        assertThat(episode.mediaUrl).isEqualTo("https://example.org/e.mp3")
        assertThat(episode.guid).isEqualTo("https://example.org/e.mp3")
        assertThat(episode.mediaSize).isNull()
        assertThat(episode.pubDate).isNull()
    }

    @Test
    fun transcript_prefersTimedFormat() {
        val feed = xml(
            """
            <rss version="2.0" xmlns:podcast="https://podcastindex.org/namespace/1.0">
              <channel><title>T</title>
                <item>
                  <title>E</title><guid>g</guid>
                  <enclosure url="https://example.org/e.mp3" type="audio/mpeg"/>
                  <podcast:transcript url="https://example.org/t.html" type="text/html"/>
                  <podcast:transcript url="https://example.org/t.vtt" type="text/vtt"/>
                  <podcast:chapters url="https://example.org/c.json" type="application/json+chapters"/>
                </item>
              </channel>
            </rss>
            """
        )
        val episode = feed.episodes.single()
        assertThat(episode.transcriptUrl).isEqualTo("https://example.org/t.vtt")
        assertThat(episode.transcriptType).isEqualTo("text/vtt")
        assertThat(episode.chaptersUrl).isEqualTo("https://example.org/c.json")
    }

    @Test
    fun podloveChapters_areParsedInOrder() {
        val feed = xml(
            """
            <rss version="2.0" xmlns:psc="http://podlove.org/simple-chapters">
              <channel><title>T</title>
                <item>
                  <title>E</title><guid>g</guid>
                  <enclosure url="https://example.org/e.mp3" type="audio/mpeg"/>
                  <psc:chapters version="1.1">
                    <psc:chapter start="135" title="Deuxième"/>
                    <psc:chapter start="0" title="Intro" href="https://example.org/intro" image="https://example.org/i.png"/>
                    <psc:chapter start="00:07:36.500" title="Troisième"/>
                    <psc:chapter title="Sans début"/>
                    <psc:chapter start="60"/>
                  </psc:chapters>
                </item>
              </channel>
            </rss>
            """
        )
        val chapters = feed.episodes.single().chapters
        assertThat(chapters.map { it.title }).containsExactly("Intro", "Deuxième", "Troisième").inOrder()
        assertThat(chapters[0].startMs).isEqualTo(0)
        assertThat(chapters[0].url).isEqualTo("https://example.org/intro")
        assertThat(chapters[0].imageUrl).isEqualTo("https://example.org/i.png")
        assertThat(chapters[1].startMs).isEqualTo(135_000)
        assertThat(chapters[2].startMs).isEqualTo(456_500)
    }

    @Test
    fun aushaFixture_hasPodloveChapters() {
        val episode = fixture("ausha").episodes.first { it.chapters.isNotEmpty() }
        assertThat(episode.chapters.first().startMs).isEqualTo(0)
        assertThat(
            episode.chapters.map {
                it.title
            }
        ).contains("Introduction à la reconstruction après un pervers narcissique")
    }

    @Test
    fun mediaContent_isUsedWhenNoEnclosure() {
        val feed = xml(
            """
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel><title>T</title>
                <item><title>E</title><guid>g</guid>
                  <media:content url="https://example.org/cover.jpg" type="image/jpeg"/>
                  <media:content url="https://example.org/e.m4a" type="audio/mp4" fileSize="10"/>
                </item>
              </channel>
            </rss>
            """
        )
        assertThat(feed.episodes.single().mediaUrl).isEqualTo("https://example.org/e.m4a")
    }

    @Test
    fun atomFeed() {
        val feed = xml(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom cast</title>
              <subtitle>Sous-titre</subtitle>
              <link rel="alternate" href="https://example.org"/>
              <author><name>Alex</name></author>
              <logo>https://example.org/logo.png</logo>
              <entry>
                <id>urn:uuid:1</id>
                <title>Premier</title>
                <updated>2026-07-27T05:00:00Z</updated>
                <summary>Résumé</summary>
                <link rel="enclosure" href="https://example.org/1.mp3" type="audio/mpeg" length="123"/>
                <link rel="alternate" href="https://example.org/1"/>
              </entry>
              <entry><id>urn:uuid:2</id><title>Sans audio</title></entry>
            </feed>
            """
        )
        assertThat(feed.title).isEqualTo("Atom cast")
        assertThat(feed.author).isEqualTo("Alex")
        assertThat(feed.imageUrl).isEqualTo("https://example.org/logo.png")
        val entry = feed.episodes.single()
        assertThat(entry.guid).isEqualTo("urn:uuid:1")
        assertThat(entry.link).isEqualTo("https://example.org/1")
        assertThat(entry.mediaSize).isEqualTo(123)
        assertThat(entry.pubDate).isEqualTo(Instant.parse("2026-07-27T05:00:00Z").toEpochMilli())
    }

    @Test
    fun invalidXml_throwsFeedParseException() {
        assertThrows(FeedParseException::class.java) { xml("<rss><channel><title>oops</channel>") }
        assertThrows(FeedParseException::class.java) { xml("<html><body>404</body></html>") }
    }
}
