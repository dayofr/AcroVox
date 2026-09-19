package com.acrovox.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feed",
    indices = [Index(value = ["feed_url"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "feed_url") val feedUrl: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    val link: String? = null,
    val language: String? = null,
    /** Catégories iTunes, séparées par des virgules. */
    val categories: String? = null,
    @ColumnInfo(name = "subscribed_at") val subscribedAt: Long,
    @ColumnInfo(name = "last_refresh_at") val lastRefreshAt: Long? = null,
    /** Cache HTTP pour les requêtes conditionnelles. */
    val etag: String? = null,
    @ColumnInfo(name = "last_modified") val lastModified: String? = null,
    /** Vitesse propre au podcast ; null = réglage global. */
    @ColumnInfo(name = "playback_speed") val playbackSpeed: Float? = null,
    @ColumnInfo(name = "skip_intro_ms") val skipIntroMs: Long = 0,
    @ColumnInfo(name = "skip_outro_ms") val skipOutroMs: Long = 0,
    @ColumnInfo(name = "notify_new_episodes") val notifyNewEpisodes: Boolean = false
)
