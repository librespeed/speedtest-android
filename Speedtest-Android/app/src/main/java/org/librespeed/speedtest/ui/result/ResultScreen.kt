package org.librespeed.speedtest.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.currentLocale
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.data.TestStats
import org.librespeed.speedtest.share.ShareResult
import org.librespeed.speedtest.ui.components.Sparkline
import org.librespeed.speedtest.ui.history.formatDate
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents
import java.util.Locale

@Composable
fun ResultScreen(
    entryId: Long,
    onBack: () -> Unit,
    onTestAgain: (String) -> Unit,
    onTestDetails: (Long) -> Unit,
    onShare: (Long) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context.applicationContext) }
    val useMBytes by prefs.useMBytes.collectAsStateWithLifecycle(initialValue = false)
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }
    var missing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var bloatInfo by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        val loaded = withContext(Dispatchers.IO) { HistoryDatabase(context.applicationContext).read(entryId) }
        if (loaded == null) missing = true else entry = loaded
    }
    LaunchedEffect(missing) { if (missing) onBack() }

    val result = entry ?: return
    val unitLabel = stringResource(if (useMBytes) R.string.unit_mbytes else R.string.unit_mbps)
    fun display(value: Double): Double = if (useMBytes) value / 8 else value

    fun copyText() {
        ShareResult.copy(
            context,
            ShareResult.buildText(
                context, result.server, result.download, result.upload, result.ping,
                result.jitter, result.loss, useMBytes, result.shareUrl,
                result.networkType, result.ipVersion
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
            Text(
                text = stringResource(R.string.result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onShare(result.id) }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_result), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.test_details_title)) },
                        onClick = {
                            menuOpen = false
                            onTestDetails(result.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_copy)) },
                        onClick = {
                            menuOpen = false
                            copyText()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            //a composition-bound scope dies with the popped screen and can
                            //cancel the delete before it starts; this one outlives both
                            CoroutineScope(Dispatchers.IO).launch {
                                HistoryDatabase(context.applicationContext).delete(result.id)
                            }
                            onBack()
                        }
                    )
                }
            }
        }

        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
            ) {
                Icon(
                    Icons.Filled.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = formatDate(result.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = listOfNotNull(
                            GeoDistance.cleanName(result.server),
                            GeoDistance.sponsor(result.server)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val accents = LocalSpeedAccents.current
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeedCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowCircleDown,
                    label = stringResource(R.string.test_download),
                    value = display(result.download),
                    unit = unitLabel,
                    accent = accents.download,
                    samples = result.downloadSamples
                )
                SpeedCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowCircleUp,
                    label = stringResource(R.string.test_upload),
                    value = display(result.upload),
                    unit = unitLabel,
                    accent = accents.upload,
                    samples = result.uploadSamples
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBox(Modifier.weight(1f), Icons.Filled.NetworkPing, stringResource(R.string.test_ping), result.ping, stringResource(R.string.unit_ms))
                MetricBox(Modifier.weight(1f), Icons.Filled.SsidChart, stringResource(R.string.test_jitter), result.jitter, stringResource(R.string.unit_ms))
            }

            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (result.ipVersion != 0) {
                        DetailRow(stringResource(R.string.detail_protocol), "IPv${result.ipVersion}", Icons.Filled.Lan)
                        RowDivider()
                    }
                    result.ipInfo?.takeIf { it.isNotBlank() }?.let { info ->
                        DetailRow(stringResource(R.string.detail_ip), info.substringBefore(" - ").trim(), Icons.Filled.MyLocation)
                        RowDivider()
                    }
                    DetailRow(stringResource(R.string.detail_client), "Android ${android.os.Build.VERSION.RELEASE}", Icons.Filled.PhoneAndroid)
                    RowDivider()
                    DetailRow(stringResource(R.string.detail_server), GeoDistance.cleanName(result.server), Icons.Filled.Public)
                    result.shareUrl?.let {
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_result_id), it.substringAfterLast("=", it), Icons.Filled.Tag)
                    }
                    if (result.durationMs > 0) {
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_duration), stringResource(R.string.unit_seconds_fmt, String.format(currentLocale, "%.1f", result.durationMs / 1000.0)), Icons.Filled.Schedule)
                    }
                    val totalMb = (result.downloadSamples.sum() + result.uploadSamples.sum()) * 0.1 / 8
                    if (totalMb > 0) {
                        RowDivider()
                        DetailRow(stringResource(R.string.detail_data), stringResource(R.string.data_mb_fmt, String.format(currentLocale, "%.0f", totalMb)), Icons.Filled.DataUsage)
                    }
                    val loadedMax = maxOf(result.loadedDown, result.loadedUp)
                    TestStats.bufferbloatGrade(result.ping, loadedMax)?.let { grade ->
                        RowDivider()
                        DetailRow(
                            stringResource(R.string.detail_bufferbloat),
                            String.format(currentLocale, "%s · +%.0f %s", grade, (loadedMax - result.ping).coerceAtLeast(0.0), stringResource(R.string.unit_ms)),
                            Icons.Filled.NetworkCheck,
                            onClick = { bloatInfo = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onShare(result.id) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_result), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onTestAgain(result.server) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.result_test_again), style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTestDetails(result.id) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (bloatInfo) {
        AlertDialog(
            onDismissRequest = { bloatInfo = false },
            title = { Text(stringResource(R.string.detail_bufferbloat)) },
            text = { Text(stringResource(R.string.bufferbloat_info)) },
            confirmButton = {
                TextButton(onClick = { bloatInfo = false }) { Text(stringResource(R.string.dialog_close)) }
            }
        )
    }
}

@Composable
private fun SpeedCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: Double,
    unit: String,
    accent: Color,
    samples: List<Double>
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (value < 0) "—" else String.format(currentLocale, "%.2f", value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (samples.size > 1) {
                Sparkline(
                    data = samples,
                    color = accent,
                    modifier = Modifier.fillMaxWidth().height(30.dp).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricBox(modifier: Modifier, icon: ImageVector, title: String, value: Double, unit: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (value < 0) "—" else String.format(currentLocale, "%.1f", value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun DetailRow(title: String, value: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 9.dp)
    }
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(
                it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}
