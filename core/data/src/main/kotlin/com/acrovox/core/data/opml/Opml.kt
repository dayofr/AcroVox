package com.acrovox.core.data.opml

import android.util.Xml
import java.io.InputStream
import java.io.OutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/** Podcast listé dans un fichier OPML. */
data class OpmlOutline(val title: String, val xmlUrl: String, val htmlUrl: String? = null)

class OpmlException(message: String, cause: Throwable? = null) : Exception(message, cause)

object Opml {
    /**
     * Lit les `<outline xmlUrl=…>` à toute profondeur (les dossiers sont aplatis).
     * Doublons retirés, ordre conservé.
     */
    fun read(input: InputStream): List<OpmlOutline> {
        val parser = Xml.newPullParser().apply { setInput(input, null) }
        val outlines = LinkedHashMap<String, OpmlOutline>()
        var sawOpml = false
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                when (parser.name.lowercase()) {
                    "opml" -> sawOpml = true
                    "outline" -> {
                        val url = parser.attribute("xmlUrl") ?: parser.attribute("url") ?: continue
                        val title = parser.attribute("title") ?: parser.attribute("text") ?: url
                        outlines.putIfAbsent(url, OpmlOutline(title, url, parser.attribute("htmlUrl")))
                    }
                }
            }
        } catch (e: XmlPullParserException) {
            throw OpmlException("Fichier OPML illisible : ${e.message}", e)
        }
        if (!sawOpml) throw OpmlException("Ce fichier n'est pas un OPML")
        return outlines.values.toList()
    }

    fun write(outlines: List<OpmlOutline>, output: OutputStream, now: ZonedDateTime = ZonedDateTime.now()) {
        val serializer = Xml.newSerializer()
        serializer.setOutput(output, "UTF-8")
        serializer.startDocument("UTF-8", null)
        serializer.startTag(null, "opml").attribute(null, "version", "2.0")
        serializer.startTag(null, "head")
        serializer.startTag(null, "title").text("Abonnements AcroVox").endTag(null, "title")
        serializer.startTag(
            null,
            "dateCreated"
        ).text(DateTimeFormatter.RFC_1123_DATE_TIME.format(now)).endTag(null, "dateCreated")
        serializer.endTag(null, "head")
        serializer.startTag(null, "body")
        outlines.forEach { outline ->
            serializer.startTag(null, "outline")
            serializer.attribute(null, "type", "rss")
            serializer.attribute(null, "text", outline.title)
            serializer.attribute(null, "title", outline.title)
            serializer.attribute(null, "xmlUrl", outline.xmlUrl)
            outline.htmlUrl?.let { serializer.attribute(null, "htmlUrl", it) }
            serializer.endTag(null, "outline")
        }
        serializer.endTag(null, "body")
        serializer.endTag(null, "opml")
        serializer.endDocument()
        serializer.flush()
    }

    /** Attribut sans tenir compte de la casse du nom (xmlUrl, xmlurl…). */
    private fun XmlPullParser.attribute(name: String): String? = (0 until attributeCount)
        .firstOrNull { getAttributeName(it).equals(name, ignoreCase = true) }
        ?.let { getAttributeValue(it).trim() }
        ?.takeIf { it.isNotEmpty() }
}
