package com.acrovox.core.model

/** Cycle de vie d'un épisode côté utilisateur. */
enum class EpisodeState {
    /** Arrivé au rafraîchissement, dans la boîte de réception, pas encore trié. */
    NEW,

    /**
     * Présent au catalogue lors de l'abonnement, jamais passé par la boîte de réception.
     * Ni gardé ni ignoré : aucune action de synchronisation.
     */
    AVAILABLE,

    /** Gardé par l'utilisateur, pas encore commencé. */
    UNPLAYED,

    IN_PROGRESS,

    PLAYED,

    /** Écarté volontairement depuis la boîte de réception. Synchronisé en `delete` gPodder. */
    IGNORED
}
