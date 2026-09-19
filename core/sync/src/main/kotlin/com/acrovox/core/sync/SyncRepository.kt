package com.acrovox.core.sync

import com.acrovox.core.database.AcroVoxDatabase
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncResult {
    data class Success(val subscriptionsAdded: Int, val subscriptionsRemoved: Int, val actionsSent: Int) : SyncResult

    data object NotConnected : SyncResult

    /** @param retryable une erreur réseau passagère : réessayer plus tard. */
    data class Failed(val message: String, val retryable: Boolean) : SyncResult
}

/**
 * Envoi vers un serveur gPodder, dans un seul sens : l'app envoie ses abonnements et ses
 * actions d'épisodes, elle ne lit rien du serveur.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val db: AcroVoxDatabase,
    private val client: GpodderClient,
    private val accounts: SyncAccountRepository,
    private val clock: Clock
) {
    private val mutex = Mutex()

    /**
     * Vérifie les identifiants, déclare cet appareil et enregistre le compte.
     * Le premier envoi transmet tous les abonnements et les actions déjà enregistrées.
     */
    suspend fun connect(serverInput: String, username: String, password: String, deviceCaption: String) {
        val server = GpodderClient.parseServer(serverInput)
            ?: throw GpodderProtocolException("Adresse du serveur invalide")
        val credentials = GpodderCredentials(server, username.trim(), password)
        client.login(credentials)
        val deviceId = "acrovox-" + (1..DEVICE_SUFFIX_LENGTH).map { DEVICE_CHARS[Random.nextInt(DEVICE_CHARS.length)] }
            .joinToString("")
        client.registerDevice(credentials, deviceId, deviceCaption)
        accounts.save(credentials, deviceId)
    }

    suspend fun disconnect() = accounts.clear()

    fun observePendingActions(): Flow<Int> = db.episodeActionDao().observeCount()

    suspend fun sync(): SyncResult = mutex.withLock {
        val (credentials, deviceId) = accounts.credentials() ?: return SyncResult.NotConnected
        try {
            val current = db.feedDao().getUrls().map(String::trim).toSet()
            val sent = accounts.sentSubscriptions()
            val add = current - sent
            val remove = sent - current
            if (add.isNotEmpty() || remove.isNotEmpty()) {
                client.uploadSubscriptions(credentials, deviceId, add.sorted(), remove.sorted())
                accounts.setSentSubscriptions(current)
            }

            val actions = db.episodeActionDao()
            var sentActions = 0
            while (true) {
                val batch = actions.getPending(BATCH_SIZE)
                if (batch.isEmpty()) break
                client.uploadEpisodeActions(credentials, deviceId, batch)
                // Retirées seulement après un envoi réussi : le serveur ne dédoublonne pas.
                actions.delete(batch.map { it.id })
                sentActions += batch.size
            }
            accounts.recordSuccess(clock.millis())
            SyncResult.Success(add.size, remove.size, sentActions)
        } catch (e: GpodderAuthException) {
            fail(e.message ?: "Identifiants refusés", retryable = false)
        } catch (e: GpodderProtocolException) {
            fail(e.message ?: "Réponse inattendue du serveur", retryable = false)
        } catch (e: IOException) {
            fail("Serveur injoignable : ${e.message ?: "erreur réseau"}", retryable = true)
        }
    }

    private suspend fun fail(message: String, retryable: Boolean): SyncResult {
        accounts.recordError(message)
        return SyncResult.Failed(message, retryable)
    }

    private companion object {
        const val BATCH_SIZE = 100
        const val DEVICE_SUFFIX_LENGTH = 6
        const val DEVICE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
