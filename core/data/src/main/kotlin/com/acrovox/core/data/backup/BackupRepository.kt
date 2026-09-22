package com.acrovox.core.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.acrovox.core.database.AcroVoxDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Erreur de sauvegarde/restauration, message affichable tel quel. */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Contenu d'une sauvegarde prête à être restaurée. */
data class BackupInfo(val feedCount: Int, val episodeCount: Int)

/** Fichiers extraits d'une sauvegarde, en attente de confirmation. */
data class StagedBackup(val info: BackupInfo, val dbFile: File, val datastoreFiles: List<File>)

/**
 * Export/import de la base complète (Room + réglages DataStore) dans un zip via SAF.
 *
 * L'import remplace les fichiers puis redémarre l'app : l'instance Room portée
 * par Hilt ne peut pas être rouverte sur un autre fichier dans le même processus.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AcroVoxDatabase
) {
    companion object {
        const val DB_NAME = "acrovox.db"
        const val DB_VERSION = 1
        private const val DB_ENTRY = "acrovox.db"
        private const val DATASTORE_PREFIX = "datastore/"
        private val RequiredTables = setOf(
            "feed",
            "episode",
            "queue_item",
            "download",
            "chapter",
            "episode_action",
            "playback_history"
        )
    }

    private val databaseFile: File get() = context.getDatabasePath(DB_NAME)
    private val datastoreDir: File get() = File(context.filesDir, "datastore")

    /** Exporte la base + les réglages vers [uri] (sélecteur de fichier). */
    suspend fun export(uri: Uri) {
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            val out = context.contentResolver.openOutputStream(uri)
                ?: throw BackupException("Impossible d'écrire dans ce fichier.")
            out.use { stream ->
                ZipOutputStream(stream).use { zip ->
                    addEntry(zip, DB_ENTRY, databaseFile)
                    datastoreFiles().forEach { addEntry(zip, DATASTORE_PREFIX + it.name, it) }
                }
            }
        }
    }

    /**
     * Copie la sauvegarde [uri] en cache et la vérifie.
     * Ne touche pas aux données en place.
     */
    suspend fun stageImport(uri: Uri): StagedBackup = withContext(Dispatchers.IO) {
        val staged = File(context.cacheDir, "backup-staging")
        staged.deleteRecursively()
        staged.mkdirs()
        val zipFile = File(staged, "backup.zip")
        (context.contentResolver.openInputStream(uri) ?: throw BackupException("Fichier illisible."))
            .use { input -> zipFile.outputStream().use { input.copyTo(it) } }
        try {
            ZipFile(zipFile).use { zip ->
                val dbEntry = zip.getEntry(DB_ENTRY)
                    ?: throw BackupException("Archive invalide : base manquante.")
                val dbFile = File(staged, DB_NAME)
                zip.getInputStream(dbEntry).use { input -> dbFile.outputStream().use { input.copyTo(it) } }
                val datastoreFiles = zip.entries().asSequence()
                    .filter { it.name.startsWith(DATASTORE_PREFIX) && !it.isDirectory }
                    .map { entry ->
                        val name = entry.name.removePrefix(DATASTORE_PREFIX)
                        require(name.isNotBlank() && "/" !in name && name != "." && name != "..") {
                            "Archive invalide : entrée inattendue."
                        }
                        val target = File(staged, "datastore-$name")
                        zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                        target
                    }
                    .toList()
                return@withContext StagedBackup(checkDatabase(dbFile), dbFile, datastoreFiles)
            }
        } catch (e: BackupException) {
            staged.deleteRecursively()
            throw e
        } catch (e: Exception) {
            staged.deleteRecursively()
            throw BackupException("Archive illisible.", e)
        }
    }

    /** Remplace les données par [staged] puis redémarre l'app. */
    suspend fun import(staged: StagedBackup) {
        withContext(Dispatchers.IO) {
            db.close()
            val target = databaseFile
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            File(target.path + "-journal").delete()
            staged.dbFile.copyTo(target, overwrite = true)
            staged.datastoreFiles.forEach { file ->
                val name = file.name.removePrefix("datastore-")
                file.copyTo(File(datastoreDir, name), overwrite = true)
            }
            staged.dbFile.parentFile?.deleteRecursively()
            restart()
        }
    }

    /** Abandonne une sauvegarde mise en attente (nettoie le cache). */
    suspend fun discard(staged: StagedBackup) {
        withContext(Dispatchers.IO) {
            staged.dbFile.parentFile?.deleteRecursively()
        }
    }

    private fun datastoreFiles(): List<File> =
        datastoreDir.listFiles { file -> file.isFile && file.name.endsWith(".preferences_pb") }
            ?.toList() ?: emptyList()

    private fun addEntry(zip: ZipOutputStream, name: String, file: File) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun checkDatabase(file: File): BackupInfo {
        if (!file.isFile) throw BackupException("Archive invalide : base manquante.")
        try {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { sqlite ->
                val version = sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                    cursor.moveToFirst()
                    cursor.getInt(0)
                }
                if (version != DB_VERSION) {
                    throw BackupException("Version de sauvegarde incompatible (v$version, attendue v$DB_VERSION).")
                }
                val tables = sqlite.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table'",
                    null
                ).use { cursor ->
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
                }
                if (!tables.containsAll(RequiredTables)) {
                    throw BackupException("Archive invalide : tables manquantes.")
                }
                val feeds = count(sqlite, "feed")
                val episodes = count(sqlite, "episode")
                return BackupInfo(feeds, episodes)
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: SQLiteException) {
            throw BackupException("Ce fichier n'est pas une base AcroVox.", e)
        }
    }

    private fun count(sqlite: SQLiteDatabase, table: String): Int =
        sqlite.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun restart() {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
        exitProcess(0)
    }
}
