package org.librespeed.speedtest.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

data class HistoryEntry(
    val id: Long = 0,
    val date: Long,
    val server: String,
    val ping: Double,
    val jitter: Double,
    val download: Double,
    val upload: Double,
    val loss: Double,
    val ipInfo: String?,
    val ipVersion: Int,
    val shareUrl: String?,
    val networkType: String? = null,
    val downloadSamples: List<Double> = emptyList(),
    val uploadSamples: List<Double> = emptyList(),
    val durationMs: Long = 0,
    val mode: String? = null,
    val loadedDown: Double = -1.0,
    val loadedUp: Double = -1.0,
    val networkDetail: String? = null,
    /** A telemetry POST was made for this run; [shareUrl] additionally confirms the server stored it. */
    val telemetrySent: Boolean = false
)

class HistoryDatabase(context: Context) : SQLiteOpenHelper(context, "history.db", null, 5) {

    override fun onCreate(db: SQLiteDatabase) {
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
                "shareUrl TEXT," +
                "networkType TEXT," +
                "dlSamples TEXT," +
                "ulSamples TEXT," +
                "duration INTEGER NOT NULL DEFAULT 0," +
                "mode TEXT," +
                "loadedDown REAL NOT NULL DEFAULT -1," +
                "loadedUp REAL NOT NULL DEFAULT -1," +
                "networkDetail TEXT," +
                "telemetrySent INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE history ADD COLUMN networkType TEXT")
            db.execSQL("ALTER TABLE history ADD COLUMN dlSamples TEXT")
            db.execSQL("ALTER TABLE history ADD COLUMN ulSamples TEXT")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE history ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE history ADD COLUMN mode TEXT")
            db.execSQL("ALTER TABLE history ADD COLUMN loadedDown REAL NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE history ADD COLUMN loadedUp REAL NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE history ADD COLUMN networkDetail TEXT")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE history ADD COLUMN telemetrySent INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun insert(entry: HistoryEntry): Long = writableDatabase.insert(
        "history", null,
        ContentValues().apply {
            put("date", entry.date)
            put("server", entry.server)
            put("ping", entry.ping)
            put("jitter", entry.jitter)
            put("download", entry.download)
            put("upload", entry.upload)
            put("loss", entry.loss)
            put("ipInfo", entry.ipInfo)
            put("ipVersion", entry.ipVersion)
            put("shareUrl", entry.shareUrl)
            put("networkType", entry.networkType)
            put("dlSamples", entry.downloadSamples.toJson())
            put("ulSamples", entry.uploadSamples.toJson())
            put("duration", entry.durationMs)
            put("mode", entry.mode)
            put("loadedDown", entry.loadedDown)
            put("loadedUp", entry.loadedUp)
            put("networkDetail", entry.networkDetail)
            put("telemetrySent", if (entry.telemetrySent) 1 else 0)
        }
    )

    fun readAll(): List<HistoryEntry> = query("SELECT * FROM history ORDER BY date DESC", null)

    fun read(id: Long): HistoryEntry? = query("SELECT * FROM history WHERE id=?", arrayOf(id.toString())).firstOrNull()

    fun delete(id: Long) {
        writableDatabase.delete("history", "id=?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete("history", null, null)
    }

    private fun query(sql: String, args: Array<String>?): List<HistoryEntry> {
        val result = mutableListOf<HistoryEntry>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    HistoryEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        date = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
                        server = cursor.getString(cursor.getColumnIndexOrThrow("server")),
                        ping = cursor.getDouble(cursor.getColumnIndexOrThrow("ping")),
                        jitter = cursor.getDouble(cursor.getColumnIndexOrThrow("jitter")),
                        download = cursor.getDouble(cursor.getColumnIndexOrThrow("download")),
                        upload = cursor.getDouble(cursor.getColumnIndexOrThrow("upload")),
                        loss = cursor.getDouble(cursor.getColumnIndexOrThrow("loss")),
                        ipInfo = cursor.getString(cursor.getColumnIndexOrThrow("ipInfo")),
                        ipVersion = cursor.getInt(cursor.getColumnIndexOrThrow("ipVersion")),
                        shareUrl = cursor.getString(cursor.getColumnIndexOrThrow("shareUrl")),
                        networkType = cursor.getString(cursor.getColumnIndexOrThrow("networkType")),
                        downloadSamples = cursor.getString(cursor.getColumnIndexOrThrow("dlSamples")).fromJson(),
                        uploadSamples = cursor.getString(cursor.getColumnIndexOrThrow("ulSamples")).fromJson(),
                        durationMs = cursor.getLong(cursor.getColumnIndexOrThrow("duration")),
                        mode = cursor.getString(cursor.getColumnIndexOrThrow("mode")),
                        loadedDown = cursor.getDouble(cursor.getColumnIndexOrThrow("loadedDown")),
                        loadedUp = cursor.getDouble(cursor.getColumnIndexOrThrow("loadedUp")),
                        networkDetail = cursor.getString(cursor.getColumnIndexOrThrow("networkDetail")),
                        telemetrySent = cursor.getInt(cursor.getColumnIndexOrThrow("telemetrySent")) != 0
                    )
                )
            }
        }
        return result
    }

    private fun List<Double>.toJson(): String {
        val array = JSONArray()
        forEach { array.put(it) }
        return array.toString()
    }

    private fun String?.fromJson(): List<Double> = try {
        val array = JSONArray(this ?: "[]")
        (0 until array.length()).map { array.getDouble(it) }
    } catch (_: Exception) {
        emptyList()
    }

}
