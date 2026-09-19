package com.acrovox.core.data

import com.acrovox.core.data.opml.Opml
import com.acrovox.core.data.opml.OpmlException
import com.acrovox.core.data.opml.OpmlOutline
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpmlTest {
    private fun read(xml: String) = Opml.read(xml.trimIndent().byteInputStream())

    @Test
    fun readsAntennaPodExport() {
        val outlines = read(
            """
            <?xml version='1.0' encoding='UTF-8' standalone='no' ?>
            <opml version="2.0">
              <head><title>AntennaPod Subscriptions</title></head>
              <body>
                <outline text="Underscore_" title="Underscore_" type="rss" xmlUrl="https://feeds.acast.com/u" htmlUrl="https://u.fr" />
                <outline text="Affaires sensibles" type="rss" xmlUrl="https://radiofrance-podcast.net/a.xml" />
              </body>
            </opml>
            """
        )
        assertThat(outlines).containsExactly(
            OpmlOutline("Underscore_", "https://feeds.acast.com/u", "https://u.fr"),
            OpmlOutline("Affaires sensibles", "https://radiofrance-podcast.net/a.xml")
        ).inOrder()
    }

    @Test
    fun flattensFolders_skipsDuplicates_ignoresAttributeCase() {
        val outlines = read(
            """
            <opml version="1.0"><body>
              <outline text="Tech">
                <outline text="A" xmlurl="https://a.org/feed"/>
                <outline text="A bis" xmlUrl="https://a.org/feed"/>
              </outline>
              <outline text="B" xmlUrl="https://b.org/feed"/>
            </body></opml>
            """
        )
        assertThat(outlines.map { it.xmlUrl }).containsExactly("https://a.org/feed", "https://b.org/feed").inOrder()
        assertThat(outlines.first().title).isEqualTo("A")
    }

    @Test
    fun notOpml_throws() {
        assertThrows(OpmlException::class.java) { read("<rss><channel/></rss>") }
        assertThrows(OpmlException::class.java) { read("pas du xml") }
    }

    @Test
    fun writeThenRead_roundTrips() {
        val outlines = listOf(OpmlOutline("L'Heure du Monde & co", "https://a.org/feed?x=1&y=2", "https://a.org"))
        val out = ByteArrayOutputStream()

        Opml.write(outlines, out)

        assertThat(Opml.read(out.toByteArray().inputStream())).isEqualTo(outlines)
    }
}
