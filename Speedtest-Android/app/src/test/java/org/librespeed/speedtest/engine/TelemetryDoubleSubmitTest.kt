package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The central fallback used to fire whenever no share id could be parsed from
 * the tested server's response — even when that server had accepted (and
 * likely stored) the POST, recording the same run twice. Falling back is only
 * correct when the local submission failed outright.
 */
class TelemetryDoubleSubmitTest {

    private class Recorded(private val localResponse: () -> MockResponse) : Dispatcher() {
        val paths = Collections.synchronizedList(mutableListOf<String>())

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: ""
            paths.add(path)
            return when {
                path.contains("/central/telemetry.php") -> MockResponse().setBody("id 99\n")
                path.contains("/results/telemetry.php") -> localResponse()
                path.contains("empty") -> MockResponse().setResponseCode(200)
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun runWorker(localResponse: () -> MockResponse): Pair<List<String>, String?> {
        val server = MockWebServer()
        val dispatcher = Recorded(localResponse)
        server.dispatcher = dispatcher
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val config = SpeedtestConfig().apply {
                test_order = "P"
                count_ping = 2
                ping_connectTimeout = 2000
                ping_soTimeout = 2000
            }
            val telemetry = TelemetryConfig(TelemetryConfig.LEVEL_FULL, base, "/central/telemetry.php", "/central/?id=%s")
            val done = CountDownLatch(1)
            val receivedId = AtomicReference<String?>(null)
            object : SpeedtestWorker(TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP"), config, telemetry) {
                override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = Unit
                override fun onIPInfoUpdate(ipInfo: String?) = Unit
                override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = receivedId.set(id)
                override fun onEnd() = done.countDown()
                override fun onCriticalFailure(err: String?) = Unit
            }
            assertTrue("worker did not finish", done.await(60, TimeUnit.SECONDS))
            return dispatcher.paths.toList() to receivedId.get()
        } finally {
            server.shutdown()
        }
    }

    @Test(timeout = 120_000)
    fun acceptedPostWithoutAnIdIsNotResubmittedCentrally() {
        val (paths, id) = runWorker { MockResponse().setBody("stored\n") }
        assertEquals(1, paths.count { it.contains("/results/telemetry.php") })
        assertEquals("the run must not be recorded twice", 0, paths.count { it.contains("/central/") })
        assertNull(id)
    }

    @Test(timeout = 120_000)
    fun transportFailureStillFallsBackToTheCentralServer() {
        val (paths, id) = runWorker { MockResponse().setResponseCode(404) }
        assertEquals(1, paths.count { it.contains("/central/telemetry.php") })
        assertEquals("99", id?.trim())
    }

}
