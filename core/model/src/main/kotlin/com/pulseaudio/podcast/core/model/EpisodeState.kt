package com.pulseaudio.podcast.core.model

/** Cycle de vie d'un épisode côté utilisateur. */
enum class EpisodeState {
    /** Arrivé au rafraîchissement, dans la boîte de réception, pas encore trié. */
    NEW,

    /** Gardé par l'utilisateur, pas encore commencé. */
    UNPLAYED,

    IN_PROGRESS,

    PLAYED,

    /** Écarté volontairement depuis la boîte de réception. Synchronisé en `delete` gPodder. */
    IGNORED,
}
