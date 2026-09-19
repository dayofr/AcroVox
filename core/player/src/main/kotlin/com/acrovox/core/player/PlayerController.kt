package com.acrovox.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.data.settings.PlaybackSettingsRepository
import com.acrovox.core.download.DownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ce que l'interface affiche du lecteur. */
data class PlayerState(
    val episodeId: Long? = null,
    val title: String = "",
    val podcastTitle: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val sleepTimer: SleepTimerState = SleepTimerState.Off
) {
    val hasEpisode: Boolean get() = episodeId != null
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Point d'entrée de l'interface vers la lecture. Se connecte au [PlaybackService]
 * par un MediaController et publie un [PlayerState].
 */
@Singleton
class PlayerController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val episodes: EpisodeRepository,
    private val settings: PlaybackSettingsRepository,
    private val sleepTimer: SleepTimer,
    private val downloads: DownloadManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        scope.launch { sleepTimer.state.collect { timer -> _state.update { it.copy(sleepTimer = timer) } } }
        scope.launch { restoreLastEpisode() }
    }

    /**
     * Lit un épisode, depuis [positionMs] ou sa position sauvegardée. Si le streaming est désactivé
     * et l'épisode absent, propose de le télécharger au lieu de le lire.
     */
    fun play(episodeId: Long, positionMs: Long? = null) {
        scope.launch {
            if (!downloads.checkPlayable(episodeId)) return@launch
            val c = connect()
            if (episodeIdOf(c.currentMediaItem) == episodeId) {
                positionMs?.let(c::seekTo)
            } else {
                c.setMediaItem(episodeRequest(episodeId), positionMs ?: C.TIME_UNSET)
            }
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() {
        scope.launch {
            val c = connect()
            if (c.isPlaying) {
                c.pause()
            } else {
                if (c.mediaItemCount ==
                    0
                ) {
                    state.value.episodeId?.let { c.setMediaItem(episodeRequest(it), C.TIME_UNSET) }
                }
                c.prepare()
                c.play()
            }
        }
    }

    fun seekTo(positionMs: Long) = command { it.seekTo(positionMs.coerceAtLeast(0)) }

    fun skipBack() {
        scope.launch {
            val back = settings.current().skipBackSeconds * 1000L
            val c = connect()
            c.seekTo((c.currentPosition - back).coerceAtLeast(0))
        }
    }

    fun skipForward() {
        scope.launch {
            val forward = settings.current().skipForwardSeconds * 1000L
            val c = connect()
            val target = c.currentPosition + forward
            c.seekTo(if (c.duration != C.TIME_UNSET) target.coerceAtMost(c.duration) else target)
        }
    }

    /** Vitesse de l'épisode en cours et vitesse globale par défaut. */
    fun setSpeed(speed: Float) {
        scope.launch { settings.setSpeed(speed) }
        command { it.setPlaybackSpeed(speed) }
    }

    fun next() {
        scope.launch {
            val current = state.value.episodeId ?: return@launch
            episodes.nextInQueue(current)?.let { play(it) }
        }
    }

    fun previous() {
        scope.launch {
            val c = connect()
            if (c.currentPosition > RESTART_THRESHOLD_MS) {
                c.seekTo(0)
                return@launch
            }
            val current = state.value.episodeId ?: return@launch
            episodes.previousInQueue(current)?.let { play(it) }
        }
    }

    fun setSleepTimer(minutes: Int) = sleepTimer.set(minutes)

    fun setSleepAtEndOfEpisode() = sleepTimer.setEndOfEpisode()

    fun cancelSleepTimer() = sleepTimer.cancel()

    private fun command(block: (MediaController) -> Unit) {
        scope.launch { block(connect()) }
    }

    private suspend fun connect(): MediaController = mutex.withLock {
        controller?.takeIf { it.isConnected } ?: run {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, token).buildAsync().await().also { c ->
                controller = c
                c.addListener(listener)
                publish(c)
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updateTicker(player)
        }
    }

    private fun publish(player: Player) {
        val id = episodeIdOf(player.currentMediaItem)
        val metadata: MediaMetadata = player.mediaMetadata
        _state.update { current ->
            if (id == null) {
                current.copy(isPlaying = false, isBuffering = false)
            } else {
                current.copy(
                    episodeId = id,
                    title = metadata.title?.toString() ?: current.title,
                    podcastTitle = metadata.artist?.toString() ?: current.podcastTitle,
                    artworkUrl = metadata.artworkUri?.toString() ?: current.artworkUrl,
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    positionMs = player.currentPosition,
                    durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: current.durationMs,
                    speed = player.playbackParameters.speed
                )
            }
        }
    }

    /** Rafraîchit la position à l'écran pendant la lecture. */
    private fun updateTicker(player: Player) {
        ticker?.cancel()
        if (!player.isPlaying) return
        ticker = scope.launch {
            while (isActive) {
                _state.update { it.copy(positionMs = player.currentPosition) }
                delay(TICK_MS)
            }
        }
    }

    /** Au lancement, le mini-lecteur montre le dernier épisode commencé, en pause. */
    private suspend fun restoreLastEpisode() {
        if (_state.value.hasEpisode) return
        val last = episodes.observeResume().first() ?: return
        val (episode, feed) = last
        _state.update {
            if (it.hasEpisode) {
                it
            } else {
                it.copy(
                    episodeId = episode.id,
                    title = episode.title,
                    podcastTitle = feed.title,
                    artworkUrl = episode.imageUrl ?: feed.imageUrl,
                    positionMs = episode.positionMs,
                    durationMs = episode.durationMs ?: 0,
                    speed = feed.playbackSpeed ?: settings.current().speed
                )
            }
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val RESTART_THRESHOLD_MS = 5_000L
    }
}
