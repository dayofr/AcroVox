package com.acrovox.core.download

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Emplacement des fichiers : dossier privé de l'app, supprimé avec elle. */
class DownloadFiles @Inject constructor(@param:ApplicationContext private val context: Context) {
    val directory: File
        get() = (context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: File(context.filesDir, "podcasts"))
            .apply { mkdirs() }

    /** Fichier en cours de téléchargement, gardé pour reprendre après une coupure. */
    fun partFile(episodeId: Long) = File(directory, "$episodeId.part")

    fun finalFile(episodeId: Long, mediaUrl: String, mediaType: String?) =
        File(directory, "$episodeId.${extensionOf(mediaUrl, mediaType)}")

    fun freeBytes(): Long = directory.usableSpace
}

private val MimeExtensions = mapOf(
    "audio/mpeg" to "mp3",
    "audio/mp3" to "mp3",
    "audio/mp4" to "m4a",
    "audio/x-m4a" to "m4a",
    "audio/aac" to "aac",
    "audio/ogg" to "ogg",
    "audio/opus" to "opus",
    "audio/wav" to "wav",
    "video/mp4" to "mp4"
)

internal fun extensionOf(mediaUrl: String, mediaType: String?): String {
    val fromUrl = mediaUrl.substringBefore('?').substringBefore('#').substringAfterLast('/')
        .substringAfterLast('.', "").lowercase()
    if (fromUrl.length in 2..4 && fromUrl.all { it.isLetterOrDigit() }) return fromUrl
    return MimeExtensions[mediaType?.lowercase()] ?: "mp3"
}
