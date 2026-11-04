package org.librespeed.speedtest.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.ClientInfo

private const val PROJECT_URL = "https://github.com/librespeed/speedtest-android"

private data class Language(val tag: String, val label: String)

//labels stay in their own language on purpose; add new locales here and in locales_config.xml
private val LANGUAGES = listOf(
    Language("en", "English"),
    Language("cs", "Čeština")
)

@Composable
fun SettingsScreen(
    serverLabel: String?,
    serverPinned: Boolean,
    serverCount: Int,
    onServersClick: () -> Unit,
    onLicensesClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    val useMBytes by prefs.useMBytes.collectAsStateWithLifecycle(initialValue = false)
    val telemetry by prefs.telemetryEnabled.collectAsStateWithLifecycle(initialValue = false)
    val testMode by prefs.testMode.collectAsStateWithLifecycle(initialValue = "standard")
    val scheduledTests by prefs.scheduledTests.collectAsStateWithLifecycle(initialValue = "off")
    val notificationLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    var unitsDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var scheduledDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var testModeDialog by remember { mutableStateOf(false) }
    var whatIsSentDialog by remember { mutableStateOf(false) }
    var reportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        SectionTitle(stringResource(R.string.settings_section_general))
        SettingsCard {
            ValueRow(
                title = stringResource(R.string.settings_server),
                value = when {
                    serverPinned && serverLabel != null -> serverLabel
                    else -> stringResource(R.string.server_auto_select)
                },
                onClick = onServersClick
            )
            RowDivider()
            ValueRow(
                title = stringResource(R.string.settings_units),
                value = stringResource(if (useMBytes) R.string.unit_mbytes else R.string.unit_mbps),
                onClick = { unitsDialog = true }
            )
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                RowDivider()
                val localeManager = remember {
                    context.getSystemService(android.app.LocaleManager::class.java)
                }
                val current = localeManager?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
                ValueRow(
                    title = stringResource(R.string.settings_language),
                    value = LANGUAGES.find { it.tag == current }?.label
                        ?: stringResource(R.string.language_system),
                    onClick = { languageDialog = true }
                )
            }
            RowDivider()
            ValueRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(
                    when (themeMode) {
                        "light" -> R.string.theme_light
                        "dark" -> R.string.theme_dark
                        else -> R.string.theme_system
                    }
                ),
                onClick = { themeDialog = true }
            )
            RowDivider()
            ValueRow(
                title = stringResource(R.string.settings_scheduled),
                value = stringResource(
                    when (scheduledTests) {
                        "6h" -> R.string.scheduled_6h
                        "daily" -> R.string.scheduled_daily
                        "weekly" -> R.string.scheduled_weekly
                        else -> R.string.scheduled_off
                    }
                ),
                onClick = { scheduledDialog = true }
            )
            RowDivider()
            ValueRow(
                title = stringResource(R.string.settings_test_mode),
                value = stringResource(
                    when (testMode) {
                        "single" -> R.string.test_mode_single
                        "stability" -> R.string.test_mode_stability
                        else -> R.string.test_mode_standard
                    }
                ),
                onClick = { testModeDialog = true }
            )
        }
        SectionTitle(stringResource(R.string.settings_section_telemetry))
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_telemetry),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_telemetry_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = telemetry,
                    onCheckedChange = { checked -> scope.launch { prefs.setTelemetryEnabled(checked) } }
                )
            }
            Text(
                text = stringResource(R.string.settings_whats_sent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = { whatIsSentDialog = true }, role = Role.Button)
                    .padding(top = 10.dp, bottom = 2.dp)
            )
        }
        SectionTitle(stringResource(R.string.settings_section_diagnostics))
        SettingsCard {
            ValueRow(
                title = stringResource(R.string.settings_report),
                value = stringResource(R.string.settings_report_hint),
                onClick = { reportDialog = true }
            )
        }
        SectionTitle(stringResource(R.string.settings_section_about))
        SettingsCard {
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = ClientInfo.client,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RowDivider()
            NavRow(stringResource(R.string.settings_report_issue)) { context.openUrl("$PROJECT_URL/issues") }
            RowDivider()
            NavRow(stringResource(R.string.settings_privacy)) { context.openUrl("$PROJECT_URL/blob/master/PRIVACY.md") }
            RowDivider()
            NavRow(stringResource(R.string.settings_licenses), onLicensesClick)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (unitsDialog) {
        RadioDialog(
            title = stringResource(R.string.settings_units),
            options = listOf(
                stringResource(R.string.unit_mbps_long) to !useMBytes,
                stringResource(R.string.unit_mbytes_long) to useMBytes
            ),
            hint = stringResource(R.string.settings_units_hint),
            onSelect = { index -> scope.launch { prefs.setUseMBytes(index == 1) } },
            onDismiss = { unitsDialog = false }
        )
    }
    if (languageDialog && android.os.Build.VERSION.SDK_INT >= 33) {
        val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
        val current = localeManager?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
        val options = listOf(null to stringResource(R.string.language_system)) +
            LANGUAGES.map { it.tag to it.label }
        RadioDialog(
            title = stringResource(R.string.settings_language),
            options = options.map { (tag, label) -> label to (current == tag) },
            hint = null,
            onSelect = { index ->
                localeManager?.applicationLocales = options[index].first
                    ?.let { android.os.LocaleList.forLanguageTags(it) }
                    ?: android.os.LocaleList.getEmptyLocaleList()
            },
            onDismiss = { languageDialog = false }
        )
    }
    if (scheduledDialog) {
        RadioDialog(
            title = stringResource(R.string.settings_scheduled),
            options = listOf(
                stringResource(R.string.scheduled_off) to (scheduledTests == "off"),
                stringResource(R.string.scheduled_6h) to (scheduledTests == "6h"),
                stringResource(R.string.scheduled_daily) to (scheduledTests == "daily"),
                stringResource(R.string.scheduled_weekly) to (scheduledTests == "weekly")
            ),
            hint = stringResource(R.string.scheduled_hint),
            onSelect = { index ->
                val mode = listOf("off", "6h", "daily", "weekly")[index]
                scope.launch { prefs.setScheduledTests(mode) }
                org.librespeed.speedtest.work.ScheduledTests.apply(context.applicationContext, mode)
                if (mode != "off" && android.os.Build.VERSION.SDK_INT >= 33) {
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { scheduledDialog = false }
        )
    }
    if (themeDialog) {
        RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf(
                stringResource(R.string.theme_system) to (themeMode == "system"),
                stringResource(R.string.theme_light) to (themeMode == "light"),
                stringResource(R.string.theme_dark) to (themeMode == "dark")
            ),
            hint = null,
            onSelect = { index ->
                scope.launch { prefs.setThemeMode(listOf("system", "light", "dark")[index]) }
            },
            onDismiss = { themeDialog = false }
        )
    }
    if (testModeDialog) {
        RadioDialog(
            title = stringResource(R.string.settings_test_mode),
            options = listOf(
                stringResource(R.string.test_mode_standard) to (testMode == "standard"),
                stringResource(R.string.test_mode_single) to (testMode == "single"),
                stringResource(R.string.test_mode_stability) to (testMode == "stability")
            ),
            hint = stringResource(R.string.test_mode_hint),
            onSelect = { index ->
                scope.launch { prefs.setTestMode(listOf("standard", "single", "stability")[index]) }
            },
            onDismiss = { testModeDialog = false }
        )
    }
    if (reportDialog) {
        val report = remember {
            var last: org.librespeed.speedtest.data.HistoryEntry? = null
            try {
                last = org.librespeed.speedtest.data.HistoryDatabase(context.applicationContext).readAll().firstOrNull()
            } catch (_: Exception) {
            }
            org.librespeed.speedtest.data.DiagnosticReport.build(
                context, last,
                telemetryEnabled = telemetry, testMode = testMode, serverCount = serverCount
            )
        }
        AlertDialog(
            onDismissRequest = { reportDialog = false },
            title = { Text(stringResource(R.string.settings_report)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.settings_report_send),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(report, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        } catch (_: Exception) {
                        }
                        reportDialog = false
                    }) { Text(stringResource(R.string.report_share)) }
                    TextButton(onClick = {
                        org.librespeed.speedtest.share.ShareResult.copy(context, report)
                        reportDialog = false
                    }) { Text(stringResource(R.string.share_copy)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { reportDialog = false }) { Text(stringResource(R.string.dialog_close)) }
            }
        )
    }
    if (whatIsSentDialog) {
        AlertDialog(
            onDismissRequest = { whatIsSentDialog = false },
            title = { Text(stringResource(R.string.settings_whats_sent)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_telemetry_detail))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_telemetry_detail_off),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { whatIsSentDialog = false }) { Text(stringResource(R.string.dialog_close)) }
            }
        )
    }
}

private fun android.content.Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: Exception) {
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RadioDialog(
    title: String,
    options: List<Pair<String, Boolean>>,
    hint: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, (label, selected) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelect(index)
                            onDismiss()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = {
                            onSelect(index)
                            onDismiss()
                        })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
        }
    )
}
