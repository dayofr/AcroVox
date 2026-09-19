package com.acrovox.core.download

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Erreur qu'un nouvel essai ne corrigera pas (fichier introuvable, espace insuffisant…). */
class PermanentDownloadException(message: String) : IOException(message)

/**
 * Téléchargement HTTP d'un fichier audio, avec reprise : les octets déjà présents dans le fichier
 * partiel ne sont pas retéléchargés si le serveur accepte les requêtes `Range`.
 */
class EpisodeDownloader @Inject constructor(client: OkHttpClient) {
    private val http = client.newBuilder()
        .cache(null)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Télécharge [url] dans [part].
     *
     * @param freeBytes espace disponible, vérifié avant d'écrire.
     * @param onProgress appelé au plus toutes les [progressIntervalMs] avec les octets reçus et la taille totale.
     * @return taille finale du fichier.
     */
    suspend fun download(
        url: String,
        part: File,
        freeBytes: () -> Long,
        progressIntervalMs: Long = PROGRESS_INTERVAL_MS,
        onProgress: suspend (bytes: Long, total: Long?) -> Unit
    ): Long {
        val existing = if (part.exists()) part.length() else 0L
        val request = Request.Builder().url(url)
            .apply { if (existing > 0) header("Range", "bytes=$existing-") }
            .build()
        val call = http.newCall(request)
        val job = coroutineContext[Job]
        val cancelHandle = job?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (response.code == HTTP_RANGE_NOT_SATISFIABLE && existing > 0) {
                    // Fichier partiel invalide (fichier changé sur le serveur) : on recommence.
                    part.delete()
                    return download(url, part, freeBytes, progressIntervalMs, onProgress)
                }
                checkResponse(response)
                val append = response.code == HTTP_PARTIAL && existing > 0
                val start = if (append) existing else 0L
                val body = response.body
                val length = body.contentLength().takeIf { it >= 0 }
                val total = totalSize(response, start, length)
                if (length != null && length > freeBytes()) {
                    throw PermanentDownloadException("Espace de stockage insuffisant")
                }
                var bytes = start
                var lastReport = 0L
                FileOutputStream(part, append).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            bytes += read
                            val now = System.nanoTime() / 1_000_000
                            if (now - lastReport >= progressIntervalMs) {
                                lastReport = now
                                onProgress(bytes, total)
                            }
                        }
                    }
                }
                if (total != null && bytes < total) throw IOException("Téléchargement incomplet")
                onProgress(bytes, total ?: bytes)
                return bytes
            }
        } finally {
            cancelHandle?.dispose()
        }
    }

    private fun checkResponse(response: Response) {
        if (!response.isSuccessful) {
            val code = response.code
            val retryable = code == HTTP_TIMEOUT || code == HTTP_TOO_MANY || code >= HTTP_SERVER_ERROR
            val message = "Erreur du serveur ($code)"
            throw if (retryable) IOException(message) else PermanentDownloadException(message)
        }
        // Portail captif ou lien vers une page : ce n'est pas un fichier audio.
        val type = response.header("Content-Type")?.lowercase()
        if (type != null && type.startsWith("text/html")) {
            throw IOException("Le serveur a renvoyé une page web au lieu du fichier")
        }
    }

    /** Taille complète : Content-Range si la réponse est partielle, sinon Content-Length. */
    private fun totalSize(response: Response, start: Long, length: Long?): Long? =
        response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
            ?: length?.let { it + start }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
        const val BUFFER_SIZE = 64 * 1024
        const val HTTP_PARTIAL = 206
        const val HTTP_TIMEOUT = 408
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
        const val HTTP_TOO_MANY = 429
        const val HTTP_SERVER_ERROR = 500
    }
}
