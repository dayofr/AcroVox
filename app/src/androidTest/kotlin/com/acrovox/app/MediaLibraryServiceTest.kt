package com.acrovox.app

import android.content.ComponentName
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.acrovox.core.player.MediaLibraryTree
import com.acrovox.core.player.PlaybackService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parcourt la bibliothèque comme Android Auto : un MediaBrowser se connecte au vrai service.
 * Tourne sur les données de l'appareil ; il faut au moins un abonnement.
 */
@RunWith(AndroidJUnit4::class)
class MediaLibraryServiceTest {
    private lateinit var browser: MediaBrowser

    @Before
    fun connect() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        browser = withContext(Dispatchers.Main) {
            MediaBrowser.Builder(context, token).setApplicationLooper(Looper.getMainLooper()).buildAsync().await()
        }
    }

    @After
    fun release() = runBlocking { withContext(Dispatchers.Main) { browser.release() } }

    private suspend fun <T> main(block: suspend MediaBrowser.() -> T): T = withContext(Dispatchers.Main) {
        browser.block()
    }

    @Test
    fun browseRootFoldersAndSubscriptions() = runBlocking {
        val root = main { getLibraryRoot(null).await().value!! }
        assertThat(root.mediaId).isEqualTo(MediaLibraryTree.ROOT)

        val folders = main { getChildren(root.mediaId, 0, 20, null).await().value!! }
        assertThat(folders.map { it.mediaMetadata.title.toString() })
            .containsExactly("File d'attente", "Boîte de réception", "Téléchargements", "Abonnements").inOrder()

        val feeds = main { getChildren(MediaLibraryTree.FEEDS, 0, 50, null).await().value!! }
        assertThat(feeds).isNotEmpty()
        val episodes = main { getChildren(feeds.first().mediaId, 0, 10, null).await().value!! }
        assertThat(episodes).isNotEmpty()
        assertThat(episodes.first().mediaMetadata.isPlayable).isTrue()
        assertThat(episodes.first().mediaMetadata.mediaType).isEqualTo(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
    }

    @Test
    fun searchByPodcastTitle() = runBlocking {
        val feed = main { getChildren(MediaLibraryTree.FEEDS, 0, 50, null).await().value!! }.first()
        val word = feed.mediaMetadata.title.toString().split(" ", ":").first { it.length >= 3 }

        main { search(word, null).await() }
        val results = main { getSearchResult(word, 0, 10, null).await().value!! }

        assertThat(results).isNotEmpty()
    }

    @Test
    fun voiceRequestStartsAnEpisode() = runBlocking {
        val feed = main { getChildren(MediaLibraryTree.FEEDS, 0, 50, null).await().value!! }.first()
        val query = feed.mediaMetadata.title.toString()
        val request = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setSearchQuery(query).build())
            .build()

        main {
            setMediaItem(request)
            prepare()
        }
        var current: MediaItem? = null
        repeat(50) {
            current = main { currentMediaItem }
            if (current?.localConfiguration != null || current?.mediaId?.isNotEmpty() == true) return@repeat
            delay(100)
        }
        main { pause() }

        assertThat(current!!.mediaId.toLongOrNull()).isNotNull()
        assertThat(current!!.mediaMetadata.artist.toString()).isEqualTo(query)
    }

    /**
     * Android Auto et le volant : les commandes suivant/précédent sont annoncées
     * même si la timeline ne contient qu'un épisode (file gérée à la main).
     */
    @Test
    fun nextPreviousCommandsAreAdvertised() = runBlocking {
        val commands = main { availableCommands }
        assertThat(commands.contains(Player.COMMAND_SEEK_TO_NEXT)).isTrue()
        assertThat(commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS)).isTrue()
    }
}
