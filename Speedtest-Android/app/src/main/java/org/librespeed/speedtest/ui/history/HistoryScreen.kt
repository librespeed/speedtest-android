package org.librespeed.speedtest.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.currentLocale
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.share.HistoryExport
import org.librespeed.speedtest.ui.components.Sparkline
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpen: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val allNetworks by viewModel.allNetworks.collectAsStateWithLifecycle()
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var filterDialog by remember { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_history),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { filterDialog = true }, enabled = entries.isNotEmpty() || filter.active) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.history_filter),
                    tint = if (filter.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { confirmClear = true }, enabled = entries.isNotEmpty()) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.history_clear))
            }
            Box {
                IconButton(onClick = { exportMenu = true }, enabled = entries.isNotEmpty()) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_export_csv)) },
                        onClick = {
                            exportMenu = false
                            HistoryExport.shareCsv(context, entries)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_export_json)) },
                        onClick = {
                            exportMenu = false
                            HistoryExport.shareJson(context, entries)
                        }
                    )
                }
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                if (entries.size >= 3) {
                    item(key = "trends") {
                        TrendsCard(entries)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, onClick = { onOpen(entry.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text(stringResource(R.string.history_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
    if (filterDialog) {
        FilterDialog(
            filter = filter,
            networks = allNetworks,
            servers = allServers,
            onApply = { viewModel.setFilter(it) },
            onDismiss = { filterDialog = false }
        )
    }
}

fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

@Composable
private fun TrendsCard(entries: List<HistoryEntry>) {
    //oldest to newest so the curve reads left to right
    val chronological = remember(entries) { entries.sortedBy { it.date } }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.history_trends).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val accents = LocalSpeedAccents.current
            Row {
                TrendColumn(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.test_download),
                    average = chronological.map { it.download }.average(),
                    samples = chronological.map { it.download },
                    accent = accents.download
                )
                Spacer(Modifier.width(14.dp))
                TrendColumn(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.test_upload),
                    average = chronological.map { it.upload }.average(),
                    samples = chronological.map { it.upload },
                    accent = accents.upload
                )
            }
        }
    }
}

@Composable
private fun TrendColumn(
    modifier: Modifier,
    label: String,
    average: Double,
    samples: List<Double>,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format(currentLocale, "%.0f", average),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${stringResource(R.string.unit_mbps)} ø",
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Sparkline(
            data = samples,
            color = accent,
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(top = 4.dp)
        )
    }
}

@Composable
private fun FilterDialog(
    filter: HistoryFilter,
    networks: List<String>,
    servers: List<String>,
    onApply: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_filter)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FilterGroup(
                    title = stringResource(R.string.filter_period),
                    options = listOf(
                        stringResource(R.string.filter_all) to null,
                        stringResource(R.string.filter_days_7) to 7,
                        stringResource(R.string.filter_days_30) to 30
                    ),
                    selected = filter.days,
                    onSelect = { onApply(filter.copy(days = it)) }
                )
                if (networks.isNotEmpty()) {
                    FilterGroup(
                        title = stringResource(R.string.filter_network),
                        options = listOf(stringResource(R.string.filter_all) to null) + networks.map { it to it },
                        selected = filter.network,
                        onSelect = { onApply(filter.copy(network = it)) }
                    )
                }
                if (servers.isNotEmpty()) {
                    FilterGroup(
                        title = stringResource(R.string.filter_server),
                        options = listOf(stringResource(R.string.filter_all) to null) + servers.map { it to it },
                        selected = filter.server,
                        onSelect = { onApply(filter.copy(server = it)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
        }
    )
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<Pair<String, T?>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Text(
        text = title.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
    options.forEach { (label, value) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == value, onClick = { onSelect(value) })
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    entry.networkType == null -> Icons.Filled.Public
                    entry.networkType.startsWith("Wi-Fi") -> Icons.Filled.Wifi
                    entry.networkType == "Ethernet" -> Icons.Filled.SettingsEthernet
                    else -> Icons.Filled.SignalCellularAlt
                },
                contentDescription = entry.networkType,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(formatDate(entry.date), entry.networkType).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = GeoDistance.cleanName(entry.server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(currentLocale, "%.0f", entry.download),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalSpeedAccents.current.download
                )
                Text(
                    text = stringResource(R.string.unit_mbps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(currentLocale, "%.0f", entry.upload),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalSpeedAccents.current.upload
                )
                Text(
                    text = stringResource(R.string.unit_mbps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
