package org.librespeed.speedtest.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.HistoryEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryExport {

    fun shareCsv(context: Context, entries: List<HistoryEntry>) =
        share(context, buildCsv(entries), "librespeed-history.csv", "text/csv")

    fun shareJson(context: Context, entries: List<HistoryEntry>) =
        share(context, buildJson(entries), "librespeed-history.json", "application/json")

    fun buildCsv(entries: List<HistoryEntry>): String = buildString {
        appendLine("date,server,network,protocol,download_mbps,upload_mbps,ping_ms,jitter_ms,loss_pct,loaded_down_ms,loaded_up_ms,duration_s,mode")
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        entries.forEach { entry ->
            appendLine(
                listOf(
                    format.format(Date(entry.date)),
                    entry.server.csv(),
                    entry.networkType.orEmpty().csv(),
                    if (entry.ipVersion != 0) "IPv${entry.ipVersion}" else "",
                    entry.download.num(),
                    entry.upload.num(),
                    entry.ping.num(),
                    entry.jitter.num(),
                    if (entry.loss >= 0) entry.loss.num() else "",
                    if (entry.loadedDown >= 0) entry.loadedDown.num() else "",
                    if (entry.loadedUp >= 0) entry.loadedUp.num() else "",
                    if (entry.durationMs > 0) (entry.durationMs / 1000.0).num() else "",
                    entry.mode.orEmpty().csv()
                ).joinToString(",")
            )
        }
    }

    fun buildJson(entries: List<HistoryEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("date", entry.date)
                put("server", entry.server)
                entry.networkType?.let { put("network", it) }
                if (entry.ipVersion != 0) put("protocol", "IPv${entry.ipVersion}")
                put("download_mbps", entry.download)
                put("upload_mbps", entry.upload)
                put("ping_ms", entry.ping)
                put("jitter_ms", entry.jitter)
                if (entry.loss >= 0) put("loss_pct", entry.loss)
                if (entry.loadedDown >= 0) put("loaded_down_ms", entry.loadedDown)
                if (entry.loadedUp >= 0) put("loaded_up_ms", entry.loadedUp)
                if (entry.durationMs > 0) put("duration_ms", entry.durationMs)
                entry.mode?.let { put("mode", it) }
            })
        }
        return array.toString(2)
    }

    private fun share(context: Context, content: String, filename: String, mime: String) {
        try {
            val directory = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(directory, filename)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.nav_history)))
        } catch (_: Exception) {
        }
    }

    //spreadsheets evaluate a leading =, +, - or @ even inside a quoted field,
    //and RFC 4180 requires quoting embedded line breaks
    private fun String.csv(): String {
        val first = trimStart().firstOrNull()
        val defused = if (first != null && first in "=+-@") "'$this" else this
        return if (defused.any { it in ",\"\r\n" }) "\"${defused.replace("\"", "\"\"")}\"" else defused
    }

    private fun Double.num(): String = String.format(Locale.US, "%.2f", this)

}
