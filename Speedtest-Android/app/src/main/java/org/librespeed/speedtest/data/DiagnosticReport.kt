package org.librespeed.speedtest.data

import android.content.Context
import android.os.Build
import org.librespeed.speedtest.ui.history.formatDate
import java.util.Locale

object DiagnosticReport {

    /** Plain-text summary for bug reports; never includes the IP address. */
    fun build(
        context: Context,
        lastEntry: HistoryEntry?,
        telemetryEnabled: Boolean,
        testMode: String,
        serverCount: Int
    ): String = buildString {
        appendLine("LibreSpeed diagnostic report")
        appendLine("Application: ${ClientInfo.client} (${org.librespeed.speedtest.BuildConfig.BUILD_TYPE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Locale: ${Locale.getDefault()}")
        appendLine("Network: ${NetworkInfo.describe(context) ?: "unknown"}")
        NetworkInfo.detail(context)?.let { appendLine("Mobile network: $it") }
        appendLine("Test mode: $testMode")
        appendLine("Telemetry: ${if (telemetryEnabled) "on" else "off"}")
        appendLine("Servers available: $serverCount")
        if (lastEntry == null) {
            appendLine("Last test: none")
        } else {
            appendLine("Last test: ${formatDate(lastEntry.date)}")
            appendLine("  Server: ${lastEntry.server}")
            appendLine(
                String.format(
                    Locale.US, "  Download: %.2f Mbps, Upload: %.2f Mbps",
                    lastEntry.download, lastEntry.upload
                )
            )
            appendLine(
                String.format(
                    Locale.US, "  Ping: %.1f ms, Jitter: %.1f ms, Loss: %s",
                    lastEntry.ping, lastEntry.jitter,
                    if (lastEntry.loss >= 0) String.format(Locale.US, "%.1f %%", lastEntry.loss) else "n/a"
                )
            )
            if (lastEntry.loadedDown >= 0) {
                appendLine(String.format(Locale.US, "  Latency under download: %.1f ms", lastEntry.loadedDown))
            }
            if (lastEntry.loadedUp >= 0) {
                appendLine(String.format(Locale.US, "  Latency under upload: %.1f ms", lastEntry.loadedUp))
            }
            lastEntry.networkType?.let { appendLine("  Network: $it") }
            if (lastEntry.ipVersion != 0) appendLine("  Protocol: IPv${lastEntry.ipVersion}")
        }
    }

}
