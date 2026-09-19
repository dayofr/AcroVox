package com.acrovox.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Lecture en arrière-plan. Résout les épisodes demandés par identifiant, sauvegarde la position,
 * enregistre l'historique et les actions gPodder `play`, enchaîne sur la file.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var episodes: EpisodeRepository

    @Inject lateinit var settings: PlaybackSettingsRepository

    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var streamCache: Cache

    @Inject lateinit var sleepTimer: SleepTimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var tracker: Job? = null

    /** Début du segment d'écoute en cours, pour l'action gPodder `play`. */
    private var segmentStartMs: Long? = null
    private var currentEpisodeId: Long? = null
    private var skipOutroMs = 0L

    override fun onCreate() {
        super.onCreate()
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(streamCache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this, httpFactory))
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                /* handleAudioFocus = */
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
        player.addListener(listener)

        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivity = launch?.let {
            PendingIntent.getActivity(
                this,
                0,
                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        session = MediaSession.Builder(this, player)
            .setCallback(callback)
            .apply { sessionActivity?.let(::setSessionActivity) }
            .setMediaButtonPreferences(
                listOf(
                    CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
                        .setDisplayName("Reculer")
                        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                        .setSlots(CommandButton.SLOT_BACK)
                        .build(),
                    CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
                        .setDisplayName("Avancer")
                        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                        .setSlots(CommandButton.SLOT_FORWARD)
                        .build()
                )
            )
            .build()

        scope.launch {
            settings.settings.distinctUntilChanged().collect { s ->
                player.skipSilenceEnabled = s.skipSilence
            }
        }
        sleepTimer.attach(player, scope)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        endSegment()
        currentEpisodeId?.let { id ->
            val pos = player.currentPosition
            scope.launch { episodes.savePosition(id, pos) }
        }
        session?.run {
            player.release()
            release()
        }
        session = null
        sleepTimer.detach()
        scope.cancel()
        super.onDestroy()
    }

    private val callback = object : MediaSession.Callback {
        /** L'interface envoie des identifiants d'épisode ; le service fournit l'URL et les métadonnées. */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val id = mediaItems.firstNotNullOfOrNull { episodeIdOf(it) }
            val resolved = id?.let { resolve(it, startPositionMs) }
            if (resolved == null) {
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
            } else {
                MediaSession.MediaItemsWithStartPosition(listOf(resolved.first), 0, resolved.second)
            }
        }
    }

    /** MediaItem complet et position de départ ; applique vitesse et saut d'intro du podcast. */
    private suspend fun resolve(episodeId: Long, requestedPositionMs: Long): Pair<MediaItem, Long>? {
        val item = episodes.getWithFeed(episodeId) ?: return null
        val (episode, feed) = item
        val start = if (requestedPositionMs != C.TIME_UNSET && requestedPositionMs >= 0) {
            requestedPositionMs
        } else {
            PlaybackPolicy.startPosition(
                episode.positionMs,
                episode.lastPlayedAt,
                System.currentTimeMillis(),
                episode.durationMs,
                feed.skipIntroMs
            )
        }
        val global = settings.current()
        player.setPlaybackSpeed(feed.playbackSpeed ?: global.speed)
        skipOutroMs = feed.skipOutroMs
        return item.toMediaItem() to start
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            endSegment()
            currentEpisodeId = episodeIdOf(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val id = currentEpisodeId ?: episodeIdOf(player.currentMediaItem) ?: return
            currentEpisodeId = id
            if (isPlaying) {
                segmentStartMs = player.currentPosition
                scope.launch { episodes.onPlaybackStarted(id, player.currentPosition) }
                startTracking(id)
            } else {
                tracker?.cancel()
                val position = player.currentPosition
                val duration = player.duration.takeIf { it != C.TIME_UNSET }
                scope.launch { episodes.savePosition(id, position) }
                endSegment()
                if (player.playbackState != Player.STATE_ENDED &&
                    !player.playWhenReady &&
                    PlaybackPolicy.isFinished(position, duration, skipOutroMs)
                ) {
                    scope.launch { episodes.complete(id) }
                }
            }
        }

        /** Un saut termine le segment écouté : sauter la fin ne compte pas comme l'avoir écoutée. */
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            val id = currentEpisodeId ?: return
            val start = segmentStartMs ?: return
            val end = oldPosition.positionMs
            val total = playerDuration()
            scope.launch { episodes.recordListening(id, start, end, total) }
            segmentStartMs = newPosition.positionMs
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) currentEpisodeId?.let(::onEpisodeEnded)
        }
    }

    /** Sauvegarde toutes les 5 s et saute l'outro. */
    private fun startTracking(episodeId: Long) {
        tracker?.cancel()
        tracker = scope.launch {
            while (isActive) {
                delay(SAVE_INTERVAL_MS)
                val position = player.currentPosition
                episodes.savePosition(episodeId, position)
                val duration = player.duration.takeIf { it != C.TIME_UNSET }
                if (PlaybackPolicy.inOutro(position, duration, skipOutroMs)) {
                    onEpisodeEnded(episodeId)
                    return@launch
                }
            }
        }
    }

    private fun endSegment() {
        val id = currentEpisodeId ?: return
        val start = segmentStartMs ?: return
        segmentStartMs = null
        val position = player.currentPosition
        val total = playerDuration()
        scope.launch { episodes.recordListening(id, start, position, total) }
    }

    /** Fin d'épisode : écouté, retiré de la file, suivant de la file si la lecture continue est active. */
    private fun onEpisodeEnded(episodeId: Long) {
        tracker?.cancel()
        val start = segmentStartMs
        segmentStartMs = null
        val end = player.duration.takeIf { it != C.TIME_UNSET } ?: player.currentPosition
        scope.launch {
            if (start != null) episodes.recordListening(episodeId, start, end, playerDuration())
            val next = if (settings.current().continuousPlayback) episodes.nextInQueue(episodeId) else null
            episodes.complete(episodeId)
            if (sleepTimer.consumeEndOfEpisode()) {
                player.pause()
                return@launch
            }
            if (next != null) {
                val resolved = resolve(next, C.TIME_UNSET) ?: return@launch
                player.setMediaItem(resolved.first, resolved.second)
                player.prepare()
                player.play()
            } else {
                player.pause()
            }
        }
    }

    private fun playerDuration(): Long? = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}
