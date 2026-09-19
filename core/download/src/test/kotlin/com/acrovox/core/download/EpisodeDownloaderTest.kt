package com.acrovox.core.download

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EpisodeDownloaderTest {
    private val server = MockWebServer()
    private val downloader = EpisodeDownloader(OkHttpClient())
    private lateinit var part: File
    private val content = ByteArray(200_000) { (it % 251).toByte() }

    @Before
    fun setUp() {
        server.start()
        part = Files.createTempDirectory("dl").resolve("1.part").toFile()
    }

    @After fun tearDown() = server.close()

    private fun url() = server.url("/e.mp3").toString()

    private fun audio(code: Int = 200, body: ByteArray = content, headers: Map<String, String> = emptyMap()) =
        MockResponse.Builder().code(code).body(okio.Buffer().write(body)).addHeader("Content-Type", "audio/mpeg")
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()

    @Test
    fun downloadsWholeFile() = runTest {
        server.enqueue(audio())
        var last: Pair<Long, Long?>? = null

        val size = downloader.download(url(), part, { Long.MAX_VALUE }) { b, t -> last = b to t }

        assertThat(size).isEqualTo(content.size.toLong())
        assertThat(part.readBytes()).isEqualTo(content)
        assertThat(last).isEqualTo(content.size.toLong() to content.size.toLong())
    }

    @Test
    fun resumesWithRange() = runTest {
        part.writeBytes(content.copyOfRange(0, 50_000))
        server.enqueue(
            audio(
                code = 206,
                body = content.copyOfRange(50_000, content.size),
                headers = mapOf("Content-Range" to "bytes 50000-199999/200000")
            )
        )

        downloader.download(url(), part, { Long.MAX_VALUE }) { _, _ -> }

        assertThat(server.takeRequest().headers["Range"]).isEqualTo("bytes=50000-")
        assertThat(part.readBytes()).isEqualTo(content)
    }

    @Test
    fun restartsWhenServerIgnoresRange() = runTest {
        part.writeBytes(ByteArray(1_000) { 7 })
        server.enqueue(audio())

        downloader.download(url(), part, { Long.MAX_VALUE }) { _, _ -> }

        assertThat(part.readBytes()).isEqualTo(content)
    }

    @Test
    fun htmlPageIsRejected() = runTest {
        server.enqueue(
            MockResponse.Builder().body("<html>Connexion Wi-Fi</html>").addHeader("Content-Type", "text/html").build()
        )

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { downloader.download(url(), part, { Long.MAX_VALUE }) { _, _ -> } }
        }
    }

    @Test
    fun notFoundIsPermanent() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())

        assertThrows(PermanentDownloadException::class.java) {
            kotlinx.coroutines.runBlocking { downloader.download(url(), part, { Long.MAX_VALUE }) { _, _ -> } }
        }
    }

    @Test
    fun notEnoughSpaceIsPermanent() = runTest {
        server.enqueue(audio())

        assertThrows(PermanentDownloadException::class.java) {
            kotlinx.coroutines.runBlocking { downloader.download(url(), part, { 1_000L }) { _, _ -> } }
        }
    }

    @Test
    fun extensionFromUrlOrMimeType() {
        assertThat(extensionOf("https://a.org/ep.m4a?x=1", null)).isEqualTo("m4a")
        assertThat(extensionOf("https://a.org/stream/123", "audio/x-m4a")).isEqualTo("m4a")
        assertThat(extensionOf("https://a.org/stream/123", null)).isEqualTo("mp3")
    }
}
