package com.acrovox.core.player

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.id3.UrlLinkFrame
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Id3ChaptersTest {
    private fun chapter(startMs: Int, title: String?, url: String? = null) = ChapterFrame(
        "ch",
        startMs,
        startMs + 1_000,
        0,
        0,
        buildList {
            title?.let { add(TextInformationFrame("TIT2", "titre", listOf(it))) }
            url?.let { add(UrlLinkFrame("WXXX", "lien", it)) }
        }.toTypedArray()
    )

    @Test
    fun chapFrames_becomeChaptersInOrder() {
        val metadata = Metadata(chapter(60_000, "Deuxième"), chapter(0, "Intro", "https://example.org/i"))

        val chapters = metadata.toParsedChapters()

        assertThat(chapters.map { it.title }).containsExactly("Intro", "Deuxième").inOrder()
        assertThat(chapters[0].startMs).isEqualTo(0)
        assertThat(chapters[0].url).isEqualTo("https://example.org/i")
        assertThat(chapters[1].startMs).isEqualTo(60_000)
    }

    @Test
    fun chapterWithoutTitle_isSkipped() {
        val metadata = Metadata(chapter(0, null), chapter(5_000, "OK"))

        assertThat(metadata.toParsedChapters().map { it.title }).containsExactly("OK")
    }
}
