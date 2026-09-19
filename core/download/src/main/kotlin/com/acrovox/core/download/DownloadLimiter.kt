package com.acrovox.core.download

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Semaphore

/** Nombre de téléchargements simultanés ; les autres restent en file. */
@Singleton
class DownloadLimiter @Inject constructor() {
    val permits = Semaphore(MAX_PARALLEL)

    private companion object {
        const val MAX_PARALLEL = 2
    }
}
