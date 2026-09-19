package com.acrovox.core.sync

import com.acrovox.core.database.entity.EpisodeActionEntity
import com.acrovox.core.model.EpisodeActionType
import com.acrovox.core.network.di.IoDispatcher
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Identifiants d'un compte gPodder. */
data class GpodderCredentials(val server: HttpUrl, val username: String, val password: String)

/** Identifiants refusés par le serveur. */
class GpodderAuthException(message: String) : IOException(message)

/** Erreur que l'envoi suivant ne corrigera pas seul (requête refusée, réponse inattendue). */
class GpodderProtocolException(message: String) : IOException(message)

@Serializable
internal data class SubscriptionChanges(val add: List<String>, val remove: List<String>)

@Serializable
internal data class DeviceInfo(val caption: String, val type: String)

@Serializable
internal data class EpisodeActionJson(
    val podcast: String,
    val episode: String,
    val guid: String? = null,
    val device: String,
    val action: String,
    val timestamp: String,
    val started: Int? = null,
    val position: Int? = null,
    val total: Int? = null
)

/**
 * Client de l'API gPodder v2, en envoi seulement : l'app ne lit rien du serveur.
 * Authentification Basic sur chaque requête.
 */
class GpodderClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json,
    @param:IoDispatcher private val io: CoroutineDispatcher
) {
    /** Vérifie les identifiants. */
    suspend fun login(credentials: GpodderCredentials) {
        post(credentials, "api/2/auth/${credentials.username}/login.json", "")
    }

    /** Crée l'appareil, ou met à jour son nom. */
    suspend fun registerDevice(credentials: GpodderCredentials, deviceId: String, caption: String) {
        post(
            credentials,
            "api/2/devices/${credentials.username}/$deviceId.json",
            json.encodeToString(DeviceInfo(caption, type = "mobile"))
        )
    }

    suspend fun uploadSubscriptions(
        credentials: GpodderCredentials,
        deviceId: String,
        add: List<String>,
        remove: List<String>
    ) {
        post(
            credentials,
            "api/2/subscriptions/${credentials.username}/$deviceId.json",
            json.encodeToString(SubscriptionChanges(add, remove))
        )
    }

    suspend fun uploadEpisodeActions(
        credentials: GpodderCredentials,
        deviceId: String,
        actions: List<EpisodeActionEntity>
    ) {
        post(
            credentials,
            "api/2/episodes/${credentials.username}.json",
            json.encodeToString(actions.map { it.toJson(deviceId) })
        )
    }

    private suspend fun post(credentials: GpodderCredentials, path: String, body: String) = withContext(io) {
        val url = credentials.server.newBuilder().addPathSegments(path).build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(credentials.username, credentials.password))
            .post(body.toRequestBody(JSON))
            .build()
        http.newCall(request).execute().use { response ->
            when {
                response.code == HTTP_UNAUTHORIZED -> throw GpodderAuthException("Identifiants refusés")
                response.code >= HTTP_SERVER_ERROR -> throw IOException("Erreur du serveur (${response.code})")
                !response.isSuccessful -> throw GpodderProtocolException(
                    if (response.code == HTTP_NOT_FOUND) {
                        "Ce serveur ne répond pas comme un serveur gPodder"
                    } else {
                        "Requête refusée (${response.code})"
                    }
                )
                response.header("Content-Type")?.contains("json") != true ->
                    throw GpodderProtocolException("Réponse inattendue du serveur")
            }
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_SERVER_ERROR = 500

        private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)

        /**
         * Adresse du serveur saisie par l'utilisateur. Toujours en https : une redirection
         * http → https transformerait les POST en GET.
         */
        fun parseServer(input: String): HttpUrl? {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring("http://".length)
                else -> "https://$trimmed"
            }
            return "$withScheme/".toHttpUrlOrNull()
        }

        internal fun EpisodeActionEntity.toJson(deviceId: String) = EpisodeActionJson(
            podcast = podcastUrl.trim(),
            episode = episodeUrl.trim(),
            guid = guid,
            device = deviceId,
            action = action.wireName,
            timestamp = TIMESTAMP.format(Instant.ofEpochMilli(timestamp)),
            started = started,
            position = position,
            total = total
        )

        private val EpisodeActionType.wireName: String
            get() = when (this) {
                EpisodeActionType.PLAY -> "play"
                EpisodeActionType.DOWNLOAD -> "download"
                EpisodeActionType.DELETE -> "delete"
                EpisodeActionType.NEW -> "new"
            }
    }
}
