package com.acrovox.core.network.feed

import java.text.Normalizer

/**
 * iTunes ne publie pas les flux Radio France. On reconstruit l'adresse de la page de
 * l'émission sur radiofrance.fr, qui annonce son flux ; [FeedFetcher] fait le reste.
 *
 * Heuristique : environ 70 % des émissions sont trouvées. Les séries et rubriques dont la page
 * porte un autre nom que le titre échouent.
 */
object RadioFranceResolver {
    private val stations = mapOf(
        "france inter" to "franceinter",
        "france culture" to "franceculture",
        "franceinfo" to "franceinfo",
        "france info" to "franceinfo",
        "france musique" to "francemusique",
        "fip" to "fip",
        "mouv'" to "mouv",
        "mouv" to "mouv"
    )

    /** Page radiofrance.fr probable, ou null si l'auteur n'est pas une station Radio France. */
    fun pageUrl(title: String, author: String?): String? {
        val station = stations[author?.trim()?.lowercase()] ?: return null
        val slug = slug(title).ifEmpty { return null }
        return "https://www.radiofrance.fr/$station/podcasts/$slug"
    }

    internal fun slug(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
