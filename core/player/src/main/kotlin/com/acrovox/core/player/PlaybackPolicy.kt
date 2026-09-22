package com.acrovox.core.player

/** Règles de lecture sans dépendance Android : reprise, saut d'intro, fin d'épisode. */
object PlaybackPolicy {
    /** Reste à écouter en dessous duquel l'épisode est considéré comme écouté. */
    const val PLAYED_REMAINING_MS = 30_000L

    private const val SHORT_PAUSE_MS = 60_000L
    private const val LONG_PAUSE_MS = 3_600_000L
    private const val SHORT_REWIND_MS = 3_000L
    private const val LONG_REWIND_MS = 10_000L

    /**
     * Position de départ d'un épisode.
     *
     * Reprise avec un léger recul après une pause (3 s au-delà d'une minute, 10 s au-delà
     * d'une heure) pour retrouver le fil. Un épisode fini ou presque repart du début.
     * L'introduction du podcast est sautée si la reprise tombe dedans.
     */
    fun startPosition(
        savedMs: Long,
        lastPlayedAt: Long?,
        now: Long,
        durationMs: Long?,
        skipIntroMs: Long,
        playedThresholdMs: Long = PLAYED_REMAINING_MS
    ): Long {
        val resumed = when {
            savedMs <= 0 -> 0L
            durationMs != null && durationMs - savedMs <= playedThresholdMs -> 0L
            else -> {
                val paused = lastPlayedAt?.let { now - it } ?: 0L
                val rewind = when {
                    paused >= LONG_PAUSE_MS -> LONG_REWIND_MS
                    paused >= SHORT_PAUSE_MS -> SHORT_REWIND_MS
                    else -> 0L
                }
                (savedMs - rewind).coerceAtLeast(0)
            }
        }
        return if (resumed < skipIntroMs) skipIntroMs else resumed
    }

    /** Vrai si l'écoute s'arrête assez près de la fin (ou dans l'outro à sauter) pour marquer l'épisode écouté. */
    fun isFinished(
        positionMs: Long,
        durationMs: Long?,
        skipOutroMs: Long = 0,
        playedThresholdMs: Long = PLAYED_REMAINING_MS
    ): Boolean {
        if (durationMs == null || durationMs <= 0) return false
        return durationMs - positionMs <= maxOf(playedThresholdMs, skipOutroMs)
    }

    /** Vrai si la lecture est entrée dans l'outro à sauter. */
    fun inOutro(positionMs: Long, durationMs: Long?, skipOutroMs: Long): Boolean {
        if (skipOutroMs <= 0 || durationMs == null || durationMs <= skipOutroMs) return false
        return positionMs >= durationMs - skipOutroMs
    }
}
