package org.librespeed.speedtest.ui.speedtest

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fdossena.speedtest.core.Speedtest
import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.ping.Pinger
import com.fdossena.speedtest.core.serverSelector.ServerSelector
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.CustomServerFactory
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.data.NetworkInfo
import org.librespeed.speedtest.data.key
import org.librespeed.speedtest.engine.TestEngine
import org.librespeed.speedtest.engine.TestMode
import java.util.Collections
import kotlin.math.roundToInt

enum class Phase { IDLE, PING, DOWNLOAD, UPLOAD, ERROR }

data class SpeedtestUiState(
    val phase: Phase = Phase.IDLE,
    val selectingServers: Boolean = false,
    val serverListFailed: Boolean = false,
    val servers: List<TestPoint> = emptyList(),
    val selectedServer: TestPoint? = null,
    val pinnedServer: Boolean = false,
    val currentSpeed: Double = 0.0,
    val download: Double = -1.0,
    val upload: Double = -1.0,
    val ping: Double = -1.0,
    val jitter: Double = -1.0,
    val loss: Double = -1.0,
    val downloadSamples: List<Double> = emptyList(),
    val uploadSamples: List<Double> = emptyList(),
    val ipInfo: String? = null,
    val shareUrl: String? = null,
    val error: String? = null,
    val favorites: Set<String> = emptySet(),
    val customKeys: Set<String> = emptySet(),
    val useMBytes: Boolean = false,
    val distances: Map<String, Int> = emptyMap()
)

class SpeedtestViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TestEngine(application)
    private val prefs = AppPreferences(application)
    private val history = HistoryDatabase(application)
    private val _state = MutableStateFlow(SpeedtestUiState())
    val state: StateFlow<SpeedtestUiState> = _state

    /** Id of the freshly saved result; the UI navigates to it and calls [consumeResult]. */
    val lastResultId = MutableStateFlow<Long?>(null)

    private var telemetryEnabled = false
    private var testMode = "standard"
    private var autoSelected: TestPoint? = null
    private var discoveryJob: Job? = null

    //written by the UI thread (stop) and the engine's worker/stream threads
    @Volatile
    private var aborted = false
    @Volatile
    private var failed = false

    //side channel measuring latency while the line is under load (bufferbloat)
    @Volatile
    private var loadedPinger: Pinger? = null
    private val loadedDownSamples = Collections.synchronizedList(mutableListOf<Double>())
    private val loadedUpSamples = Collections.synchronizedList(mutableListOf<Double>())

    init {
        viewModelScope.launch {
            prefs.favorites.collect { favorites -> _state.update { it.copy(favorites = favorites) } }
        }
        viewModelScope.launch {
            prefs.useMBytes.collect { useMBytes -> _state.update { it.copy(useMBytes = useMBytes) } }
        }
        viewModelScope.launch { prefs.telemetryEnabled.collect { telemetryEnabled = it } }
        viewModelScope.launch { prefs.testMode.collect { testMode = it } }
        refreshServers()
    }

    fun consumeResult() {
        lastResultId.value = null
    }

    fun refreshServers() {
        if (_state.value.selectingServers) return
        _state.update { it.copy(selectingServers = true, serverListFailed = false) }
        discoveryJob = viewModelScope.launch {
            try {
                val custom = prefs.customServers.first()
                val rememberedKey = prefs.rememberedServer.first()
                val discovery = engine.discover(custom) { servers ->
                    //a remembered server is usable as soon as the list is in; pinging
                    //everything only refines the automatic choice, so Start need not wait
                    val match = rememberedKey?.let { key -> servers.find { it.key() == key } } ?: return@discover
                    _state.update { it.copy(servers = servers, selectedServer = match, pinnedServer = true) }
                }
                autoSelected = discovery.selected
                val remembered = rememberedKey
                    ?.let { key -> discovery.servers.find { it.key() == key && it.ping >= 0 } }
                _state.update {
                    it.copy(
                        selectingServers = false,
                        servers = discovery.servers,
                        //a test already running against the remembered server keeps its label
                        selectedServer = if (isRunning()) it.selectedServer else remembered ?: discovery.selected,
                        pinnedServer = remembered != null,
                        customKeys = custom.map { c -> c.key() }.toSet(),
                        serverListFailed = discovery.selected == null && discovery.servers.isEmpty()
                    )
                }
            } catch (_: CancellationException) {
                //a test started on the remembered server; the rest of the list stays unpinged
                _state.update { it.copy(selectingServers = false) }
            } catch (_: Exception) {
                _state.update { it.copy(selectingServers = false, serverListFailed = true) }
            }
            computeDistances()
        }
    }

    /** Fills in distances to servers when coarse location is granted; results stay on the device. */
    fun computeDistances() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) return@launch
            val location = lastKnownLocation(app) ?: return@launch
            if (!Geocoder.isPresent()) return@launch
            val geocoder = Geocoder(app)
            val cache = prefs.getGeoCache().toMutableMap()
            for (server in _state.value.servers) {
                val place = GeoDistance.cleanName(server.name)
                val coordinates = cache[place] ?: try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(place, 1)?.firstOrNull()
                        ?.let { it.latitude to it.longitude }
                        ?.also {
                            cache[place] = it
                            prefs.putGeoCache(place, it.first, it.second)
                        }
                } catch (_: Exception) {
                    null
                } ?: continue
                val km = GeoDistance.km(location.latitude, location.longitude, coordinates.first, coordinates.second)
                _state.update {
                    it.copy(distances = it.distances + (server.key() to km.roundToInt()))
                }
            }
        }
    }

    //only called after computeDistances verified ACCESS_COARSE_LOCATION
    @android.annotation.SuppressLint("MissingPermission")
    private fun lastKnownLocation(app: Application): Location? = try {
        val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    } catch (_: Exception) {
        null
    }

    fun selectServer(testPoint: TestPoint) {
        if (isRunning()) return
        _state.update { it.copy(selectedServer = testPoint, pinnedServer = true) }
        viewModelScope.launch { prefs.setRememberedServer(testPoint.key()) }
    }

    /** Returns to automatic server selection. */
    fun useAutoSelect() {
        if (isRunning()) return
        _state.update { it.copy(selectedServer = autoSelected ?: it.selectedServer, pinnedServer = false) }
        viewModelScope.launch { prefs.setRememberedServer(null) }
    }

    fun toggleFavorite(testPoint: TestPoint) {
        viewModelScope.launch { prefs.toggleFavorite(testPoint.key()) }
    }

    /** Re-pings a single server and refreshes the list when done. */
    fun pingServer(testPoint: TestPoint) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                object : ServerSelector(arrayOf(testPoint), 2000) {
                    override fun onServerSelected(server: TestPoint?) {
                        _state.update { it.copy(servers = it.servers.toList()) }
                    }
                }.start()
            } catch (_: Exception) {
            }
        }
    }

    /** Returns false when the URL is invalid. */
    fun addCustomServer(name: String, url: String): Boolean {
        val testPoint = try {
            CustomServerFactory.create(name, url)
        } catch (_: Exception) {
            return false
        }
        viewModelScope.launch {
            prefs.addCustomServer(testPoint)
            refreshServers()
        }
        return true
    }

    fun removeCustomServer(testPoint: TestPoint) {
        viewModelScope.launch {
            prefs.removeCustomServer(testPoint.key())
            refreshServers()
        }
    }

    fun startOrStop() {
        if (isRunning()) stop() else start()
    }

    private fun isRunning(): Boolean = _state.value.phase in setOf(Phase.PING, Phase.DOWNLOAD, Phase.UPLOAD)

    private fun startLoadedPinger(server: TestPoint) {
        if (loadedPinger != null) return
        loadedPinger = try {
            object : Pinger(Connection(server.server), server.pingURL) {
                override fun onPong(ns: Long): Boolean {
                    val ms = ns / 1_000_000.0
                    when (_state.value.phase) {
                        Phase.DOWNLOAD -> loadedDownSamples.add(ms)
                        Phase.UPLOAD -> loadedUpSamples.add(ms)
                        else -> Unit
                    }
                    return true
                }

                override fun onError(err: String?) = Unit
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stopLoadedPinger() {
        runCatching { loadedPinger?.stopASAP() }
        loadedPinger = null
    }

    private fun start() {
        val current = _state.value
        val server = current.selectedServer ?: return
        //pinging the rest of the list would compete with the measurement
        if (current.selectingServers) discoveryJob?.cancel()
        aborted = false
        failed = false
        val networkType = NetworkInfo.describe(getApplication())
        val networkDetail = NetworkInfo.detail(getApplication())
        val mode = TestMode.fromKey(testMode)
        val telemetry = telemetryEnabled
        loadedDownSamples.clear()
        loadedUpSamples.clear()
        val startedAt = System.currentTimeMillis()
        _state.update {
            it.copy(
                phase = Phase.PING,
                currentSpeed = 0.0,
                download = -1.0, upload = -1.0, ping = -1.0, jitter = -1.0, loss = -1.0,
                downloadSamples = emptyList(), uploadSamples = emptyList(),
                ipInfo = null, shareUrl = null, error = null
            )
        }
        engine.prepare(current.servers, server, telemetry, mode)
        engine.start(object : Speedtest.SpeedtestHandler() {
            override fun onDownloadUpdate(dl: Double, progress: Double) {
                startLoadedPinger(server)
                _state.update {
                    it.copy(
                        phase = Phase.DOWNLOAD,
                        currentSpeed = dl,
                        download = dl,
                        downloadSamples = if (progress > 0) it.downloadSamples + dl else it.downloadSamples
                    )
                }
            }

            override fun onUploadUpdate(ul: Double, progress: Double) {
                startLoadedPinger(server)
                _state.update {
                    it.copy(
                        phase = Phase.UPLOAD,
                        currentSpeed = ul,
                        upload = ul,
                        uploadSamples = if (progress > 0) it.uploadSamples + ul else it.uploadSamples
                    )
                }
            }

            override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) {
                _state.update { it.copy(phase = Phase.PING, ping = ping, jitter = jitter) }
            }

            override fun onLossUpdate(loss: Double) {
                _state.update { it.copy(loss = loss) }
            }

            override fun onIPInfoUpdate(ipInfo: String?) {
                _state.update { it.copy(ipInfo = ipInfo) }
            }

            override fun onTestIDReceived(id: String?, shareURL: String?) {
                _state.update { it.copy(shareUrl = shareURL) }
            }

            override fun onEnd() {
                stopLoadedPinger()
                //the engine always fires onEnd, even after onCriticalFailure already
                //reported the run broken; a failed run keeps its error on screen and
                //is never saved as a result
                if (failed) return
                val finished = _state.value
                if (aborted || finished.download < 0) {
                    resetToIdle()
                    return
                }
                viewModelScope.launch {
                    val entryId = withContext(Dispatchers.IO) {
                        history.insert(
                            HistoryEntry(
                                date = System.currentTimeMillis(),
                                server = server.name,
                                ping = finished.ping,
                                jitter = finished.jitter,
                                download = finished.download,
                                upload = finished.upload,
                                loss = finished.loss,
                                ipInfo = finished.ipInfo,
                                ipVersion = server.ipVersion,
                                shareUrl = finished.shareUrl,
                                networkType = networkType,
                                downloadSamples = finished.downloadSamples,
                                uploadSamples = finished.uploadSamples,
                                durationMs = System.currentTimeMillis() - startedAt,
                                mode = mode.key,
                                loadedDown = loadedDownSamples.toList().average().takeIf { !it.isNaN() } ?: -1.0,
                                loadedUp = loadedUpSamples.toList().average().takeIf { !it.isNaN() } ?: -1.0,
                                networkDetail = networkDetail,
                                telemetrySent = telemetry
                            )
                        )
                    }
                    resetToIdle()
                    lastResultId.value = entryId
                }
            }

            override fun onCriticalFailure(err: String?) {
                failed = true
                stopLoadedPinger()
                _state.update { it.copy(phase = Phase.ERROR, error = err, currentSpeed = 0.0) }
            }
        })
    }

    private fun resetToIdle() {
        _state.update {
            it.copy(
                phase = Phase.IDLE,
                currentSpeed = 0.0,
                download = -1.0, upload = -1.0, ping = -1.0, jitter = -1.0, loss = -1.0,
                downloadSamples = emptyList(), uploadSamples = emptyList(),
                ipInfo = null, shareUrl = null, error = null
            )
        }
    }

    /** Selects the server with the given name (without pinning it) and starts a test. */
    fun testAgain(serverName: String) {
        if (isRunning()) return
        _state.value.servers.find { it.name == serverName }?.let { server ->
            _state.update { it.copy(selectedServer = server) }
        }
        startOrStop()
    }

    private fun stop() {
        aborted = true
        stopLoadedPinger()
        engine.abort()
    }

    override fun onCleared() {
        stopLoadedPinger()
        engine.abort()
    }

}
