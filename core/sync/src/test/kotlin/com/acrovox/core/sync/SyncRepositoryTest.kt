package com.acrovox.core.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.data.repository.EpisodeRepository
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.EpisodeEntity
import com.acrovox.core.database.entity.FeedEntity
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncRepositoryTest {
    /** Chiffrement factice : le Keystore n'existe pas sous Robolectric. */
    private object PlainCipher : SecretCipher {
        override fun encrypt(plain: String) = "x$plain"

        override fun decrypt(encoded: String) = encoded.removePrefix("x")
    }

    private val server = MockWebServer()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val clock = Clock.fixed(Instant.parse("2026-09-19T14:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AcroVoxDatabase
    private lateinit var accounts: SyncAccountRepository
    private lateinit var sync: SyncRepository
    private lateinit var episodes: EpisodeRepository
    private var feedId = 0L

    @Before
    fun setUp() = runTest {
        server.start()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcroVoxDatabase::class.java).allowMainThreadQueries().build()
        accounts = SyncAccountRepository(context, PlainCipher)
        accounts.clear()
        sync = SyncRepository(db, GpodderClient(OkHttpClient(), json, Dispatchers.IO), accounts, clock)
        episodes = EpisodeRepository(db, clock)
        feedId = db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/feed", title = "P", subscribedAt = 0))
    }

    @After
    fun tearDown() {
        db.close()
        server.close()
    }

    private fun ok() = MockResponse.Builder().body("{}").addHeader("Content-Type", "application/json").build()

    private fun RecordedRequest.bodyJson() = json.parseToJsonElement(body!!.utf8())

    @Test
    fun notConnected_sendsNothing() = runTest {
        assertThat(sync.sync()).isEqualTo(SyncResult.NotConnected)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun inboxTriage_sendsOneDeletePerIgnoredEpisode() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        accounts.setSentSubscriptions(setOf("https://example.org/feed"))
        val ids = db.episodeDao().mergeFromFeed(
            feedId,
            (1..3).map {
                EpisodeEntity(
                    feedId = feedId,
                    guid = "g$it",
                    title = "e$it",
                    pubDate = 0,
                    mediaUrl = "https://e/$it.mp3"
                )
            }
        )
        episodes.ignore(ids)
        server.enqueue(ok())

        val result = sync.sync()

        assertThat(result).isEqualTo(SyncResult.Success(0, 0, 3))
        val request = server.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/api/2/episodes/alice.json")
        assertThat(request.headers["Authorization"]).startsWith("Basic ")
        val actions = request.bodyJson().jsonArray.map { it.jsonObject }
        assertThat(actions.map { it["action"]!!.jsonPrimitive.content }).containsExactly("delete", "delete", "delete")
        assertThat(actions.first()["device"]!!.jsonPrimitive.content).isEqualTo("acrovox-test")
        assertThat(actions.first()["timestamp"]!!.jsonPrimitive.content).isEqualTo("2026-09-19T14:00:00")
        assertThat(actions.first().containsKey("position")).isFalse()
        assertThat(db.episodeActionDao().count()).isEqualTo(0)
    }

    @Test
    fun subscriptions_sendsOnlyDifference() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        server.enqueue(ok())
        sync.sync()
        val first = server.takeRequest().bodyJson().jsonObject
        assertThat(
            first["add"]!!.jsonArray.map {
                it.jsonPrimitive.content
            }
        ).containsExactly("https://example.org/feed")
        assertThat(first["remove"]!!.jsonArray).isEmpty()

        // Rien n'a changé : aucune requête.
        sync.sync()
        assertThat(server.requestCount).isEqualTo(1)

        db.feedDao().delete(feedId)
        db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/other", title = "O", subscribedAt = 0))
        server.enqueue(ok())
        sync.sync()
        val second = server.takeRequest().bodyJson().jsonObject
        assertThat(
            second["add"]!!.jsonArray.map {
                it.jsonPrimitive.content
            }
        ).containsExactly("https://example.org/other")
        assertThat(
            second["remove"]!!.jsonArray.map {
                it.jsonPrimitive.content
            }
        ).containsExactly("https://example.org/feed")
    }

    @Test
    fun failure_keepsActionsForNextTime() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        accounts.setSentSubscriptions(setOf("https://example.org/feed"))
        val id = db.episodeDao().mergeFromFeed(
            feedId,
            listOf(EpisodeEntity(feedId = feedId, guid = "g", title = "e", pubDate = 0, mediaUrl = "https://e/1.mp3"))
        )
        episodes.recordListening(id.single(), 0, 60_000, 600_000)
        server.enqueue(MockResponse.Builder().code(503).build())

        val result = sync.sync()

        assertThat(result).isInstanceOf(SyncResult.Failed::class.java)
        assertThat((result as SyncResult.Failed).retryable).isTrue()
        assertThat(db.episodeActionDao().count()).isEqualTo(1)

        server.enqueue(ok())
        sync.sync()
        server.takeRequest()
        val play = server.takeRequest().bodyJson().jsonArray.single().jsonObject
        assertThat(play["action"]!!.jsonPrimitive.content).isEqualTo("play")
        assertThat(play["started"]!!.jsonPrimitive.content).isEqualTo("0")
        assertThat(play["position"]!!.jsonPrimitive.content).isEqualTo("60")
        assertThat(play["total"]!!.jsonPrimitive.content).isEqualTo("600")
        assertThat(db.episodeActionDao().count()).isEqualTo(0)
    }

    @Test
    fun wrongPassword_isNotRetried() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        server.enqueue(MockResponse.Builder().code(401).build())

        val result = sync.sync() as SyncResult.Failed

        assertThat(result.retryable).isFalse()
        assertThat(result.message).isEqualTo("Identifiants refusés")
    }

    @Test
    fun htmlResponse_isRejected() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        server.enqueue(MockResponse.Builder().code(404).body("<html>").addHeader("Content-Type", "text/html").build())

        val result = sync.sync() as SyncResult.Failed

        assertThat(result.retryable).isFalse()
        assertThat(result.message).contains("gPodder")
    }

    @Test
    fun largeQueue_isSentInBatches() = runTest {
        accounts.save(GpodderCredentials(server.url("/"), "alice", "secret"), "acrovox-test")
        accounts.setSentSubscriptions(setOf("https://example.org/feed"))
        val id = db.episodeDao().mergeFromFeed(
            feedId,
            listOf(EpisodeEntity(feedId = feedId, guid = "g", title = "e", pubDate = 0, mediaUrl = "https://e/1.mp3"))
        ).single()
        repeat(250) { episodes.recordListening(id, it * 1000L, it * 1000L + 500, 600_000) }
        repeat(3) { server.enqueue(ok()) }

        val result = sync.sync()

        assertThat(result).isEqualTo(SyncResult.Success(0, 0, 250))
        assertThat((1..3).map { (server.takeRequest().bodyJson() as JsonArray).size }).containsExactly(100, 100, 50)
    }

    @Test
    fun connect_logsInAndRegistersDevice() = runTest {
        repeat(2) { server.enqueue(ok()) }
        // parseServer force https ; le test passe directement par le client avec l'adresse locale.
        val client = GpodderClient(OkHttpClient(), json, Dispatchers.IO)
        val credentials = GpodderCredentials(server.url("/"), "alice", "secret")
        client.login(credentials)
        client.registerDevice(credentials, "acrovox-abc", "AcroVox (Pixel)")

        assertThat(server.takeRequest().url.encodedPath).isEqualTo("/api/2/auth/alice/login.json")
        val device = server.takeRequest()
        assertThat(device.url.encodedPath).isEqualTo("/api/2/devices/alice/acrovox-abc.json")
        val body = device.bodyJson() as JsonObject
        assertThat(body["caption"]!!.jsonPrimitive.content).isEqualTo("AcroVox (Pixel)")
        assertThat(body["type"]!!.jsonPrimitive.content).isEqualTo("mobile")
    }

    @Test
    fun serverAddress_alwaysHttps() {
        assertThat(GpodderClient.parseServer("http://api.theque.dayo.ovh")!!.toString())
            .isEqualTo("https://api.theque.dayo.ovh/")
        assertThat(GpodderClient.parseServer(" gpodder.net/ ")!!.toString()).isEqualTo("https://gpodder.net/")
        assertThat(GpodderClient.parseServer("   ")).isNull()
    }
}
