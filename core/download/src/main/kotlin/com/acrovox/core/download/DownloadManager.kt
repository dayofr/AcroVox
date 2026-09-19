package com.acrovox.core.download

import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.DownloadEntity
import com.acrovox.core.database.entity.DownloadWithEpisode
import com.acrovox.core.model.DownloadState
import com.acrovox.core.model.DownloadStatus
import java.io.File
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Question posée à l'utilisateur, affichée par la racine de l'interface. */
sealed interface DownloadPrompt {
    /** « Wi-Fi uniquement » et pas de Wi-Fi : télécharger sur données mobiles ou attendre. */
    data class ChooseNetwork(val episodeIds: List<Long>) : DownloadPrompt

    /** « Téléchargés uniquement » et épisode absent : proposer de le télécharger. */
    data class NotDownloaded(val episodeId: Long) : DownloadPrompt

    data class ConfirmDelete(val episodeId: Long, val title: String) : DownloadPrompt
}

/**
 * Téléchargements manuels. Rien n'est téléchargé sans une demande explicite de l'utilisateur.
 */
@Singleton
class DownloadManager @Inject constructor(
    private val db: AcroVoxDatabase,
    private val scheduler: DownloadScheduler,
    private val settings: DownloadSettingsRepository,
    private val network: NetworkMonitor,
    private val files: DownloadFiles,
    private val clock: Clock
) {
    private val dao get() = db.downloadDao()

    private val _prompt = MutableStateFlow<DownloadPrompt?>(null)
    val prompt: StateFlow<DownloadPrompt?> = _prompt.asStateFlow()

    val unmetered: Flow<Boolean> get() = network.unmetered

    fun observeStates(): Flow<Map<Long, DownloadState>> =
        dao.observeAll().map { list -> list.associate { it.episodeId to it.toState() } }

    fun observeState(episodeId: Long): Flow<DownloadState> = dao.observe(episodeId).map { it.toState() }

    fun observeDownloads(): Flow<List<DownloadWithEpisode>> = dao.observeWithEpisodes()

    fun observeUsedBytes(): Flow<Long> = dao.observeUsedBytes()

    fun freeBytes(): Long = files.freeBytes()

    /** Bouton de téléchargement d'un épisode : lance, annule, ou propose de supprimer le fichier. */
    suspend fun toggle(episodeId: Long) {
        val download = dao.get(episodeId)
        when (download?.status) {
            null, DownloadStatus.FAILED -> request(listOf(episodeId))
            DownloadStatus.COMPLETED -> {
                val title = db.episodeDao().get(episodeId)?.title.orEmpty()
                _prompt.value = DownloadPrompt.ConfirmDelete(episodeId, title)
            }
            else -> cancel(listOf(episodeId))
        }
    }

    /**
     * Demande de téléchargement. En « Wi-Fi uniquement » hors Wi-Fi, demande à l'utilisateur
     * s'il veut télécharger tout de suite ou attendre le Wi-Fi.
     */
    suspend fun request(episodeIds: List<Long>) {
        val existing = dao.get(episodeIds).filter { it.status != DownloadStatus.FAILED }.map { it.episodeId }.toSet()
        val todo = episodeIds.filterNot { it in existing }
        if (todo.isEmpty()) return
        if (settings.current().wifiOnly && !network.isUnmetered()) {
            _prompt.value = DownloadPrompt.ChooseNetwork(todo)
        } else {
            enqueue(todo, wifiOnly = settings.current().wifiOnly)
        }
    }

    /** Télécharge sur n'importe quel réseau, données mobiles comprises. */
    suspend fun downloadNow(episodeIds: List<Long>) {
        dismissPrompt()
        enqueue(episodeIds, wifiOnly = false)
    }

    /** Met en file : le téléchargement partira au prochain Wi-Fi. */
    suspend fun waitForWifi(episodeIds: List<Long>) {
        dismissPrompt()
        enqueue(episodeIds, wifiOnly = true)
    }

    fun dismissPrompt() {
        _prompt.value = null
    }

    /** Arrête et oublie des téléchargements en cours ou en attente. */
    suspend fun cancel(episodeIds: List<Long>) {
        episodeIds.forEach { id ->
            scheduler.cancel(id)
            dao.delete(id)
            files.partFile(id).delete()
        }
    }

    /**
     * Supprime les fichiers. L'épisode garde son état : libérer de la place n'est pas l'ignorer,
     * aucune action gPodder n'est enregistrée.
     */
    suspend fun delete(episodeIds: List<Long>) {
        dao.get(episodeIds).forEach { download ->
            scheduler.cancel(download.episodeId)
            download.localPath?.let { File(it).delete() }
            files.partFile(download.episodeId).delete()
            dao.delete(download.episodeId)
        }
    }

    /** Fichier d'un épisode téléchargé, s'il est encore sur le disque. */
    suspend fun localFile(episodeId: Long): File? {
        val download = dao.get(episodeId) ?: return null
        if (download.status != DownloadStatus.COMPLETED) return null
        return download.localPath?.let(::File)?.takeIf { it.exists() }
    }

    /** Lecture en streaming interdite par le réglage et fichier absent. */
    suspend fun isBlocked(episodeId: Long): Boolean = settings.current().downloadedOnly && localFile(episodeId) == null

    /**
     * Vérifie qu'un épisode peut être lu. Sinon, propose de le télécharger.
     *
     * @return true si la lecture peut commencer.
     */
    suspend fun checkPlayable(episodeId: Long): Boolean {
        if (!isBlocked(episodeId)) return true
        _prompt.value = DownloadPrompt.NotDownloaded(episodeId)
        return false
    }

    private suspend fun enqueue(episodeIds: List<Long>, wifiOnly: Boolean) {
        val now = clock.millis()
        episodeIds.forEach { id ->
            val status = if (wifiOnly) DownloadStatus.WAITING_FOR_WIFI else DownloadStatus.QUEUED
            dao.upsert(DownloadEntity(episodeId = id, status = status, createdAt = now))
            scheduler.enqueue(id, wifiOnly)
        }
    }
}

fun DownloadEntity?.toState(): DownloadState = when (this?.status) {
    null -> DownloadState.None
    DownloadStatus.QUEUED -> DownloadState.Queued(waitingForWifi = false)
    DownloadStatus.WAITING_FOR_WIFI -> DownloadState.Queued(waitingForWifi = true)
    DownloadStatus.RUNNING -> DownloadState.Running(
        totalBytes?.takeIf {
            it > 0
        }?.let { bytesDownloaded.toFloat() / it }
    )
    DownloadStatus.COMPLETED -> DownloadState.Completed
    DownloadStatus.FAILED -> DownloadState.Failed(error)
}
