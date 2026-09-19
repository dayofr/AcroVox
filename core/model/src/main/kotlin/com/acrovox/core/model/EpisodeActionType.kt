package com.acrovox.core.model

/** Actions d'épisode de l'API gPodder. */
enum class EpisodeActionType {
    /** Écoute : début, position, durée totale. */
    PLAY,
    DOWNLOAD,

    /** Suppression volontaire ou passage en [EpisodeState.IGNORED]. */
    DELETE,

    /** Retour en non lu. */
    NEW
}
