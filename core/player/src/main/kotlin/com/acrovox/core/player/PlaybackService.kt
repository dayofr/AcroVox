package com.acrovox.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.acrovox.core.data.repository.ChaptersRepository
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.acrovox.core.download.DownloadManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient

/**
 * Lecture en arrière-plan. Résout les épisodes demandés par identifiant, sauvegarde la position,
 * enregistre l'historique et les actions gPodder `play`, enchaîne sur la file.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {
    @Inject lateinit var episodes: EpisodeRepository

    @Inject lateinit var chapters: ChaptersRepository

    @Inject lateinit var settings: PlaybackSettingsRepository

    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var streamCache: Cache

    @Inject lateinit var sleepTimer: SleepTimer

    @Inject lateinit var downloads: DownloadManager

    @Inject lateinit var library: MediaLibraryTree

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null
    private var tracker: Job? = null
    private var loudness: LoudnessEnhancer? = null
    private var volumeNormalization = false

    /** Début du segment d'écoute en cours, pour l'action gPodder `play`. */
    private var segmentStartMs: Long? = null
    private var currentEpisodeId: Long? = null
    private var skipOutroMs = 0L

    override fun onCreate() {
        super.onCreate()
        // Seul le streaming passe par le cache ; les fichiers téléchargés sont lus directement.
        val streamFactory = CacheDataSource.Factory()
            .setCache(streamCache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
        val dataSourceFactory = DefaultDataSource.Factory(this, streamFactory)
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
        session = MediaLibrarySession.Builder(this, QueuePlayer(player), callback)
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
                volumeNormalization = s.volumeNormalization
                applyVolumeNormalization()
            }
        }
        sleepTimer.attach(player, scope)
    }

    /** Normalisation du volume : recrée l'effet à chaque session audio. */
    private fun applyVolumeNormalization() {
        loudness?.release()
        loudness = null
        if (!volumeNormalization) return
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        loudness = runCatching {
            LoudnessEnhancer(sessionId).apply {
                setTargetGain(NORMALIZATION_GAIN_MB)
                enabled = true
            }
        }.getOrNull()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            flushPosition()
            stopSelf()
        }
    }

    override fun onDestroy() {
        endSegment()
        loudness?.release()
        loudness = null
        flushPosition()
        session?.run {
            player.release()
            release()
        }
        session = null
        sleepTimer.detach()
        scope.cancel()
        super.onDestroy()
    }

    /**
     * File pilotée à la main : la timeline ne contient qu'un épisode, donc
     * suivant/précédent sont résolus dans la file plutôt que dans la timeline.
     * Utilisé par Android Auto, le volant, le casque et la notification.
     */
    private inner class QueuePlayer(wrapped: Player) : ForwardingPlayer(wrapped) {
        override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()

        override fun seekToNext() = skipToNext()

        override fun seekToNextMediaItem() = skipToNext()

        override fun seekToPrevious() = skipToPrevious()

        override fun seekToPreviousMediaItem() = skipToPrevious()

        private fun skipToNext() {
            val current = currentEpisodeId ?: episodeIdOf(currentMediaItem) ?: return
            scope.launch { playResolved(nextPlayable(current) ?: return@launch) }
        }

        private fun skipToPrevious() {
            if (currentPosition > RESTART_THRESHOLD_MS) {
                seekTo(0)
                return
            }
            val current = currentEpisodeId ?: episodeIdOf(currentMediaItem) ?: return
            scope.launch { playResolved(episodes.previousInQueue(current) ?: return@launch) }
        }
    }

    private val callback = object : MediaLibrarySession.Callback {
        /**
         * Déclare suivant/précédent : Android Auto affiche les boutons et le volant
         * (ainsi que casque, montre, Assistant) peut les utiliser. La timeline ne
         * contient qu'un épisode, la file est gérée à la main (voir onSeekToNext).
         */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
            return MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                playerCommands
            )
        }

        /**
         * L'interface envoie des identifiants d'épisode ; le service fournit l'URL et les métadonnées.
         * Une demande vocale arrive sans identifiant, avec une recherche.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val id = mediaItems.firstNotNullOfOrNull { requestedEpisode(it) }
            val resolved = id?.let { resolve(it, startPositionMs) }
            if (resolved == null) {
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
            } else {
                MediaSession.MediaItemsWithStartPosition(listOf(resolved.first), 0, resolved.second)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {
            mediaItems.mapNotNull { requestedEpisode(it)?.let { id -> resolve(id, C.TIME_UNSET)?.first } }
                .toMutableList()
        }

        /** Reprise depuis la voiture, un casque Bluetooth ou le système : le dernier épisode écouté. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val last = episodes.getLastPlayed()?.episode?.id
            val resolved = last?.let { resolve(it, C.TIME_UNSET) }
                ?: throw UnsupportedOperationException("Aucun épisode à reprendre")
            MediaSession.MediaItemsWithStartPosition(listOf(resolved.first), 0, resolved.second)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(library.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val children = library.children(parentId)
            if (children == null) {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItemList(children.page(page, pageSize), params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            library.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> = scope.future {
            session.notifySearchResultChanged(browser, query, library.search(query).size, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            LibraryResult.ofItemList(library.search(query).page(page, pageSize), params)
        }
    }

    /** Épisode demandé : par identifiant, ou par recherche vocale (« lis Underscore sur AcroVox »). */
    private suspend fun requestedEpisode(item: MediaItem): Long? = episodeIdOf(item) ?: if (item.mediaId.isEmpty()) {
        library.episodeForVoice(item.requestMetadata.searchQuery)
    } else {
        null
    }

    private fun List<MediaItem>.page(page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0 || pageSize == Int.MAX_VALUE) return this
        return drop(page * pageSize).take(pageSize)
    }

    /**
     * MediaItem complet et position de départ ; applique vitesse et saut d'intro du podcast.
     * Lit le fichier téléchargé s'il existe. Null si l'épisode n'est pas téléchargé et que le
     * streaming est désactivé.
     */
    private suspend fun resolve(episodeId: Long, requestedPositionMs: Long): Pair<MediaItem, Long>? {
        if (downloads.isBlocked(episodeId)) return null
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
        return item.toMediaItem(downloads.localFile(episodeId)?.path) to start
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            endSegment()
            currentEpisodeId = episodeIdOf(mediaItem)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            applyVolumeNormalization()
        }

        /** Tags ID3 du fichier : remplit les chapitres si le flux n'en donne pas. */
        override fun onMetadata(metadata: Metadata) {
            val parsed = metadata.toParsedChapters()
            if (parsed.isEmpty()) return
            val id = currentEpisodeId ?: episodeIdOf(player.currentMediaItem) ?: return
            scope.launch { chapters.storeIfEmpty(id, parsed) }
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
                scope.launch {
                    episodes.savePosition(id, position)
                    val thresholdMs = settings.current().playedThresholdSeconds * 1_000L
                    if (player.playbackState != Player.STATE_ENDED &&
                        !player.playWhenReady &&
                        PlaybackPolicy.isFinished(position, duration, skipOutroMs, thresholdMs)
                    ) {
                        episodes.complete(id)
                    }
                }
                endSegment()
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

    /**
     * Sauvegarde synchrone de la position (sortie voiture, swipe, mise en veille).
     * Bloque au plus [FLUSH_TIMEOUT_MS] : un `scope.launch` annulé par `scope.cancel()`
     * juste après dans [onDestroy] perdait la position quand le process mourait.
     * Les positions nulles sont ignorées par le repository (lecteur sans média).
     */
    private fun flushPosition() {
        val id = currentEpisodeId ?: episodeIdOf(player.currentMediaItem) ?: return
        val pos = runCatching { player.currentPosition }.getOrDefault(0)
        if (pos <= 0) return
        runCatching {
            runBlocking { withTimeoutOrNull(FLUSH_TIMEOUT_MS) { episodes.savePosition(id, pos) } }
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

    /** Lit l'épisode demandé depuis le début estimé (vitesse et intro du podcast). */
    private suspend fun playResolved(episodeId: Long) {
        val resolved = resolve(episodeId, C.TIME_UNSET) ?: return
        player.setMediaItem(resolved.first, resolved.second)
        player.prepare()
        player.play()
    }

    /** Fin d'épisode : écouté, retiré de la file, suivant de la file si la lecture continue est active. */
    private fun onEpisodeEnded(episodeId: Long) {
        tracker?.cancel()
        val start = segmentStartMs
        segmentStartMs = null
        val end = player.duration.takeIf { it != C.TIME_UNSET } ?: player.currentPosition
        scope.launch {
            if (start != null) episodes.recordListening(episodeId, start, end, playerDuration())
            val next = if (settings.current().continuousPlayback) nextPlayable(episodeId) else null
            episodes.complete(episodeId)
            if (sleepTimer.consumeEndOfEpisode()) {
                player.pause()
                return@launch
            }
            if (next != null) {
                playResolved(next)
            } else {
                player.pause()
            }
        }
    }

    /** Suivant de la file, en sautant les épisodes non téléchargés si le streaming est désactivé. */
    private suspend fun nextPlayable(episodeId: Long): Long? {
        var candidate = episodes.nextInQueue(episodeId)
        val seen = mutableSetOf(episodeId)
        while (candidate != null && candidate !in seen && downloads.isBlocked(candidate)) {
            seen += candidate
            candidate = episodes.nextInQueue(candidate)
        }
        return candidate?.takeIf { it !in seen }
    }

    private fun playerDuration(): Long? = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L

        /** Budget max du flush synchrone à la destruction du service. */
        const val FLUSH_TIMEOUT_MS = 2_000L

        /** Passé ce seuil, « précédent » recommence l'épisode au lieu de reculer. */
        const val RESTART_THRESHOLD_MS = 5_000L

        /** Gain de la normalisation du volume, en millibels. */
        const val NORMALIZATION_GAIN_MB = 500
    }
}
