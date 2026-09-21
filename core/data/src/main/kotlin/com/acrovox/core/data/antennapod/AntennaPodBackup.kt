package com.acrovox.core.data.antennapod

import android.database.sqlite.SQLiteDatabase

/** Abonnement lu dans l'export AntennaPod. */
data class ApFeed(
    /** Identifiant `Feeds.id`, pour rattacher les épisodes. */
    val rowId: Long,
    val title: String,
    val customTitle: String?,
    val feedUrl: String,
    val link: String?,
    val description: String?,
    val language: String?,
    val author: String?,
    val imageUrl: String?,
    val playbackSpeed: Float,
    val skipIntroMs: Long,
    val skipOutroMs: Long
)

/** Épisode lu dans l'export, avec son média. */
data class ApItem(
    /** Identifiant `FeedItems.id`, pour la file et les favoris. */
    val rowId: Long,
    val feedRowId: Long,
    val title: String,
    val guid: String?,
    val link: String?,
    val description: String?,
    val imageUrl: String?,
    /** -1 nouveau, 0 non lu, 1 lu. */
    val read: Int,
    val mediaUrl: String?,
    val positionMs: Long,
    val completedAt: Long,
    val lastPlayedAt: Long
)

data class ApBackup(
    val feeds: List<ApFeed>,
    val items: List<ApItem>,
    /** Identifiants `FeedItems.id` dans l'ordre de la file. */
    val queueItemIds: List<Long>,
    /** Identifiants `FeedItems.id` favoris. */
    val favoriteItemIds: Set<Long>
)

/**
 * Lit un export AntennaPod (SQLite) sans Room : le schéma diffère.
 * Les colonnes manquantes (vieilles versions) valent null / 0.
 */
object AntennaPodBackup {
    fun read(path: String): ApBackup {
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val feeds = mutableListOf<ApFeed>()
            db.query("Feeds", null, null, null, null, null, null).use { c ->
                val col = c.columns()
                val idIndex = col["id"] ?: -1
                while (c.moveToNext()) {
                    val url = c.optText(col, "download_url") ?: continue
                    feeds += ApFeed(
                        rowId = if (idIndex >= 0) c.getLong(idIndex) else -1,
                        title = c.optText(col, "title").orEmpty(),
                        customTitle = c.optText(col, "custom_title"),
                        feedUrl = url,
                        link = c.optText(col, "link"),
                        description = c.optText(col, "description"),
                        language = c.optText(col, "language"),
                        author = c.optText(col, "author"),
                        imageUrl = c.optText(col, "image_url"),
                        playbackSpeed = c.optFloat(col, "feed_playback_speed", -1f),
                        skipIntroMs = c.optLong(col, "feed_skip_intro", 0),
                        skipOutroMs = c.optLong(col, "feed_skip_ending", 0)
                    )
                }
            }
            val items = mutableListOf<ApItem>()
            db.rawQuery(
                "SELECT i.*, m.download_url AS ap_media_url, m.position AS ap_position," +
                    " m.playback_completion_date AS ap_completed, m.last_played_time AS ap_last_played" +
                    " FROM FeedItems i LEFT JOIN FeedMedia m ON m.feeditem = i.id",
                null
            ).use { c ->
                val col = c.columns()
                val idIndex = col["id"] ?: 0
                while (c.moveToNext()) {
                    items += ApItem(
                        rowId = c.getLong(idIndex),
                        feedRowId = c.optLong(col, "feed", -1),
                        title = c.optText(col, "title").orEmpty(),
                        guid = c.optText(col, "item_identifier"),
                        link = c.optText(col, "link"),
                        description = c.optText(col, "description"),
                        imageUrl = c.optText(col, "image_url"),
                        read = c.optInt(col, "read", 0),
                        mediaUrl = c.optText(col, "ap_media_url"),
                        positionMs = c.optLong(col, "ap_position", 0),
                        completedAt = c.optLong(col, "ap_completed", 0),
                        lastPlayedAt = c.optLong(col, "ap_last_played", 0)
                    )
                }
            }
            val queue = mutableListOf<Long>()
            if (db.hasTable("Queue")) {
                db.query("Queue", arrayOf("feeditem"), null, null, null, null, "id").use { c ->
                    while (c.moveToNext()) queue += c.getLong(0)
                }
            }
            val favorites = mutableSetOf<Long>()
            if (db.hasTable("Favorites")) {
                db.query("Favorites", arrayOf("feeditem"), null, null, null, null, null).use { c ->
                    while (c.moveToNext()) favorites += c.getLong(0)
                }
            }
            return ApBackup(feeds, items, queue, favorites)
        }
    }

    private fun android.database.Cursor.columns(): Map<String, Int> =
        (0 until columnCount).associate { getColumnName(it).lowercase() to it }

    private fun android.database.Cursor.optText(col: Map<String, Int>, name: String): String? {
        val i = col[name] ?: return null
        return if (isNull(i)) null else getString(i)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun android.database.Cursor.optText(index: Int): String? =
        if (isNull(index)) null else getString(index)?.trim()?.takeIf { it.isNotEmpty() }

    private fun android.database.Cursor.optInt(col: Map<String, Int>, name: String, default: Int): Int {
        val i = col[name] ?: return default
        return if (isNull(i)) default else getInt(i)
    }

    private fun android.database.Cursor.optLong(col: Map<String, Int>, name: String, default: Long): Long {
        val i = col[name] ?: return default
        return if (isNull(i)) default else getLong(i)
    }

    private fun android.database.Cursor.optFloat(col: Map<String, Int>, name: String, default: Float): Float {
        val i = col[name] ?: return default
        return if (isNull(i)) default else getFloat(i)
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use {
            it.moveToFirst()
        }
}
