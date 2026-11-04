package org.librespeed.speedtest.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fdossena.speedtest.core.Speedtest
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.MainActivity
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.data.NetworkInfo
import org.librespeed.speedtest.data.key
import org.librespeed.speedtest.engine.TestEngine
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object ScheduledTests {

    const val WORK_NAME = "scheduled_test"

    /** mode: "off", "6h", "daily" or "weekly" */
    fun apply(context: Context, mode: String) {
        val workManager = WorkManager.getInstance(context)
        if (mode == "off") {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val (interval, unit) = when (mode) {
            "6h" -> 6L to TimeUnit.HOURS
            "weekly" -> 7L to TimeUnit.DAYS
            else -> 1L to TimeUnit.DAYS
        }
        val request = PeriodicWorkRequestBuilder<ScheduledTestWorker>(interval, unit)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

}

/** Runs a full speed test in the background and saves it to the history like a manual run. */
class ScheduledTestWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext
        val prefs = AppPreferences(app)
        try {
            val engine = TestEngine(app)
            val discovery = engine.discover(prefs.customServers.first())
            val remembered = prefs.rememberedServer.first()
                ?.let { key -> discovery.servers.find { it.key() == key && it.ping >= 0 } }
            val server = remembered ?: discovery.selected ?: return@withContext Result.retry()
            val entry = runTest(engine, discovery.servers, server, prefs.telemetryEnabled.first())
                ?: return@withContext Result.retry()
            val id = HistoryDatabase(app).insert(entry)
            notify(app, entry, id)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun runTest(
        engine: TestEngine,
        servers: List<TestPoint>,
        server: TestPoint,
        telemetryEnabled: Boolean
    ): HistoryEntry? = suspendCancellableCoroutine { continuation ->
        val startedAt = System.currentTimeMillis()
        val app = applicationContext
        val networkType = NetworkInfo.describe(app)
        val networkDetail = NetworkInfo.detail(app)
        val downloadSamples = Collections.synchronizedList(mutableListOf<Double>())
        val uploadSamples = Collections.synchronizedList(mutableListOf<Double>())
        var download = -1.0
        var upload = -1.0
        var ping = -1.0
        var jitter = -1.0
        var loss = -1.0
        var ipInfo: String? = null
        var shareUrl: String? = null

        engine.prepare(servers, server, telemetryEnabled)
        engine.start(object : Speedtest.SpeedtestHandler() {
            override fun onDownloadUpdate(dl: Double, progress: Double) {
                if (dl > 0) download = dl
                if (progress > 0) downloadSamples.add(dl)
            }

            override fun onUploadUpdate(ul: Double, progress: Double) {
                if (ul > 0) upload = ul
                if (progress > 0) uploadSamples.add(ul)
            }

            override fun onPingJitterUpdate(p: Double, j: Double, progress: Double) {
                ping = p
                jitter = j
            }

            override fun onLossUpdate(l: Double) {
                loss = l
            }

            override fun onIPInfoUpdate(info: String?) {
                ipInfo = info
            }

            override fun onTestIDReceived(id: String?, shareURL: String?) {
                shareUrl = shareURL
            }

            override fun onEnd() {
                if (!continuation.isActive) return
                if (download < 0) {
                    continuation.resume(null)
                    return
                }
                continuation.resume(
                    HistoryEntry(
                        date = System.currentTimeMillis(),
                        server = server.name,
                        ping = ping,
                        jitter = jitter,
                        download = download,
                        upload = upload,
                        loss = loss,
                        ipInfo = ipInfo,
                        ipVersion = server.ipVersion,
                        shareUrl = shareUrl,
                        networkType = networkType,
                        downloadSamples = downloadSamples.toList(),
                        uploadSamples = uploadSamples.toList(),
                        durationMs = System.currentTimeMillis() - startedAt,
                        mode = "scheduled",
                        networkDetail = networkDetail,
                        telemetrySent = telemetryEnabled
                    )
                )
            }

            override fun onCriticalFailure(err: String?) {
                if (continuation.isActive) continuation.resume(null)
            }
        })
        continuation.invokeOnCancellation { engine.abort() }
    }

    private fun notify(context: Context, entry: HistoryEntry, entryId: Long) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                "results",
                context.getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = String.format(
            Locale.getDefault(), "↓ %.0f · ↑ %.0f %s · %.0f %s",
            entry.download, entry.upload, context.getString(R.string.unit_mbps),
            entry.ping, context.getString(R.string.unit_ms)
        )
        val notification = NotificationCompat.Builder(context, "results")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(entryId.toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

}
