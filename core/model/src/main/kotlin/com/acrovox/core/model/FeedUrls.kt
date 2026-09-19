package com.acrovox.core.model

/**
 * Normalise une adresse saisie ou collée : schémas `feed://`, `itpc://`, `pcast://`,
 * `podcast://` convertis en https, schéma absent ajouté. Null si ce n'est pas une adresse.
 */
fun normalizeFeedUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed.contains(' ')) return null
    val explicitScheme = trimmed.contains("://") || trimmed.startsWith("feed:", ignoreCase = true)
    val withScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        Regex("^(feed|itpc|pcast|podcast)://", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
            "https://" + trimmed.substringAfter("://")
        trimmed.startsWith("feed:", ignoreCase = true) -> trimmed.substringAfter(':')
        else -> "https://$trimmed"
    }
    val host = withScheme.substringAfter("://").substringBefore('/').substringBefore('?')
    // Sans schéma, exiger un point évite de prendre un mot-clé pour une adresse.
    val plausibleHost = host.isNotEmpty() && (explicitScheme || (host.contains('.') && host.length > 3))
    return withScheme.takeIf { plausibleHost }
}

/** Vrai si la saisie ressemble à une adresse plutôt qu'à des mots-clés. */
fun looksLikeUrl(input: String): Boolean {
    val trimmed = input.trim()
    return trimmed.contains("://") ||
        (!trimmed.contains(' ') && Regex("""^[\w-]+(\.[\w-]+)+(/.*)?$""").matches(trimmed))
}
