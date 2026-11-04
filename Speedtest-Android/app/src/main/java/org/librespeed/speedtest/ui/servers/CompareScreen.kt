package org.librespeed.speedtest.ui.servers

import android.app.Application
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.fdossena.speedtest.core.Speedtest
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.currentLocale
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.key
import org.librespeed.speedtest.engine.TestEngine
import org.librespeed.speedtest.engine.TestMode
import org.librespeed.speedtest.ui.speedtest.SpeedtestViewModel
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents
import java.util.Locale
import kotlin.coroutines.resume

data class CompareRow(
    val server: TestPoint,
    val download: Double? = null,
    val running: Boolean = false
)

data class CompareUiState(
    val rows: List<CompareRow> = emptyList(),
    val running: Boolean = false,
    val finished: Boolean = false
)

class CompareViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state
    private var engine: TestEngine? = null

    /** Short download test against the three fastest reachable servers, one after another. */
    fun run(servers: List<TestPoint>) {
        if (_state.value.running) return
        val top = servers.filter { it.ping >= 0 }.sortedBy { it.ping }.take(3)
        if (top.isEmpty()) return
        _state.value = CompareUiState(rows = top.map { CompareRow(it) }, running = true)
        viewModelScope.launch(Dispatchers.IO) {
            top.forEachIndexed { index, testPoint ->
                updateRow(index) { it.copy(running = true) }
                val download = quickDownload(testPoint)
                updateRow(index) { it.copy(running = false, download = download) }
            }
            _state.update { it.copy(running = false, finished = true) }
        }
    }

    private fun updateRow(index: Int, transform: (CompareRow) -> CompareRow) {
        _state.update { state ->
            state.copy(rows = state.rows.mapIndexed { i, row -> if (i == index) transform(row) else row })
        }
    }

    private suspend fun quickDownload(testPoint: TestPoint): Double =
        suspendCancellableCoroutine { continuation ->
            val testEngine = TestEngine(getApplication())
            engine = testEngine
            testEngine.prepare(listOf(testPoint), testPoint, telemetryEnabled = false, mode = TestMode.COMPARE)
            var last = -1.0
            testEngine.start(object : Speedtest.SpeedtestHandler() {
                override fun onDownloadUpdate(dl: Double, progress: Double) {
                    if (dl > 0) last = dl
                }

                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = Unit
                override fun onIPInfoUpdate(ipInfo: String?) = Unit
                override fun onTestIDReceived(id: String?, shareURL: String?) = Unit

                override fun onEnd() {
                    if (continuation.isActive) continuation.resume(last)
                }

                override fun onCriticalFailure(err: String?) {
                    if (continuation.isActive) continuation.resume(-1.0)
                }
            })
            continuation.invokeOnCancellation { testEngine.abort() }
        }

    override fun onCleared() {
        runCatching { engine?.abort() }
    }

}

@Composable
fun CompareScreen(
    speedtestViewModel: SpeedtestViewModel,
    onBack: () -> Unit,
    compareViewModel: CompareViewModel = viewModel()
) {
    val servers by speedtestViewModel.state.collectAsStateWithLifecycle()
    val state by compareViewModel.state.collectAsStateWithLifecycle()
    val best = state.rows
        .filter { (it.download ?: -1.0) > 0 }
        .maxByOrNull { it.download ?: -1.0 }
        ?.takeIf { state.finished }

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
                text = stringResource(R.string.servers_compare),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.compare_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            val accent = LocalSpeedAccents.current.download
            state.rows.forEach { row ->
                val isBest = best?.server?.key() == row.server.key()
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (isBest) BorderStroke(1.dp, accent.copy(alpha = 0.7f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isBest) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = GeoDistance.cleanName(row.server.name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${GeoDistance.hostLabel(row.server.server)} · ${row.server.ping.toInt()} ${stringResource(R.string.unit_ms)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.finished && row.download != null && row.download > 0) {
                                TextButton(
                                    onClick = {
                                        speedtestViewModel.selectServer(row.server)
                                        onBack()
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(R.string.compare_use), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        when {
                            row.running -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            row.download != null && row.download > 0 -> Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(currentLocale, "%.1f", row.download),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent
                                )
                                Text(
                                    text = stringResource(R.string.unit_mbps),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            row.download != null -> Text(
                                text = stringResource(R.string.servers_unreachable),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { compareViewModel.run(servers.servers) },
                enabled = !state.running && servers.servers.any { it.ping >= 0 },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.compare_run), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
