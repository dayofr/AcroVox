package com.acrovox.core.network.rss

import android.util.Xml
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * Lit un flux RSS 2.0 ou Atom en streaming, avec les extensions iTunes et Podcasting 2.0.
 *
 * Les épisodes sans fichier audio (pas d'`enclosure`) sont ignorés.
 */
class FeedParser @Inject constructor() {

    /** @param encoding null = détecté depuis la déclaration XML. */
    fun parse(input: InputStream, encoding: String? = null): ParsedFeed = try {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(input, encoding)
        }
        parser.nextTag()
        when {
            parser.name == "rss" -> parseRss(parser)
            parser.name == "feed" && parser.namespace == ATOM_NAMESPACE -> parseAtom(parser)
            else -> throw FeedParseException("Pas un flux RSS ou Atom (racine <${parser.name}>)")
        }
    } catch (e: XmlPullParserException) {
        throw FeedParseException("XML invalide : ${e.message}", e)
    } catch (e: IOException) {
        throw FeedParseException("Lecture du flux impossible : ${e.message}", e)
    }

    // RSS 2.0

    private fun parseRss(parser: XmlPullParser): ParsedFeed {
        var feed: ParsedFeed? = null
        parser.forEachChild {
            if (parser.namespace.isEmpty() && parser.name == "channel") feed = parseChannel(parser) else parser.skip()
        }
        return feed ?: throw FeedParseException("Flux RSS sans <channel>")
    }

    private fun parseChannel(parser: XmlPullParser): ParsedFeed {
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var summary: String? = null
        var language: String? = null
        var author: String? = null
        var itunesImage: String? = null
        var rssImage: String? = null
        var podcastGuid: String? = null
        var newFeedUrl: String? = null
        val categories = linkedSetOf<String>()
        val episodes = mutableListOf<ParsedEpisode>()

        parser.forEachChild {
            when (Ns.of(parser.namespace) to parser.name) {
                Ns.RSS to "title" -> title = parser.readText()
                Ns.RSS to "link" -> link = parser.readText()
                Ns.RSS to "description" -> description = parser.readText()
                Ns.RSS to "language" -> language = parser.readText()
                Ns.RSS to "image" -> rssImage = parseRssImage(parser)
                Ns.RSS to "item" -> parseItem(parser)?.let(episodes::add)
                Ns.ITUNES to "author" -> author = parser.readText()
                Ns.ITUNES to "summary" -> summary = parser.readText()
                Ns.ITUNES to "image" -> itunesImage = parser.attr("href").also { parser.skip() }
                Ns.ITUNES to "category" -> parseCategory(parser, categories)
                Ns.ITUNES to "new-feed-url" -> newFeedUrl = parser.readText()
                Ns.PODCAST to "guid" -> podcastGuid = parser.readText()
                else -> parser.skip()
            }
        }
        return ParsedFeed(
            title = title.orEmpty(),
            author = author,
            description = description ?: summary,
            imageUrl = itunesImage ?: rssImage,
            link = link,
            language = language,
            categories = categories.toList(),
            podcastGuid = podcastGuid,
            newFeedUrl = newFeedUrl,
            episodes = episodes
        )
    }

    private fun parseRssImage(parser: XmlPullParser): String? {
        var url: String? = null
        parser.forEachChild { if (parser.name == "url") url = parser.readText() else parser.skip() }
        return url
    }

    /** `itunes:category` peut contenir des sous-catégories. */
    private fun parseCategory(parser: XmlPullParser, into: MutableSet<String>) {
        parser.attr("text")?.let(into::add)
        parser.forEachChild {
            if (Ns.of(parser.namespace) == Ns.ITUNES &&
                parser.name == "category"
            ) {
                parseCategory(parser, into)
            } else {
                parser.skip()
            }
        }
    }

    private fun parseItem(parser: XmlPullParser): ParsedEpisode? {
        var guid: String? = null
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var contentEncoded: String? = null
        var summary: String? = null
        var pubDate: Long? = null
        var duration: Long? = null
        var enclosure: Enclosure? = null
        var mediaContent: Enclosure? = null
        var image: String? = null
        var chapters: String? = null
        var podloveChapters: List<ParsedChapter> = emptyList()
        val transcripts = mutableListOf<Pair<String, String?>>()

        parser.forEachChild {
            when (Ns.of(parser.namespace) to parser.name) {
                Ns.RSS to "guid" -> guid = parser.readText()
                Ns.RSS to "title" -> title = parser.readText()
                Ns.RSS to "link" -> link = parser.readText()
                Ns.RSS to "description" -> description = parser.readText()
                Ns.RSS to "pubDate" -> pubDate = FeedDates.parse(parser.readText())
                Ns.RSS to "enclosure" -> enclosure = parser.readEnclosure("url")
                Ns.CONTENT to "encoded" -> contentEncoded = parser.readText()
                Ns.ITUNES to "summary" -> summary = parser.readText()
                Ns.ITUNES to "duration" -> duration = parseDurationMs(parser.readText())
                Ns.ITUNES to "image" -> image = parser.attr("href").also { parser.skip() }
                Ns.ITUNES to "title" -> if (title == null) title = parser.readText() else parser.skip()
                Ns.MEDIA to "content" ->
                    mediaContent =
                        mediaContent ?: parser.readEnclosure("url")?.takeIf { it.isAudioOrVideo }
                Ns.PODCAST to "chapters" -> chapters = parser.attr("url").also { parser.skip() }
                Ns.PODLOVE to "chapters" -> podloveChapters = parsePodloveChapters(parser)
                Ns.PODCAST to "transcript" -> {
                    parser.attr("url")?.let { transcripts += it to parser.attr("type") }
                    parser.skip()
                }
                else -> parser.skip()
            }
        }

        val media = enclosure ?: mediaContent ?: return null
        val transcript = transcripts.minByOrNull { (_, type) -> transcriptRank(type) }
        return ParsedEpisode(
            guid = guid?.takeIf { it.isNotBlank() } ?: media.url,
            title = title?.takeIf { it.isNotBlank() } ?: media.url.substringAfterLast('/'),
            description = contentEncoded ?: description ?: summary,
            link = link,
            pubDate = pubDate,
            durationMs = duration,
            mediaUrl = media.url,
            mediaType = media.type,
            mediaSize = media.length,
            imageUrl = image,
            chaptersUrl = chapters,
            chapters = podloveChapters,
            transcriptUrl = transcript?.first,
            transcriptType = transcript?.second
        )
    }

    /** `<psc:chapters>` : chaque `<psc:chapter start title href image/>`, dans l'ordre du flux. */
    private fun parsePodloveChapters(parser: XmlPullParser): List<ParsedChapter> {
        val chapters = mutableListOf<ParsedChapter>()
        parser.forEachChild {
            if (Ns.of(parser.namespace) == Ns.PODLOVE && parser.name == "chapter") {
                val start = parser.attr("start")?.let(::parsePodloveStartMs)
                val title = parser.attr("title")
                if (start != null && !title.isNullOrBlank()) {
                    chapters += ParsedChapter(start, title, parser.attr("href"), parser.attr("image"))
                }
            }
            parser.skip()
        }
        return chapters.sortedBy { it.startMs }
    }

    /** `start` Podlove : secondes (`57`, `57.5`) ou `HH:MM:SS[.mmm]`. */
    private fun parsePodloveStartMs(raw: String): Long? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (':' !in value) return value.toDoubleOrNull()?.times(1_000)?.toLong()?.takeIf { it >= 0 }
        var total = 0.0
        for (part in value.split(':')) {
            total = total * 60 + (part.toDoubleOrNull() ?: return null)
        }
        return (total * 1_000).toLong().takeIf { it >= 0 }
    }

    // Atom

    private fun parseAtom(parser: XmlPullParser): ParsedFeed {
        var title: String? = null
        var subtitle: String? = null
        var link: String? = null
        var author: String? = null
        var logo: String? = null
        var icon: String? = null
        var itunesImage: String? = null
        val episodes = mutableListOf<ParsedEpisode>()

        parser.forEachChild {
            when (Ns.of(parser.namespace) to parser.name) {
                Ns.ATOM to "title" -> title = parser.readText()
                Ns.ATOM to "subtitle" -> subtitle = parser.readText()
                Ns.ATOM to "link" -> {
                    if (parser.attr("rel") in setOf(null, "alternate")) link = parser.attr("href")
                    parser.skip()
                }
                Ns.ATOM to "author" -> author = parseAtomAuthor(parser)
                Ns.ATOM to "logo" -> logo = parser.readText()
                Ns.ATOM to "icon" -> icon = parser.readText()
                Ns.ATOM to "entry" -> parseAtomEntry(parser)?.let(episodes::add)
                Ns.ITUNES to "image" -> itunesImage = parser.attr("href").also { parser.skip() }
                else -> parser.skip()
            }
        }
        return ParsedFeed(
            title = title.orEmpty(),
            author = author,
            description = subtitle,
            imageUrl = itunesImage ?: logo ?: icon,
            link = link,
            episodes = episodes
        )
    }

    private fun parseAtomAuthor(parser: XmlPullParser): String? {
        var name: String? = null
        parser.forEachChild { if (parser.name == "name") name = parser.readText() else parser.skip() }
        return name
    }

    private fun parseAtomEntry(parser: XmlPullParser): ParsedEpisode? {
        var id: String? = null
        var title: String? = null
        var summary: String? = null
        var content: String? = null
        var published: Long? = null
        var updated: Long? = null
        var link: String? = null
        var enclosure: Enclosure? = null
        var duration: Long? = null
        var image: String? = null

        parser.forEachChild {
            when (Ns.of(parser.namespace) to parser.name) {
                Ns.ATOM to "id" -> id = parser.readText()
                Ns.ATOM to "title" -> title = parser.readText()
                Ns.ATOM to "summary" -> summary = parser.readText()
                Ns.ATOM to "content" -> content = parser.readText()
                Ns.ATOM to "published" -> published = FeedDates.parse(parser.readText())
                Ns.ATOM to "updated" -> updated = FeedDates.parse(parser.readText())
                Ns.ATOM to "link" -> when (parser.attr("rel")) {
                    "enclosure" -> enclosure = parser.readEnclosure("href")
                    null, "alternate" -> link = parser.attr("href").also { parser.skip() }
                    else -> parser.skip()
                }
                Ns.ITUNES to "duration" -> duration = parseDurationMs(parser.readText())
                Ns.ITUNES to "image" -> image = parser.attr("href").also { parser.skip() }
                else -> parser.skip()
            }
        }
        val media = enclosure ?: return null
        return ParsedEpisode(
            guid = id?.takeIf { it.isNotBlank() } ?: media.url,
            title = title?.takeIf { it.isNotBlank() } ?: media.url.substringAfterLast('/'),
            description = content ?: summary,
            link = link,
            pubDate = published ?: updated,
            durationMs = duration,
            mediaUrl = media.url,
            mediaType = media.type,
            mediaSize = media.length,
            imageUrl = image
        )
    }

    private data class Enclosure(val url: String, val type: String?, val length: Long?) {
        val isAudioOrVideo get() = type == null || type.startsWith("audio/") || type.startsWith("video/")
    }

    private fun XmlPullParser.readEnclosure(urlAttribute: String): Enclosure? {
        val url = attr(urlAttribute)?.trim()
        val enclosure = if (url.isNullOrEmpty()) {
            null
        } else {
            Enclosure(url, attr("type"), attr("length")?.trim()?.toLongOrNull()?.takeIf { it > 0 })
        }
        skip()
        return enclosure
    }

    /** Ordre de préférence : les formats horodatés d'abord (affichage synchronisé). */
    private fun transcriptRank(type: String?): Int = when (type?.lowercase()) {
        "text/vtt" -> 0
        "application/x-subrip", "application/srt" -> 1
        "application/json" -> 2
        "text/html" -> 3
        else -> 4
    }
}

private const val ATOM_NAMESPACE = "http://www.w3.org/2005/Atom"

/** Espaces de noms reconnus. Plusieurs URI circulent pour iTunes et Podcasting 2.0. */
private enum class Ns {
    RSS,
    ATOM,
    ITUNES,
    CONTENT,
    MEDIA,
    PODCAST,
    PODLOVE,
    OTHER
    ;

    companion object {
        fun of(uri: String?): Ns {
            val u = uri.orEmpty().lowercase().trimEnd('/')
            return when {
                u.isEmpty() -> RSS
                u == ATOM_NAMESPACE.lowercase() -> ATOM
                u.startsWith("http://www.itunes.com/dtds/podcast-1.0") -> ITUNES
                u == "http://purl.org/rss/1.0/modules/content" -> CONTENT
                u == "http://search.yahoo.com/mrss" -> MEDIA
                u.contains("podcastindex.org/namespace") || u.contains("podcastindex-org/podcast-namespace") -> PODCAST
                u == "http://podlove.org/simple-chapters" -> PODLOVE
                else -> OTHER
            }
        }
    }
}
