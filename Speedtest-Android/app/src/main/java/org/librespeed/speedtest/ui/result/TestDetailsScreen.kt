package org.librespeed.speedtest.ui.result

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.BuildConfig
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.currentLocale
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.data.TestStats
import org.librespeed.speedtest.ui.history.formatDate
import java.util.Locale

@Composable
fun TestDetailsScreen(entryId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context.applicationContext) }
    val useMBytes by prefs.useMBytes.collectAsStateWithLifecycle(initialValue = false)
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }
    var missing by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        val loaded = withContext(Dispatchers.IO) { HistoryDatabase(context.applicationContext).read(entryId) }
        if (loaded == null) missing = true else entry = loaded
    }
    LaunchedEffect(missing) { if (missing) onBack() }

    val result = entry ?: return

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
            Text(
                text = stringResource(R.string.test_details_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            Section(stringResource(R.string.section_test)) {
                result.shareUrl?.let {
                    DetailRow(stringResource(R.string.detail_result_id), it.substringAfterLast("=", it), Icons.Filled.Tag)
                    RowDivider()
                }
                DetailRow(stringResource(R.string.section_date), formatDate(result.date), Icons.Filled.Event)
                if (result.durationMs > 0) {
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_duration), stringResource(R.string.unit_seconds_fmt, String.format(currentLocale, "%.1f", result.durationMs / 1000.0)), Icons.Filled.Schedule)
                }
                result.mode?.let {
                    RowDivider()
                    DetailRow(
                        stringResource(R.string.detail_mode),
                        stringResource(
                            when (it) {
                                "single" -> R.string.mode_single
                                "stability" -> R.string.mode_stability
                                "scheduled" -> R.string.mode_scheduled
                                else -> R.string.mode_standard
                            }
                        ),
                        Icons.Filled.Tune
                    )
                }
            }

            Section(stringResource(R.string.section_server)) {
                DetailRow(stringResource(R.string.servers_add_name), GeoDistance.cleanName(result.server), Icons.Filled.Public)
                GeoDistance.sponsor(result.server)?.let {
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_isp), it, Icons.Filled.Business)
                }
                if (result.ping >= 0) {
                    RowDivider()
                    DetailRow(stringResource(R.string.info_latency), String.format(currentLocale, "%.1f %s", result.ping, stringResource(R.string.unit_ms)), Icons.Filled.NetworkPing)
                }
            }

            Section(stringResource(R.string.section_network)) {
                if (result.ipVersion != 0) {
                    DetailRow(stringResource(R.string.detail_protocol), "IPv${result.ipVersion}", Icons.Filled.Lan)
                    RowDivider()
                }
                result.networkType?.let {
                    DetailRow(stringResource(R.string.detail_network), it, Icons.Filled.Wifi)
                    RowDivider()
                }
                result.ipInfo?.takeIf { it.isNotBlank() }?.let { info ->
                    val ip = info.substringBefore(" - ").trim()
                    val provider = info.substringAfter(" - ", "").trim()
                    DetailRow(stringResource(R.string.detail_ip), ip, Icons.Filled.MyLocation)
                    if (provider.isNotEmpty()) {
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_isp), provider, Icons.Filled.Business)
                    }
                }
                result.networkDetail?.let {
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_operator), it, Icons.Filled.SignalCellularAlt)
                }
                if (result.loadedDown >= 0) {
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_loaded_dl), String.format(currentLocale, "%.1f %s", result.loadedDown, stringResource(R.string.unit_ms)), Icons.Filled.NetworkCheck)
                }
                if (result.loadedUp >= 0) {
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_loaded_ul), String.format(currentLocale, "%.1f %s", result.loadedUp, stringResource(R.string.unit_ms)), Icons.Filled.NetworkCheck)
                }
                TestStats.bufferbloatGrade(result.ping, maxOf(result.loadedDown, result.loadedUp))?.let { grade ->
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_bufferbloat), grade, Icons.Filled.NetworkCheck)
                }
            }

            if (result.mode == "stability") {
                TestStats.stability(result.downloadSamples)?.let { s ->
                    val speedUnit = stringResource(if (useMBytes) R.string.unit_mbytes else R.string.unit_mbps)
                    //read here, in composable scope; the local function below is not one
                    val locale = currentLocale
                    fun speed(value: Double) = String.format(locale, "%.2f %s", if (useMBytes) value / 8 else value, speedUnit)
                    Section(stringResource(R.string.section_stability)) {
                        DetailRow(stringResource(R.string.detail_min), speed(s.min), Icons.AutoMirrored.Filled.TrendingDown)
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_max), speed(s.max), Icons.AutoMirrored.Filled.TrendingUp)
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_avg), speed(s.average), Icons.Filled.Timeline)
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_variation), "± ${s.variationPct} ${stringResource(R.string.unit_percent)}", Icons.Filled.SsidChart)
                    }
                }
            }

            Section(stringResource(R.string.section_data)) {
                val downloadMb = result.downloadSamples.sum() * 0.1 / 8
                val uploadMb = result.uploadSamples.sum() * 0.1 / 8
                val locale = currentLocale
                fun mb(value: Double) = String.format(locale, "%.0f", value)
                if (downloadMb > 0) {
                    DetailRow(stringResource(R.string.detail_dl_data), stringResource(R.string.data_mb_fmt, mb(downloadMb)), Icons.Filled.ArrowDownward)
                    RowDivider()
                }
                if (uploadMb > 0) {
                    DetailRow(stringResource(R.string.detail_ul_data), stringResource(R.string.data_mb_fmt, mb(uploadMb)), Icons.Filled.ArrowUpward)
                    RowDivider()
                }
                DetailRow(stringResource(R.string.detail_data), stringResource(R.string.data_mb_fmt, mb(downloadMb + uploadMb)), Icons.Filled.DataUsage)
            }

            Section(stringResource(R.string.section_telemetry)) {
                //shareUrl only proves the server confirmed the stored result; the payload
                //was transmitted whenever the run had telemetry on (legacy rows: shareUrl)
                DetailRow(
                    stringResource(R.string.settings_section_telemetry),
                    stringResource(if (result.telemetrySent || result.shareUrl != null) R.string.telemetry_submitted else R.string.telemetry_not_submitted),
                    Icons.Filled.CloudUpload
                )
            }

            Section(stringResource(R.string.section_client)) {
                DetailRow(stringResource(R.string.section_application), "LibreSpeed", Icons.Filled.Apps)
                RowDivider()
                DetailRow(stringResource(R.string.section_version), BuildConfig.VERSION_NAME, Icons.Filled.Info)
                RowDivider()
                DetailRow("Android", Build.VERSION.RELEASE, Icons.Filled.Android)
                RowDivider()
                DetailRow(stringResource(R.string.section_device), "${Build.MANUFACTURER} ${Build.MODEL}", Icons.Filled.Smartphone)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}
