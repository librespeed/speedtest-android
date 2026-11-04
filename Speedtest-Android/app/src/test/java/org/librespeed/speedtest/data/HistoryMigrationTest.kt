package org.librespeed.speedtest.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Users with old measurements must never lose their history on an app update. */
@RunWith(AndroidJUnit4::class)
//robolectric does not support SDK 36 yet
@Config(sdk = [35])
class HistoryMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun cleanSlate() {
        context.deleteDatabase("history.db")
    }

    private fun createLegacyDatabase(version: Int) {
        val path = context.getDatabasePath("history.db").apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        val extraColumns = buildString {
            if (version >= 2) append(", networkType TEXT, dlSamples TEXT, ulSamples TEXT")
            if (version >= 3) append(", duration INTEGER NOT NULL DEFAULT 0")
            if (version >= 4) append(", mode TEXT, loadedDown REAL NOT NULL DEFAULT -1, loadedUp REAL NOT NULL DEFAULT -1, networkDetail TEXT")
        }
        db.execSQL(
            "CREATE TABLE history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date INTEGER NOT NULL," +
                "server TEXT NOT NULL," +
                "ping REAL NOT NULL," +
                "jitter REAL NOT NULL," +
                "download REAL NOT NULL," +
                "upload REAL NOT NULL," +
                "loss REAL NOT NULL," +
                "ipInfo TEXT," +
                "ipVersion INTEGER NOT NULL DEFAULT 0," +
                "shareUrl TEXT" +
                extraColumns + ")"
        )
        db.insert(
            "history", null,
            ContentValues().apply {
                put("date", 1_700_000_000_000)
                put("server", "Prague, Czech Republic (CESNET)")
                put("ping", 8.5)
                put("jitter", 1.2)
                put("download", 512.25)
                put("upload", 256.5)
                put("loss", 0.0)
                put("ipInfo", "203.0.113.7 - Example ISP")
                put("ipVersion", 4)
                put("shareUrl", "https://librespeed.example/results/?id=old1")
                if (version >= 2) {
                    put("networkType", "Wi-Fi 6")
                    put("dlSamples", "[500.0,510.0]")
                    put("ulSamples", "[250.0,260.0]")
                }
                if (version >= 3) put("duration", 20_500)
                if (version >= 4) put("mode", "standard")
            }
        )
        db.version = version
        db.close()
    }

    private fun assertLegacyRowSurvives(fromVersion: Int) {
        createLegacyDatabase(fromVersion)
        val entries = HistoryDatabase(context).readAll()
        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals(1_700_000_000_000, entry.date)
        assertEquals("Prague, Czech Republic (CESNET)", entry.server)
        assertEquals(512.25, entry.download, 0.0)
        assertEquals(256.5, entry.upload, 0.0)
        assertEquals(8.5, entry.ping, 0.0)
        assertEquals(4, entry.ipVersion)
        assertEquals("https://librespeed.example/results/?id=old1", entry.shareUrl)
        //columns added later fall back to their defaults
        if (fromVersion < 4) assertEquals(null, entry.mode)
        assertEquals(-1.0, entry.loadedDown, 0.0)
        assertEquals(-1.0, entry.loadedUp, 0.0)
        assertEquals(false, entry.telemetrySent)
        if (fromVersion >= 2) {
            assertEquals("Wi-Fi 6", entry.networkType)
            assertEquals(listOf(500.0, 510.0), entry.downloadSamples)
        }
        if (fromVersion >= 3) assertEquals(20_500, entry.durationMs)
    }

    @Test
    fun `v1 history survives the upgrade to v4`() = assertLegacyRowSurvives(1)

    @Test
    fun `v2 history survives the upgrade to v4`() = assertLegacyRowSurvives(2)

    @Test
    fun `v3 history survives the upgrade to v4`() = assertLegacyRowSurvives(3)

    @Test
    fun `v4 history survives the upgrade to v5`() = assertLegacyRowSurvives(4)

    @Test
    fun `new entries round-trip through the upgraded database`() {
        createLegacyDatabase(1)
        val database = HistoryDatabase(context)
        val id = database.insert(
            HistoryEntry(
                date = 2, server = "s", ping = 1.0, jitter = 1.0, download = 2.0, upload = 3.0,
                loss = -1.0, ipInfo = null, ipVersion = 6, shareUrl = null,
                mode = "stability", loadedDown = 12.5, loadedUp = 30.0, networkDetail = "op · signal 3/4",
                telemetrySent = true
            )
        )
        val entry = database.read(id)!!
        assertEquals("stability", entry.mode)
        assertEquals(12.5, entry.loadedDown, 0.0)
        assertEquals("op · signal 3/4", entry.networkDetail)
        assertEquals(true, entry.telemetrySent)
    }

}
