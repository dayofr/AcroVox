package com.acrovox.core.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Compte connecté, sans le mot de passe. */
data class SyncAccount(
    val server: String,
    val username: String,
    val deviceId: String,
    val lastSyncAt: Long? = null,
    val lastError: String? = null
)

private val Context.syncStore: DataStore<Preferences> by preferencesDataStore("sync")

/** Compte gPodder et état de l'envoi. Le mot de passe est chiffré par [SecretCipher]. */
@Singleton
class SyncAccountRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cipher: SecretCipher
) {
    private object Keys {
        val server = stringPreferencesKey("server")
        val username = stringPreferencesKey("username")
        val password = stringPreferencesKey("password")
        val deviceId = stringPreferencesKey("device_id")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val lastError = stringPreferencesKey("last_error")
        val sentSubscriptions = stringSetPreferencesKey("sent_subscriptions")
    }

    val account: Flow<SyncAccount?> = context.syncStore.data.map { p ->
        val server = p[Keys.server] ?: return@map null
        SyncAccount(
            server = server,
            username = p[Keys.username].orEmpty(),
            deviceId = p[Keys.deviceId].orEmpty(),
            lastSyncAt = p[Keys.lastSyncAt],
            lastError = p[Keys.lastError]
        )
    }

    /** Identifiants complets, ou null si pas de compte ou mot de passe illisible. */
    suspend fun credentials(): Pair<GpodderCredentials, String>? {
        val p = context.syncStore.data.first()
        val server = p[Keys.server]?.toHttpUrlOrNull() ?: return null
        val password = p[Keys.password]?.let(cipher::decrypt) ?: return null
        val deviceId = p[Keys.deviceId] ?: return null
        return GpodderCredentials(server, p[Keys.username].orEmpty(), password) to deviceId
    }

    suspend fun save(credentials: GpodderCredentials, deviceId: String) {
        context.syncStore.edit {
            it.clear()
            it[Keys.server] = credentials.server.toString()
            it[Keys.username] = credentials.username
            it[Keys.password] = cipher.encrypt(credentials.password)
            it[Keys.deviceId] = deviceId
        }
    }

    /** Oublie le compte et ce qui a été envoyé ; les actions en attente restent en base. */
    suspend fun clear() {
        context.syncStore.edit { it.clear() }
    }

    /** Abonnements tels que le serveur les connaît après le dernier envoi réussi. */
    suspend fun sentSubscriptions(): Set<String> = context.syncStore.data.first()[Keys.sentSubscriptions].orEmpty()

    suspend fun setSentSubscriptions(urls: Set<String>) {
        context.syncStore.edit { it[Keys.sentSubscriptions] = urls }
    }

    suspend fun recordSuccess(at: Long) {
        context.syncStore.edit {
            it[Keys.lastSyncAt] = at
            it.remove(Keys.lastError)
        }
    }

    suspend fun recordError(message: String) {
        context.syncStore.edit { it[Keys.lastError] = message }
    }
}
