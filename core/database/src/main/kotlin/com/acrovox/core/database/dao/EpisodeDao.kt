package com.acrovox.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.EpisodeWithFeed
import com.acrovox.core.model.EpisodeState
import kotlinx.coroutines.flow.Flow

data class EpisodeIdentity(val guid: String, @ColumnInfo(name = "media_url") val mediaUrl: String)

data class EpisodeLookup(val id: Long, val guid: String, val mediaUrl: String?)

@Dao
abstract class EpisodeDao {
    @Transaction
    @Query("SELECT * FROM episode WHERE state = 'NEW' ORDER BY pub_date DESC")
    abstract fun observeInbox(): Flow<List<EpisodeWithFeed>>

    @Transaction
    @Query("SELECT * FROM episode WHERE state = 'NEW' AND feed_id = :feedId ORDER BY pub_date DESC")
    abstract fun observeInboxForFeed(feedId: Long): Flow<List<EpisodeWithFeed>>

    @Transaction
    @Query("SELECT * FROM episode WHERE state != 'IGNORED' ORDER BY pub_date DESC LIMIT :limit")
    abstract fun observeLatest(limit: Int): Flow<List<EpisodeWithFeed>>

    @Query("SELECT * FROM episode WHERE feed_id = :feedId ORDER BY pub_date DESC")
    abstract fun observeByFeed(feedId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episode WHERE feed_id = :feedId AND state IN (:states) ORDER BY pub_date DESC")
    abstract fun observeByFeedAndStates(feedId: Long, states: List<EpisodeState>): Flow<List<EpisodeEntity>>

    /** Recherche dans les titres d'épisodes et de podcasts, épisodes ignorés exclus. */
    @Transaction
    @Query(
        """
        SELECT episode.* FROM episode INNER JOIN feed ON feed.id = episode.feed_id
        WHERE episode.state != 'IGNORED'
        AND (episode.title LIKE '%' || :query || '%' OR feed.title LIKE '%' || :query || '%')
        ORDER BY episode.pub_date DESC LIMIT :limit
        """
    )
    abstract suspend fun search(query: String, limit: Int): List<EpisodeWithFeed>

    @Transaction
    @Query("SELECT * FROM episode WHERE feed_id = :feedId AND state != 'IGNORED' ORDER BY pub_date DESC LIMIT :limit")
    abstract suspend fun getRecentForFeed(feedId: Long, limit: Int): List<EpisodeWithFeed>

    /** Dernier épisode écouté, pour reprendre la lecture depuis la voiture ou un casque. */
    @Transaction
    @Query("SELECT * FROM episode WHERE last_played_at IS NOT NULL ORDER BY last_played_at DESC LIMIT 1")
    abstract suspend fun getLastPlayed(): EpisodeWithFeed?

    /** Épisodes commencés, pour la carte « Reprendre l'écoute ». */
    @Transaction
    @Query("SELECT * FROM episode WHERE state = 'IN_PROGRESS' ORDER BY last_played_at DESC LIMIT :limit")
    abstract fun observeInProgress(limit: Int): Flow<List<EpisodeWithFeed>>

    @Transaction
    @Query("SELECT * FROM episode WHERE is_favorite = 1 ORDER BY pub_date DESC")
    abstract fun observeFavorites(): Flow<List<EpisodeWithFeed>>

    @Transaction
    @Query("SELECT * FROM episode WHERE id = :id")
    abstract fun observe(id: Long): Flow<EpisodeWithFeed?>

    @Query("SELECT * FROM episode WHERE id = :id")
    abstract suspend fun get(id: Long): EpisodeEntity?

    @Transaction
    @Query("SELECT * FROM episode WHERE id IN (:ids)")
    abstract suspend fun getWithFeed(ids: List<Long>): List<EpisodeWithFeed>

    @Query("SELECT * FROM episode WHERE feed_id = :feedId AND guid = :guid")
    abstract suspend fun getByGuid(feedId: Long, guid: String): EpisodeEntity?

    @Query("SELECT * FROM episode WHERE media_url = :mediaUrl")
    abstract suspend fun getByMediaUrl(mediaUrl: String): List<EpisodeEntity>

    /** guid et URL média des épisodes d'un podcast, pour reconnaître un épisode dont le guid a changé. */
    @Query("SELECT guid, media_url FROM episode WHERE feed_id = :feedId")
    abstract suspend fun getIdentities(feedId: Long): List<EpisodeIdentity>

    /** Correspondance guid / URL média vers identifiant, pour les imports. */
    @Query("SELECT id, guid, media_url AS mediaUrl FROM episode WHERE feed_id = :feedId")
    abstract suspend fun getLookup(feedId: Long): List<EpisodeLookup>

    @Query("SELECT id FROM episode WHERE state = 'NEW'")
    abstract suspend fun getInboxIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(episode: EpisodeEntity): Long

    @Update
    protected abstract suspend fun update(episode: EpisodeEntity)

    /**
     * Fusionne les épisodes lus dans le flux.
     *
     * Un épisode inconnu est inséré dans l'état [stateForNew]. Un épisode connu voit ses
     * métadonnées mises à jour, sans toucher à l'état, la position ni le favori.
     *
     * @return identifiants des épisodes insérés.
     */
    @Transaction
    open suspend fun mergeFromFeed(
        feedId: Long,
        episodes: List<EpisodeEntity>,
        stateForNew: EpisodeState = EpisodeState.NEW
    ): List<Long> {
        val inserted = mutableListOf<Long>()
        for (parsed in episodes) {
            val existing = getByGuid(feedId, parsed.guid)
            if (existing == null) {
                inserted += insert(parsed.copy(id = 0, feedId = feedId, state = stateForNew))
            } else {
                update(
                    parsed.copy(
                        id = existing.id,
                        feedId = feedId,
                        state = existing.state,
                        positionMs = existing.positionMs,
                        lastPlayedAt = existing.lastPlayedAt,
                        completedAt = existing.completedAt,
                        isFavorite = existing.isFavorite
                    )
                )
            }
        }
        return inserted
    }

    /** Épisodes gardés : sortent de la boîte ou du catalogue, sans toucher aux autres états. */
    @Query("UPDATE episode SET state = 'UNPLAYED' WHERE id IN (:ids) AND state IN ('NEW', 'AVAILABLE')")
    abstract suspend fun markKept(ids: List<Long>)

    @Transaction
    @Query("SELECT * FROM episode WHERE state != 'IGNORED' ORDER BY pub_date ASC LIMIT :limit")
    abstract fun observeOldest(limit: Int): Flow<List<EpisodeWithFeed>>

    @Query("UPDATE episode SET state = :state WHERE id IN (:ids)")
    abstract suspend fun setState(ids: List<Long>, state: EpisodeState)

    @Query(
        """
        UPDATE episode SET position_ms = :positionMs, last_played_at = :at,
            state = CASE WHEN state = 'PLAYED' THEN state ELSE 'IN_PROGRESS' END
        WHERE id = :id
        """
    )
    abstract suspend fun updatePosition(id: Long, positionMs: Long, at: Long)

    @Query("UPDATE episode SET state = 'PLAYED', position_ms = 0, completed_at = :at WHERE id = :id")
    abstract suspend fun markPlayed(id: Long, at: Long)

    @Query("UPDATE episode SET is_favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)
}
