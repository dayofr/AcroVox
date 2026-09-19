package com.acrovox.core.network.rss

import org.xmlpull.v1.XmlPullParser

/** Appelle [block] pour chaque élément enfant ; [block] doit consommer l'élément jusqu'à sa fin. */
internal inline fun XmlPullParser.forEachChild(block: () -> Unit) {
    val depth = depth
    while (true) {
        when (next()) {
            XmlPullParser.END_DOCUMENT -> return
            XmlPullParser.END_TAG -> if (this.depth == depth) return
            XmlPullParser.START_TAG -> block()
        }
    }
}

/** Texte de l'élément courant (CDATA compris), éléments imbriqués ignorés. Null si vide. */
internal fun XmlPullParser.readText(): String? {
    val depth = depth
    val text = StringBuilder()
    while (true) {
        when (next()) {
            XmlPullParser.TEXT -> if (this.depth == depth) text.append(this.text)
            XmlPullParser.END_TAG -> if (this.depth == depth) break
            XmlPullParser.END_DOCUMENT -> break
        }
    }
    return text.toString().trim().takeIf { it.isNotEmpty() }
}

/** Passe l'élément courant et tout son contenu. */
internal fun XmlPullParser.skip() {
    if (eventType != XmlPullParser.START_TAG) return
    var level = 1
    while (level > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> level++
            XmlPullParser.END_TAG -> level--
            XmlPullParser.END_DOCUMENT -> return
        }
    }
}

internal fun XmlPullParser.attr(name: String): String? = getAttributeValue(null, name)?.trim()?.takeIf {
    it.isNotEmpty()
}
