package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression tests for the privacy contract: with telemetry disabled the engine
 * must never call a telemetry endpoint; with telemetry enabled it reports to the
 * tested server exactly once.
 */
class TelemetryPrivacyTest {

    private fun quickConfig() = SpeedtestConfig(
        JSONObject()
            .put("test_order", "IP_D_U")
            .put("time_dl_max", 1)
            .put("time_ul_max", 1)
            .put("time_auto", false)
            .put("count_ping", 1)
            .put("dl_parallelStreams", 1)
            .put("ul_parallelStreams", 1)
    )

    private fun runWorker(server: MockWebServer, telemetry: TelemetryConfig): List<String> {
        val paths = Collections.synchronizedList(mutableListOf<String>())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                paths.add(path)
                return when {
                    path.contains("garbage") -> MockResponse().setBody(Buffer().write(ByteArray(65536)))
                    path.contains("empty") -> MockResponse().setResponseCode(200)
                    path.contains("getIP") -> MockResponse().setBody("127.0.0.1 - Test ISP")
                    //the engine reads the id line by line, so it needs the newline
                    path.contains("telemetry") -> MockResponse().setBody("id 1234\n")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val base = server.url("/").toString().trimEnd('/')
        val testPoint = TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP")
        val done = CountDownLatch(1)
        val worker = object : SpeedtestWorker(testPoint, quickConfig(), telemetry) {
            override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
            override fun onUploadUpdate(ul: Double, progress: Double) = Unit
            override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
            override fun onLossUpdate(loss: Double) = Unit
            override fun onIPInfoUpdate(ipInfo: String?) = Unit
            override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
            override fun onEnd() = done.countDown()
            override fun onCriticalFailure(err: String?) = done.countDown()
        }
        assertTrue("worker did not finish in time", done.await(60, TimeUnit.SECONDS))
        worker.abort()
        return paths.toList()
    }

    @Test
    fun disabledTelemetrySendsNothing() {
        val server = MockWebServer()
        server.start()
        try {
            val paths = runWorker(server, TelemetryConfig())
            assertEquals(
                "no telemetry request may leave the device when telemetry is off",
                emptyList<String>(),
                paths.filter { it.contains("telemetry") }
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun enabledTelemetryReportsToTestedServerOnce() {
        val server = MockWebServer()
        server.start()
        try {
            val telemetry = TelemetryConfig(TelemetryConfig.LEVEL_FULL, null, null, null)
            val paths = runWorker(server, telemetry)
            val telemetryCalls = paths.filter { it.contains("telemetry") }
            assertEquals("exactly one telemetry submission expected", 1, telemetryCalls.size)
            assertTrue(
                "telemetry must go to the tested server results backend",
                telemetryCalls.single().contains("/results/telemetry.php")
            )
        } finally {
            server.shutdown()
        }
    }

}
