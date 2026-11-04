package org.librespeed.speedtest.ui.result

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.share.ShareImage
import org.librespeed.speedtest.share.ShareResult

@Composable
fun ShareScreen(entryId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context.applicationContext) }
    val useMBytes by prefs.useMBytes.collectAsStateWithLifecycle(initialValue = false)
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }
    var missing by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entryId) {
        val loaded = withContext(Dispatchers.IO) { HistoryDatabase(context.applicationContext).read(entryId) }
        if (loaded == null) missing = true else entry = loaded
    }
    LaunchedEffect(missing) { if (missing) onBack() }
    LaunchedEffect(entry, useMBytes) {
        entry?.let { loaded ->
            preview = withContext(Dispatchers.Default) { ShareImage.render(context, loaded, useMBytes) }
        }
    }

    val result = entry ?: return

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
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 48.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dialog_close))
            }
            Text(
                text = stringResource(R.string.share_result),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
            preview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(Modifier.padding(top = 16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ShareOption(
                        icon = Icons.Filled.Photo,
                        title = stringResource(R.string.share_image),
                        subtitle = stringResource(R.string.share_image_hint)
                    ) {
                        //rendering and PNG-encoding the card takes a while; keep it off the UI thread
                        scope.launch(Dispatchers.Default) { ShareImage.share(context, result, useMBytes) }
                    }
                    result.shareUrl?.let { url ->
                        OptionDivider()
                        ShareOption(
                            icon = Icons.Filled.Link,
                            title = stringResource(R.string.share_link),
                            subtitle = stringResource(R.string.share_link_hint)
                        ) { ShareResult.shareLink(context, url) }
                    }
                    OptionDivider()
                    ShareOption(
                        icon = Icons.Filled.ContentCopy,
                        title = stringResource(R.string.share_copy),
                        subtitle = stringResource(R.string.share_copy_hint)
                    ) { copyText() }
                }
            }
            Spacer(Modifier.padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun ShareOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
