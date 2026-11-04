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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Packet loss used to be computed right after joining whichever pinger thread
 * was current; when a ping timed out, error recovery replaced that thread and
 * the loss was reported before the retried pings were answered — phantom loss.
 */
class PingPhantomLossTest {

    @Test(timeout = 60_000)
    fun aRetriedPingTimeoutDoesNotReportPhantomLoss() {
        val server = MockWebServer()
        val requests = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                //first ping is never answered: the stream times out and restarts;
                //the retried pings must all be answered and counted
                return if (requests.incrementAndGet() == 1) MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
                else MockResponse().setResponseCode(200)
            }
        }
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val config = SpeedtestConfig().apply {
                test_order = "P"
                count_ping = 3
                ping_connectTimeout = 2000
                ping_soTimeout = 500
            }
            val done = CountDownLatch(1)
            val reportedLoss = AtomicReference<Double?>(null)
            val failure = AtomicReference<String?>(null)
            object : SpeedtestWorker(TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP"), config, TelemetryConfig()) {
                override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = reportedLoss.set(loss)
                override fun onIPInfoUpdate(ipInfo: String?) = Unit
                override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
                override fun onEnd() = done.countDown()
                override fun onCriticalFailure(err: String?) {
                    failure.set(err)
                }
            }
            assertTrue("worker did not finish", done.await(45, TimeUnit.SECONDS))
            assertNull("a retried ping must not fail the test", failure.get())
            assertEquals("pings answered after a retry are not lost", 0.0, reportedLoss.get()!!, 0.0)
        } finally {
            server.shutdown()
        }
    }

}
