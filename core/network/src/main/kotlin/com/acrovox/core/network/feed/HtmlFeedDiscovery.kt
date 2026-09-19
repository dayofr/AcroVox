package com.acrovox.core.network.feed

import okhttp3.HttpUrl

/** Trouve le flux annoncé par une page web : `<link rel="alternate" type="application/rss+xml" href="…">`. */
internal object HtmlFeedDiscovery {
    private val linkTag = Regex("""<link\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val feedTypes = listOf("application/rss+xml", "application/atom+xml")

    fun find(html: String, base: HttpUrl): String? = linkTag.findAll(html)
        .map { it.value }
        .filter { tag -> attribute(tag, "rel")?.lowercase()?.split(' ')?.contains("alternate") == true }
        .sortedBy { tag ->
            feedTypes.indexOf(attribute(tag, "type")?.lowercase()).let {
                if (it <
                    0
                ) {
                    Int.MAX_VALUE
                } else {
                    it
                }
            }
        }
        .firstOrNull { tag -> attribute(tag, "type")?.lowercase() in feedTypes }
        ?.let { tag -> attribute(tag, "href") }
        ?.let { href -> base.resolve(href.replace("&amp;", "&"))?.toString() }

    private fun attribute(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE).find(tag)
            ?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
}
