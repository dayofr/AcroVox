package com.acrovox.core.data.repository

import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.ChapterEntity
import com.acrovox.core.network.rss.ParsedChapter
import com.acrovox.core.network.rss.ParsedEpisode
import com.acrovox.core.network.rss.PodcastChaptersFetcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Chapitres d'épisode, trois sources par ordre de priorité : Podlove (dans le
 * flux, stocké au rafraîchissement), JSON `podcast:chapters` (chargé à la
 * demande), tags ID3 (remplis pendant la lecture). On n'écrase jamais des
 * chapitres existants.
 */
@Singleton
class ChaptersRepository @Inject constructor(
    private val db: AcroVoxDatabase,
    private val chaptersFetcher: PodcastChaptersFetcher
) {
    private val chapterDao get() = db.chapterDao()

    fun observe(episodeId: Long): Flow<List<ChapterEntity>> = chapterDao.observe(episodeId)

    /** Stocke les chapitres Podlove lus dans le flux (appelé au rafraîchissement). */
    suspend fun storePodlove(feedId: Long, episodes: List<ParsedEpisode>) {
        val withChapters = episodes.filter { it.chapters.isNotEmpty() }
        if (withChapters.isEmpty()) return
        for (parsed in withChapters) {
            val id = db.episodeDao().getByGuid(feedId, parsed.guid)?.id ?: continue
            chapterDao.replace(id, parsed.chapters.map { it.toEntity(id) })
        }
    }

    /**
     * Charge le JSON `podcast:chapters` si l'épisode n'a pas encore de
     * chapitres. @return true si des chapitres sont disponibles après l'appel.
     */
    suspend fun ensureLoaded(episodeId: Long): Boolean {
        if (chapterDao.get(episodeId).isNotEmpty()) return true
        val url = db.episodeDao().get(episodeId)?.chaptersUrl ?: return false
        val chapters = chaptersFetcher.fetch(url) ?: return false
        chapterDao.replace(episodeId, chapters.map { it.toEntity(episodeId) })
        return true
    }

    /** Remplit depuis les tags du fichier si l'épisode n'a pas de chapitres. */
    suspend fun storeIfEmpty(episodeId: Long, chapters: List<ParsedChapter>): Boolean {
        if (chapters.isEmpty() || chapterDao.get(episodeId).isNotEmpty()) return false
        chapterDao.replace(episodeId, chapters.map { it.toEntity(episodeId) })
        return true
    }

    private fun ParsedChapter.toEntity(episodeId: Long) = ChapterEntity(
        episodeId = episodeId,
        startMs = startMs,
        title = title,
        imageUrl = imageUrl,
        url = url
    )
}
