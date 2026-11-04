package org.librespeed.speedtest.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.librespeed.speedtest.R

data class Library(val name: String, val license: String, val url: String)

private val ENGINE = Library(
    "LibreSpeed speedtest engine", "LGPL-3.0", "https://github.com/librespeed/speedtest-android"
)

/** Reads the licensee-generated report bundled as an asset; the engine is in-tree, so it is added by hand. */
internal fun loadLibraries(context: Context): List<Library> {
    val fromReport = try {
        val json = context.assets.open("licenses.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val artifact = array.getJSONObject(index)
            val licenses = artifact.optJSONArray("spdxLicenses") ?: return@mapNotNull null
            if (licenses.length() == 0) return@mapNotNull null
            val first = licenses.getJSONObject(0)
            Library(
                name = artifact.optString("name").ifEmpty { artifact.getString("artifactId") },
                license = first.optString("identifier"),
                url = artifact.optJSONObject("scm")?.optString("url")?.takeIf { it.isNotEmpty() }
                    ?: first.optString("url")
            )
        }.distinctBy { it.name }.sortedBy { it.name.lowercase() }
    } catch (_: Exception) {
        emptyList()
    }
    return listOf(ENGINE) + fromReport
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var libraries by remember { mutableStateOf(listOf(ENGINE)) }

    LaunchedEffect(Unit) {
        libraries = withContext(Dispatchers.IO) { loadLibraries(context.applicationContext) }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
            Text(
                text = stringResource(R.string.settings_licenses),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        libraries.forEachIndexed { index, library ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = library.url.isNotEmpty()) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, library.url.toUri()))
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = library.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = library.license,
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
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
