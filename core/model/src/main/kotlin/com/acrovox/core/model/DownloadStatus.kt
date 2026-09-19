package com.acrovox.core.model

enum class DownloadStatus {
    /** En attente de son tour, sur n'importe quel réseau. */
    QUEUED,

    /** En attente d'un réseau Wi-Fi (non facturé à l'usage). */
    WAITING_FOR_WIFI,
    RUNNING,
    COMPLETED,
    FAILED
}

/** Ce que l'interface montre d'un téléchargement. */
sealed interface DownloadState {
    data object None : DownloadState

    data class Queued(val waitingForWifi: Boolean) : DownloadState

    /** @param progress de 0 à 1, null si la taille est inconnue. */
    data class Running(val progress: Float?) : DownloadState

    data object Completed : DownloadState

    data class Failed(val reason: String?) : DownloadState
}
