package org.librespeed.speedtest.ui.servers

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.key
import org.librespeed.speedtest.ui.history.formatDate
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ServerSheet(
    server: TestPoint,
    favorite: Boolean,
    custom: Boolean,
    distanceKm: Int?,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val host = GeoDistance.hostLabel(server.server)
    var ipv4 by remember { mutableStateOf<String?>(null) }
    var ipv6 by remember { mutableStateOf<String?>(null) }
    var lastTest by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(server.key()) {
        withContext(Dispatchers.IO) {
            try {
                val addresses = InetAddress.getAllByName(host.substringBefore("/").substringBefore(":"))
                ipv4 = addresses.firstOrNull { it is Inet4Address }?.hostAddress
                ipv6 = addresses.firstOrNull { it is Inet6Address }?.hostAddress
            } catch (_: Exception) {
            }
            lastTest = try {
                HistoryDatabase(context.applicationContext).readAll()
                    .firstOrNull { it.server == server.name }?.date
            } catch (_: Exception) {
                null
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = GeoDistance.cleanName(server.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (server.ping >= 0) {
                    Text(
                        text = "${server.ping.toInt()} ${stringResource(R.string.unit_ms)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.servers_favorite),
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SheetAction(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.sheet_select),
                subtitle = stringResource(R.string.sheet_select_hint),
                onClick = onSelect
            )
            if (!favorite) {
                SheetAction(
                    icon = Icons.Outlined.StarBorder,
                    title = stringResource(R.string.sheet_favorite),
                    subtitle = stringResource(R.string.sheet_favorite_hint),
                    onClick = onToggleFavorite
                )
            }
            SheetAction(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.sheet_info),
                subtitle = stringResource(R.string.sheet_info_hint),
                onClick = null
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    InfoRow(stringResource(R.string.info_location), GeoDistance.cleanName(server.name))
                    GeoDistance.sponsor(server.name)?.let { InfoRow(stringResource(R.string.detail_isp), it) }
                    InfoRow(stringResource(R.string.detail_server), host)
                    distanceKm?.let { InfoRow(stringResource(R.string.info_distance), "~ $it km") }
                    if (server.ping >= 0) InfoRow(stringResource(R.string.info_latency), "${server.ping.toInt()} ${stringResource(R.string.unit_ms)}")
                    ipv4?.let { InfoRow("IPv4", it) }
                    ipv6?.let { InfoRow("IPv6", it) }
                    lastTest?.let { InfoRow(stringResource(R.string.info_last_test), formatDate(it)) }
                }
            }
            SheetAction(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.sheet_ping),
                subtitle = stringResource(R.string.sheet_ping_hint),
                onClick = onPing
            )
            val shareTitle = stringResource(R.string.sheet_share)
            val shareText = stringResource(R.string.share_server_text, GeoDistance.cleanName(server.name), server.server)
            SheetAction(
                icon = Icons.Filled.Share,
                title = shareTitle,
                subtitle = stringResource(R.string.sheet_share_hint),
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, shareTitle))
                    } catch (_: Exception) {
                    }
                }
            )
            if (favorite) {
                SheetAction(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.sheet_unfavorite),
                    subtitle = stringResource(R.string.sheet_unfavorite_hint),
                    tint = FavoriteRed,
                    onClick = onToggleFavorite
                )
            }
            if (custom) {
                SheetAction(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.servers_delete),
                    subtitle = stringResource(R.string.sheet_delete_hint),
                    tint = FavoriteRed,
                    onClick = onDelete
                )
            }
        }
    }
}

private val FavoriteRed = Color(0xFFEF4444)

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)?
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
    }
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint == FavoriteRed) tint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
