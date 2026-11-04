package org.librespeed.speedtest.engine

import android.content.Context
import com.fdossena.speedtest.core.Speedtest
import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.librespeed.speedtest.data.ClientInfo
import java.io.IOException
import kotlin.coroutines.resume

enum class TestMode(val key: String) {
    STANDARD("standard"), SINGLE("single"), STABILITY("stability"), COMPARE("compare");

    companion object {
        fun fromKey(key: String?): TestMode = entries.find { it.key == key } ?: STANDARD
    }
}

class TestEngine(private val context: Context) {

    init {
        //background entry points (the scheduled worker) never pass MainActivity,
        //so the sanitized app UA must be installed before the first connection
        Connection.setUserAgent(ClientInfo.userAgent)
    }

    private var speedtest: Speedtest? = null

    data class Discovery(val servers: List<TestPoint>, val selected: TestPoint?)

    /**
     * Loads the server list (remote first, bundled fallback), pings all servers and picks the best one.
     * [onListLoaded] gets the full list before any pinging starts.
     */
    suspend fun discover(
        customServers: List<TestPoint>,
        onListLoaded: (List<TestPoint>) -> Unit = {}
    ): Discovery = withContext(Dispatchers.IO) {
        val st = newSpeedtest(telemetryEnabled = false)
        customServers.forEach { runCatching { st.addTestPoint(it) } }
        var loaded = false
        val data = readAsset("ServerList.json")?.trim()
        if (data != null) {
            loaded = if (data.startsWith("\"") || data.startsWith("'")) {
                st.loadServerList(data.substring(1, data.length - 1))
            } else {
                runCatching { st.addTestPoints(JSONArray(data)); true }.getOrDefault(false)
            }
        }
        if (!loaded) {
            readAsset("ServerListFallback.json")?.let {
                runCatching { st.addTestPoints(JSONArray(it)); loaded = true }
            }
        }
        if (!loaded && customServers.isEmpty()) throw IOException("Failed to load the server list")
        onListLoaded(st.testPoints.toList())
        val selected = suspendCancellableCoroutine { continuation ->
            st.selectServer(object : Speedtest.ServerSelectedHandler() {
                override fun onServerSelected(server: TestPoint?) {
                    continuation.resume(server)
                }
            })
            continuation.invokeOnCancellation { runCatching { st.abort() } }
        }
        Discovery(st.testPoints.toList(), selected)
    }

    /**
     * Prepares a fresh test run against an already known server, without re-pinging everything.
     * [configOverrides] lets tests shorten the phases; production callers leave it null.
     */
    fun prepare(
        servers: List<TestPoint>,
        selected: TestPoint,
        telemetryEnabled: Boolean,
        mode: TestMode = TestMode.STANDARD,
        configOverrides: JSONObject? = null
    ) {
        val st = newSpeedtest(telemetryEnabled, mode, configOverrides)
        st.addTestPoints(servers.toTypedArray())
        st.setSelectedServer(selected)
        speedtest = st
    }

    fun start(handler: Speedtest.SpeedtestHandler) {
        speedtest?.start(handler)
    }

    fun abort() {
        runCatching { speedtest?.abort() }
    }

    private fun newSpeedtest(
        telemetryEnabled: Boolean,
        mode: TestMode = TestMode.STANDARD,
        configOverrides: JSONObject? = null
    ): Speedtest {
        val st = Speedtest()
        val configJson = runCatching { JSONObject(readAsset("SpeedtestConfig.json") ?: "{}") }.getOrDefault(JSONObject())
        when (mode) {
            TestMode.SINGLE -> {
                configJson.put("dl_parallelStreams", 1)
                configJson.put("ul_parallelStreams", 1)
            }
            TestMode.STABILITY -> {
                //a long sustained download shows how steady the line really is
                configJson.put("test_order", "P_D")
                configJson.put("time_dl_max", 60)
                configJson.put("time_auto", false)
            }
            TestMode.COMPARE -> {
                configJson.put("test_order", "P_D")
                configJson.put("time_dl_max", 5)
                configJson.put("time_auto", false)
            }
            TestMode.STANDARD -> Unit
        }
        configOverrides?.keys()?.forEach { key -> configJson.put(key, configOverrides.get(key)) }
        runCatching { st.setSpeedtestConfig(SpeedtestConfig(configJson)) }
        val telemetryJson = if (telemetryEnabled) {
            JSONObject().put("telemetryLevel", TelemetryConfig.LEVEL_FULL)
        } else {
            readAsset("TelemetryConfig.json")?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
        }
        runCatching { st.setTelemetryConfig(TelemetryConfig(telemetryJson)) }
        return st
    }

    private fun readAsset(name: String): String? = try {
        context.assets.open(name).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }

}
