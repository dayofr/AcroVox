package com.acrovox.core.sync

import android.content.Context
import com.acrovox.core.database.AcroVoxDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Déclenche l'envoi quand un abonnement ou une action change, compte connecté.
 * Le délai regroupe les actions rapprochées (tri de la boîte, bandeau « Annuler »).
 */
@Singleton
class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AcroVoxDatabase,
    private val accounts: SyncAccountRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            val connected = accounts.account.map { it?.let { a -> a.server to a.username } }.distinctUntilChanged()
            combine(connected, db.feedDao().observeUrls(), db.episodeActionDao().observeCount()) { key, urls, count ->
                Triple(key, urls.toSet(), count)
            }
                .distinctUntilChanged()
                .collect { (key, _, _) ->
                    if (key == null) return@collect
                    SyncWorker.schedulePeriodic(context)
                    SyncWorker.enqueue(context, DEBOUNCE_SECONDS)
                }
        }
    }

    /** Envoi immédiat (bouton « Synchroniser maintenant », connexion). */
    fun syncNow() = SyncWorker.enqueue(context)

    fun stop() = SyncWorker.cancelAll(context)

    private companion object {
        const val DEBOUNCE_SECONDS = 30L
    }
}
