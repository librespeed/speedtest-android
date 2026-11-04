package org.librespeed.speedtest.ui.speedtest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.librespeed.speedtest.R
import org.librespeed.speedtest.ui.currentLocale
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.NetworkInfo
import org.librespeed.speedtest.ui.HingeBounds
import org.librespeed.speedtest.ui.components.SpeedGauge
import org.librespeed.speedtest.ui.components.Sparkline
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SpeedtestScreen(
    viewModel: SpeedtestViewModel,
    onServersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onResult: (Long) -> Unit,
    hinge: HingeBounds? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resultId by viewModel.lastResultId.collectAsStateWithLifecycle()
    val unitLabel = stringResource(if (state.useMBytes) R.string.unit_mbytes else R.string.unit_mbps)
    fun display(value: Double): Double = if (state.useMBytes) value / 8 else value

    LaunchedEffect(resultId) {
        resultId?.let {
            viewModel.consumeResult()
            onResult(it)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Libre") }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("Speed") }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        ServerChip(
            state = state,
            onClick = {
                if (state.serverListFailed) viewModel.refreshServers() else onServersClick()
            }
        )

        if (hinge != null) {
            //half-open fold: gauge above the crease, controls below it, nothing on the hinge
            HingeSplit(
                hinge = hinge,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                top = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GaugeSection(state, unitLabel, display = { display(it) })
                    }
                },
                bottom = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MetricsSection(state, viewModel, unitLabel, display = { display(it) })
                    }
                }
            )
        } else {
            Spacer(Modifier.weight(1f))
            GaugeSection(state, unitLabel, display = { display(it) })
            Spacer(Modifier.weight(1f))
            MetricsSection(state, viewModel, unitLabel, display = { display(it) })
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Splits the available space exactly at the fold crease, keeping a small gap around it. */
@Composable
private fun HingeSplit(
    hinge: HingeBounds,
    modifier: Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit
) {
    var windowY by remember { mutableIntStateOf(0) }
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    Layout(
        contents = listOf(top, bottom),
        modifier = modifier.onGloballyPositioned { windowY = it.positionInWindow().y.roundToInt() }
    ) { (topMeasurables, bottomMeasurables), constraints ->
        val height = constraints.maxHeight
        val creaseTop = (hinge.top - windowY - gap).coerceIn(0, height)
        val creaseBottom = (hinge.bottom - windowY + gap).coerceIn(creaseTop, height)
        val topPlaceable = topMeasurables.first()
            .measure(Constraints.fixed(constraints.maxWidth, creaseTop))
        val bottomPlaceable = bottomMeasurables.first()
            .measure(Constraints.fixed(constraints.maxWidth, height - creaseBottom))
        layout(constraints.maxWidth, height) {
            topPlaceable.place(0, 0)
            bottomPlaceable.place(0, creaseBottom)
        }
    }
}

@Composable
private fun GaugeSection(state: SpeedtestUiState, unitLabel: String, display: (Double) -> Double) {
    SpeedGauge(
        speed = state.currentSpeed,
        modifier = Modifier.widthIn(max = 384.dp).fillMaxWidth(),
        useMBytes = state.useMBytes
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (state.phase == Phase.IDLE || state.phase == Phase.ERROR) "—"
                else String.format(currentLocale, "%.2f", display(state.currentSpeed)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            PhaseLabel(state)
        }
    }
}

@Composable
private fun MetricsSection(
    state: SpeedtestUiState,
    viewModel: SpeedtestViewModel,
    unitLabel: String,
    display: (Double) -> Double
) {
        val accents = LocalSpeedAccents.current
        Row(modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.test_download),
                icon = { Icon(Icons.Filled.ArrowDownward, null, tint = accents.download, modifier = Modifier.size(16.dp)) },
                value = if (state.download < 0) state.download else display(state.download),
                unit = unitLabel,
                accent = accents.download,
                samples = state.downloadSamples
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.test_upload),
                icon = { Icon(Icons.Filled.ArrowUpward, null, tint = accents.upload, modifier = Modifier.size(16.dp)) },
                value = if (state.upload < 0) state.upload else display(state.upload),
                unit = unitLabel,
                accent = accents.upload,
                samples = state.uploadSamples
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetric(Modifier.weight(1f), stringResource(R.string.test_ping), state.ping, stringResource(R.string.unit_ms))
            SmallMetric(Modifier.weight(1f), stringResource(R.string.test_jitter), state.jitter, stringResource(R.string.unit_ms))
            SmallMetric(Modifier.weight(1f), stringResource(R.string.test_loss), state.loss, stringResource(R.string.unit_percent))
        }

        Spacer(Modifier.height(14.dp))
        val running = state.phase in setOf(Phase.PING, Phase.DOWNLOAD, Phase.UPLOAD)
        val context = LocalContext.current
        val prefs = remember { AppPreferences(context.applicationContext) }
        val askedPhoneState by prefs.askedPhoneState.collectAsStateWithLifecycle(initialValue = true)
        val scope = rememberCoroutineScope()
        val phoneStateLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { viewModel.startOrStop() }
        OutlinedButton(
            onClick = {
                //ask so mobile tests can label 4G/5G; the test starts either way and we
                //keep offering the dialog for as long as the system still shows it
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
                val rationale = (context as? android.app.Activity)
                    ?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_PHONE_STATE) }
                    ?: false
                val needsPrompt = !running && !granted && NetworkInfo.isCellular(context) &&
                    (!askedPhoneState || rationale)
                if (needsPrompt) {
                    scope.launch { prefs.markPhoneStateAsked() }
                    phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                } else {
                    viewModel.startOrStop()
                }
            },
            enabled = state.selectedServer != null || state.phase != Phase.IDLE,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth().height(52.dp)
        ) {
            Icon(
                imageVector = if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    when {
                        running -> R.string.test_stop
                        state.phase == Phase.ERROR -> R.string.test_restart
                        else -> R.string.test_start
                    }
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.phase == Phase.ERROR) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.error ?: stringResource(R.string.test_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
}

@Composable
private fun ServerChip(state: SpeedtestUiState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                when {
                    //a remembered server is shown straight away; the spinner on the
                    //right says the rest of the list is still being pinged
                    state.selectingServers && state.selectedServer == null -> Text(
                        text = stringResource(R.string.servers_selecting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.serverListFailed -> Text(
                        text = stringResource(R.string.servers_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    state.selectedServer != null -> {
                        Text(
                            text = stringResource(
                                if (state.pinnedServer) R.string.server_selected else R.string.server_auto_select
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.selectedServer.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> Text(
                        text = stringResource(R.string.servers_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (state.selectingServers) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (state.serverListFailed) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.selectedServer?.takeIf { it.ping >= 0 }?.let {
                    Text(
                        text = "${it.ping.toInt()} ${stringResource(R.string.unit_ms)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhaseLabel(state: SpeedtestUiState) {
    val text = when (state.phase) {
        Phase.PING -> stringResource(R.string.phase_ping)
        Phase.DOWNLOAD -> stringResource(R.string.phase_download)
        Phase.UPLOAD -> stringResource(R.string.phase_upload)
        else -> ""
    }
    if (text.isNotEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (state.phase) {
                Phase.DOWNLOAD -> Icon(Icons.Filled.ArrowDownward, null, tint = LocalSpeedAccents.current.download, modifier = Modifier.size(20.dp))
                Phase.UPLOAD -> Icon(Icons.Filled.ArrowUpward, null, tint = LocalSpeedAccents.current.upload, modifier = Modifier.size(20.dp))
                else -> Icon(Icons.Filled.NetworkPing, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    icon: @Composable () -> Unit,
    value: Double,
    unit: String,
    accent: Color,
    samples: List<Double>
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (value < 0) "—" else String.format(currentLocale, "%.2f", value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Sparkline(
                data = samples,
                color = accent,
                modifier = Modifier.fillMaxWidth().height(30.dp).padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SmallMetric(modifier: Modifier, title: String, value: Double, unit: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (value < 0) "—" else String.format(currentLocale, "%.1f", value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    modifier = Modifier.padding(bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
