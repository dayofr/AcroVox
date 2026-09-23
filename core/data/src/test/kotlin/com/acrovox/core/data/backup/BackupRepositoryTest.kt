package com.acrovox.core.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.acrovox.core.database.AcroVoxDatabase
import com.acrovox.core.database.entity.FeedEntity
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Export zip + vérification d'import, sur une vraie base Room. */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AcroVoxDatabase
    private lateinit var repository: BackupRepository
    private lateinit var workDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(BackupRepository.DB_NAME).delete()
        workDir = File(context.cacheDir, "backup-test").apply { mkdirs() }
        db = Room.databaseBuilder(context, AcroVoxDatabase::class.java, BackupRepository.DB_NAME)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
        workDir.deleteRecursively()
        File(context.cacheDir, "backup-staging").deleteRecursively()
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun export_createsZipWithDatabase() = runTest {
        db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))

        val zip = File(workDir, "backup.zip")
        repository.export(Uri.fromFile(zip))

        val entries = ZipFile(zip).use { file -> file.entries().asSequence().map { it.name }.toSet() }
        assertThat(entries).contains("acrovox.db")
    }

    @Test
    fun export_includesDatastoreFiles() = runTest {
        val datastore = File(context.filesDir, "datastore").apply { mkdirs() }
        File(datastore, "theme.preferences_pb").writeBytes(byteArrayOf(1, 2, 3))

        val zip = File(workDir, "backup.zip")
        repository.export(Uri.fromFile(zip))

        val entries = ZipFile(zip).use { file -> file.entries().asSequence().map { it.name }.toSet() }
        assertThat(entries).contains("datastore/theme.preferences_pb")
    }

    @Test
    fun stageImport_validatesCounts() = runTest {
        db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        val zip = File(workDir, "backup.zip")
        repository.export(Uri.fromFile(zip))

        val staged = repository.stageImport(Uri.fromFile(zip))

        assertThat(staged.info.feedCount).isEqualTo(1)
        assertThat(staged.info.episodeCount).isEqualTo(0)
        assertThat(staged.dbFile.isFile).isTrue()
        repository.discard(staged)
        assertThat(File(context.cacheDir, "backup-staging").exists()).isFalse()
    }

    @Test
    fun stageImport_rejectsNonZip() = runTest {
        val garbage = File(workDir, "garbage.zip").apply { writeText("pas un zip") }

        val error = catchBackup { repository.stageImport(Uri.fromFile(garbage)) }

        assertThat(error).isNotNull()
    }

    @Test
    fun stageImport_rejectsZipWithoutDatabase() = runTest {
        val zip = File(workDir, "empty.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("datastore/theme.preferences_pb"))
            out.write(byteArrayOf(1))
            out.closeEntry()
        }

        val error = catchBackup { repository.stageImport(Uri.fromFile(zip)) }

        assertThat(error).isNotNull()
    }

    @Test
    fun stageImport_rejectsWrongVersion() = runTest {
        db.feedDao().insert(FeedEntity(feedUrl = "https://example.org/f", title = "P", subscribedAt = 0))
        val zip = File(workDir, "backup.zip")
        repository.export(Uri.fromFile(zip))
        val tampered = File(workDir, "tampered.zip")
        ZipFile(zip).use { file ->
            val entry = file.getEntry("acrovox.db")!!
            val raw = File(workDir, "raw.db")
            file.getInputStream(entry).use { input -> raw.outputStream().use { input.copyTo(it) } }
            SQLiteDatabase.openDatabase(raw.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { sqlite ->
                sqlite.execSQL("PRAGMA user_version = 99")
            }
            ZipOutputStream(tampered.outputStream()).use { out ->
                out.putNextEntry(ZipEntry("acrovox.db"))
                raw.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }

        val error = catchBackup { repository.stageImport(Uri.fromFile(tampered)) }

        assertThat(error).isNotNull()
    }

    private suspend fun catchBackup(block: suspend () -> Unit): BackupException? = try {
        block()
        null
    } catch (e: BackupException) {
        e
    }
}
