package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * An aborted test transmits nothing, regardless of telemetry level: the
 * partial results, IP/ISP blob and log must never leave the device for a run
 * the user backed out of.
 */
class AbortedTestTelemetryTest {

    @Test(timeout = 60_000)
    fun anAbortedTestSendsNoTelemetry() {
        val server = MockWebServer()
        val paths = Collections.synchronizedList(mutableListOf<String>())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                paths.add(request.path ?: "")
                //pings hang, so the test is reliably still running when aborted
                return MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
            }
        }
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val config = SpeedtestConfig().apply {
                test_order = "P"
                count_ping = 2
                ping_connectTimeout = 2000
                ping_soTimeout = 10000
            }
            val telemetry = TelemetryConfig(TelemetryConfig.LEVEL_FULL, base, "/central/telemetry.php", "/central/?id=%s")
            val done = CountDownLatch(1)
            val worker = object : SpeedtestWorker(TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP"), config, telemetry) {
                override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = Unit
                override fun onIPInfoUpdate(ipInfo: String?) = Unit
                override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
                override fun onEnd() = done.countDown()
                override fun onCriticalFailure(err: String?) = Unit
            }
            Thread.sleep(300)
            worker.abort()
            assertTrue("aborted worker must still end", done.await(30, TimeUnit.SECONDS))
            assertEquals(
                "an aborted test must transmit nothing",
                emptyList<String>(), paths.filter { it.contains("telemetry") }
            )
        } finally {
            server.shutdown()
        }
    }

}
