package org.librespeed.speedtest.share

import android.content.Context
import android.content.Intent
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.GeoDistance
import java.util.Locale

object ShareResult {

    fun copy(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("LibreSpeed", text))
        } catch (_: Exception) {
        }
    }

    fun shareLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_result)))
        } catch (_: Exception) {
        }
    }

    fun buildText(
        context: Context,
        server: String,
        download: Double,
        upload: Double,
        ping: Double,
        jitter: Double,
        loss: Double,
        useMBytes: Boolean,
        shareUrl: String?,
        networkType: String? = null,
        ipVersion: Int = 0
    ): String {
        val unit = context.getString(if (useMBytes) R.string.unit_mbytes else R.string.unit_mbps)
        fun speed(value: Double) =
            if (value < 0) "—"
            else String.format(Locale.getDefault(), "%.2f %s", if (useMBytes) value / 8 else value, unit)

        fun number(value: Double) =
            if (value < 0) "—" else String.format(Locale.getDefault(), "%.1f", value)

        return buildString {
            appendLine(
                context.getString(
                    R.string.share_text,
                    speed(download), speed(upload), number(ping), number(jitter),
                    GeoDistance.cleanName(server)
                )
            )
            if (loss >= 0) {
                appendLine(context.getString(R.string.share_text_loss, number(loss)))
            }
            networkType?.let { appendLine(context.getString(R.string.share_text_network, it)) }
            if (ipVersion != 0) {
                appendLine(context.getString(R.string.share_text_protocol, ipVersion))
            }
            shareUrl?.let { appendLine(it) }
        }.trimEnd()
    }

    fun share(
        context: Context,
        server: String,
        download: Double,
        upload: Double,
        ping: Double,
        jitter: Double,
        loss: Double,
        useMBytes: Boolean,
        shareUrl: String?,
        networkType: String? = null,
        ipVersion: Int = 0
    ) {
        val text = buildText(context, server, download, upload, ping, jitter, loss, useMBytes, shareUrl, networkType, ipVersion)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_result)))
        } catch (_: Exception) {
        }
    }

}
