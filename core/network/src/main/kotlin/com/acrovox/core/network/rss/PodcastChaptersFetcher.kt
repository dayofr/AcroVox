package com.acrovox.core.network.rss

import com.acrovox.core.network.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Récupère le JSON `podcast:chapters` d'un épisode.
 *
 * Format : `{"version": "1.0", "chapters": [{"startTime": 12.5, "title": "…",
 * "img": "…", "url": "…"}]}`, `startTime` en secondes.
 */
@Singleton
class PodcastChaptersFetcher @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Null si le JSON est illisible (l'épisode reste sans chapitres). */
    suspend fun fetch(url: String): List<ParsedChapter>? = withContext(io) {
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val parsed = json.decodeFromString<ChaptersFile>(body)
                parsed.chapters
                    .filter { it.title.isNotBlank() && it.startTime >= 0 }
                    .map {
                        ParsedChapter(
                            startMs = (it.startTime * 1_000).toLong(),
                            title = it.title,
                            url = it.url,
                            imageUrl = it.img
                        )
                    }
                    .sortedBy { it.startMs }
                    .takeIf { it.isNotEmpty() }
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: kotlinx.serialization.SerializationException) {
            null
        }
    }

    @Serializable
    private data class ChaptersFile(val chapters: List<ChapterJson> = emptyList())

    @Serializable
    private data class ChapterJson(
        val startTime: Double = -1.0,
        val title: String = "",
        val img: String? = null,
        val url: String? = null
    )
}
