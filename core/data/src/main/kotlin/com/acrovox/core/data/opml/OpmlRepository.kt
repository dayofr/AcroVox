package com.acrovox.core.data.opml

import android.content.Context
import android.net.Uri
import com.acrovox.core.data.repository.SubscriptionRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.network.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class OpmlImportResult(
    val subscribed: Int,
    val alreadySubscribed: Int,
    /** Titre du podcast et raison de l'échec. */
    val failures: List<Pair<String, String>>
)

class OpmlRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AcroVoxDatabase,
    private val subscriptions: SubscriptionRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher
) {
    suspend fun read(uri: Uri): List<OpmlOutline> = withContext(io) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw OpmlException("Fichier introuvable")
        stream.use(Opml::read)
    }

    /**
     * Abonne aux podcasts donnés, 4 à la fois.
     *
     * @param inboxLatest mettre le dernier épisode de chaque podcast dans la boîte de réception.
     * Faux par défaut : une migration ne doit pas remplir la boîte d'un coup.
     * @param onProgress nombre de podcasts traités.
     */
    suspend fun import(
        outlines: List<OpmlOutline>,
        inboxLatest: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): OpmlImportResult = coroutineScope {
        val done = AtomicInteger()
        val semaphore = Semaphore(PARALLEL_IMPORTS)
        val outcomes = outlines.map { outline ->
            async {
                semaphore.withPermit {
                    val outcome = importOne(outline, inboxLatest)
                    onProgress(done.incrementAndGet())
                    outline to outcome
                }
            }
        }.awaitAll()
        OpmlImportResult(
            subscribed = outcomes.count { it.second == Outcome.Subscribed },
            alreadySubscribed = outcomes.count { it.second == Outcome.AlreadySubscribed },
            failures = outcomes.mapNotNull { (outline, outcome) ->
                (outcome as? Outcome.Failed)?.let {
                    outline.title to
                        it.message
                }
            }
        )
    }

    private sealed interface Outcome {
        data object Subscribed : Outcome

        data object AlreadySubscribed : Outcome

        data class Failed(val message: String) : Outcome
    }

    private suspend fun importOne(outline: OpmlOutline, inboxLatest: Boolean): Outcome = try {
        if (db.feedDao().getByUrl(outline.xmlUrl) != null) {
            Outcome.AlreadySubscribed
        } else {
            // Le podcast peut être suivi sous une autre adresse (flux déménagé depuis l'export).
            val preview = subscriptions.preview(outline.xmlUrl)
            if (subscriptions.findSubscribed(preview) != null) {
                Outcome.AlreadySubscribed
            } else {
                subscriptions.subscribe(preview, inboxLatest = inboxLatest)
                Outcome.Subscribed
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failed(e.message ?: "Erreur inconnue")
    }

    /** @return nombre de podcasts exportés. */
    suspend fun export(uri: Uri): Int = withContext(io) {
        val outlines = db.feedDao().getAll().map { OpmlOutline(it.title, it.feedUrl, it.link) }
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw OpmlException("Écriture impossible")
        stream.use { Opml.write(outlines, it) }
        outlines.size
    }

    private companion object {
        const val PARALLEL_IMPORTS = 4
    }
}
