package org.librespeed.speedtest.ui.servers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdossena.speedtest.core.serverSelector.TestPoint
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.key
import org.librespeed.speedtest.ui.speedtest.SpeedtestViewModel
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ServersScreen(viewModel: SpeedtestViewModel, onCompareClick: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    //start on All servers when there are no favorites yet
    var tabOverride by remember { mutableIntStateOf(-1) }
    val tab = if (tabOverride != -1) tabOverride else if (state.favorites.isEmpty()) 1 else 0
    var showAddDialog by remember { mutableStateOf(false) }
    var sheetServer by remember { mutableStateOf<TestPoint?>(null) }
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocation = granted
        if (granted) viewModel.computeDistances()
    }

    val sorted = remember(state.servers, state.customKeys) {
        state.servers
            .filter { it.ping >= 0 || it.key() in state.customKeys }
            .sortedBy { if (it.ping < 0) Float.MAX_VALUE else it.ping }
    }
    val visible = if (tab == 0) sorted.filter { it.key() in state.favorites } else sorted

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_servers),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCompareClick) {
                Icon(Icons.Filled.Insights, contentDescription = stringResource(R.string.servers_compare))
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.servers_add))
            }
            IconButton(onClick = { viewModel.refreshServers() }, enabled = !state.selectingServers) {
                if (state.selectingServers) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.servers_refresh))
                }
            }
        }
        Card(
            onClick = { viewModel.useAutoSelect() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = if (!state.pinnedServer) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.server_auto_select),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!state.pinnedServer && state.selectedServer != null) {
                        Text(
                            text = GeoDistance.cleanName(state.selectedServer!!.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!state.pinnedServer) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (!hasLocation) {
            TextButton(
                onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.servers_show_distances))
            }
        }
        SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tabOverride = 0 }, text = { Text(stringResource(R.string.servers_favorites)) })
            Tab(selected = tab == 1, onClick = { tabOverride = 1 }, text = { Text(stringResource(R.string.servers_all)) })
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(if (tab == 0) R.string.servers_no_favorites else R.string.servers_none),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(visible, key = { it.key() }) { server ->
                    ServerRow(
                        server = server,
                        selected = state.pinnedServer && state.selectedServer?.key() == server.key(),
                        favorite = server.key() in state.favorites,
                        distanceKm = state.distances[server.key()],
                        onClick = { viewModel.selectServer(server) },
                        onToggleFavorite = { viewModel.toggleFavorite(server) },
                        onMore = { sheetServer = server }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    sheetServer?.let { server ->
        ServerSheet(
            server = server,
            favorite = server.key() in state.favorites,
            custom = server.key() in state.customKeys,
            distanceKm = state.distances[server.key()],
            onSelect = {
                viewModel.selectServer(server)
                sheetServer = null
            },
            onToggleFavorite = { viewModel.toggleFavorite(server) },
            onPing = { viewModel.pingServer(server) },
            onDelete = {
                viewModel.removeCustomServer(server)
                sheetServer = null
            },
            onDismiss = { sheetServer = null }
        )
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url ->
                if (viewModel.addCustomServer(name, url)) {
                    showAddDialog = false
                    true
                } else false
            }
        )
    }
}

@Composable
private fun latencyColor(ping: Float): Color = LocalSpeedAccents.current.let {
    when {
        ping < 50 -> it.download
        ping < 150 -> it.warn
        else -> it.bad
    }
}

@Composable
private fun ServerRow(
    server: TestPoint,
    selected: Boolean,
    favorite: Boolean,
    distanceKm: Int?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = GeoDistance.cleanName(server.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = GeoDistance.hostLabel(server.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (server.ping >= 0) {
                    Text(
                        text = "${server.ping.toInt()} ${stringResource(R.string.unit_ms)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = latencyColor(server.ping)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.servers_unreachable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                distanceKm?.let {
                    Text(
                        text = "$it km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.servers_favorite),
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.servers_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.servers_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.servers_add_name)) }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.servers_add_url)) },
                    placeholder = { Text("https://speedtest.example.com/backend") },
                    isError = failed
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(if (failed) R.string.servers_add_invalid else R.string.servers_add_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && url.isNotBlank(),
                onClick = { failed = !onAdd(name, url) }
            ) { Text(stringResource(R.string.servers_add_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}
