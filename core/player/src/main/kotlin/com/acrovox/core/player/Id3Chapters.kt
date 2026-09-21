package com.acrovox.core.player

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.id3.UrlLinkFrame
import com.acrovox.core.network.rss.ParsedChapter

/**
 * Chapitres lus dans les tags ID3 CHAP du fichier en cours de lecture.
 * Titre depuis le sous-champ TIT2, lien depuis WXXX. Sans titre, le chapitre
 * est ignoré.
 */
internal fun Metadata.toParsedChapters(): List<ParsedChapter> {
    val chapters = mutableListOf<ParsedChapter>()
    for (i in 0 until length()) {
        val frame = get(i) as? ChapterFrame ?: continue
        val title = frame.subFrames().filterIsInstance<TextInformationFrame>()
            .firstOrNull { it.id == "TIT2" }?.values?.firstOrNull()
            ?: frame.subFrames().filterIsInstance<TextInformationFrame>().firstOrNull()?.values?.firstOrNull()
        if (title.isNullOrBlank()) continue
        val url = frame.subFrames().filterIsInstance<UrlLinkFrame>().firstOrNull()?.url
        chapters += ParsedChapter(startMs = frame.startTimeMs.toLong().coerceAtLeast(0), title = title, url = url)
    }
    return chapters.sortedBy { it.startMs }
}

private fun ChapterFrame.subFrames(): List<androidx.media3.extractor.metadata.id3.Id3Frame> =
    List(getSubFrameCount(), ::getSubFrame)
