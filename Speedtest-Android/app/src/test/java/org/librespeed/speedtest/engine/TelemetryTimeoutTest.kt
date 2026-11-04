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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The telemetry connection used to be opened with no timeouts at all and the
 * worker joined the telemetry thread without a bound: a server that accepted
 * the POST but never answered wedged the worker forever, and onEnd never fired.
 */
class TelemetryTimeoutTest {

    @Test(timeout = 120_000)
    fun anUnresponsiveTelemetryServerDoesNotWedgeTheWorker() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("telemetry") -> MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
                    path.contains("empty") -> MockResponse().setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val config = SpeedtestConfig().apply {
                test_order = "P"
                count_ping = 2
                ping_connectTimeout = 2000
                ping_soTimeout = 500
            }
            val telemetry = TelemetryConfig(TelemetryConfig.LEVEL_FULL, base, "/central/telemetry.php", "/central/?id=%s")
            val done = CountDownLatch(1)
            object : SpeedtestWorker(TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP"), config, telemetry) {
                override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = Unit
                override fun onIPInfoUpdate(ipInfo: String?) = Unit
                override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
                override fun onEnd() = done.countDown()
                override fun onCriticalFailure(err: String?) = Unit
            }
            assertTrue("an unanswered telemetry POST must not hang the worker", done.await(60, TimeUnit.SECONDS))
        } finally {
            server.shutdown()
        }
    }

}
