package org.librespeed.speedtest.engine

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fdossena.speedtest.core.Speedtest
import com.fdossena.speedtest.core.serverSelector.TestPoint
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The telemetry contract tested through the real application path:
 * TestEngine reads its bundled config assets and builds the TelemetryConfig,
 * exactly as the app does — not just the engine worker in isolation.
 */
@RunWith(AndroidJUnit4::class)
//robolectric does not support SDK 36 yet
@Config(sdk = [35])
class TestEngineTelemetryTest {

    //short phases and 1 MB chunks so the suite stays fast; the telemetry path is
    //untouched. The default 20 MB upload bodies pile up in MockWebServer's request
    //recorder and OOM the test JVM, which surfaced as random 360 s hangs.
    private val quickTimes = JSONObject()
        .put("time_dl_max", 2)
        .put("time_ul_max", 2)
        .put("time_auto", false)
        .put("count_ping", 2)
        .put("dl_parallelStreams", 1)
        .put("ul_parallelStreams", 1)
        .put("dl_ckSize", 1)
        .put("ul_ckSize", 1)

    private class Recorded : Dispatcher() {
        val paths = Collections.synchronizedList(mutableListOf<String>())

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: ""
            paths.add(path)
            return when {
                path.contains("garbage") -> MockResponse().setBody(Buffer().write(ByteArray(65536)))
                path.contains("empty") -> MockResponse().setResponseCode(200)
                path.contains("getIP") -> MockResponse().setBody("127.0.0.1 - Test ISP\n")
                path.contains("telemetry") -> MockResponse().setBody("id 1234\n")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private class NoOpHandler(private val done: CountDownLatch) : Speedtest.SpeedtestHandler() {
        override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
        override fun onUploadUpdate(ul: Double, progress: Double) = Unit
        override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
        override fun onLossUpdate(loss: Double) = Unit
        override fun onIPInfoUpdate(ipInfo: String?) = Unit
        override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
        override fun onEnd() = done.countDown()
        override fun onCriticalFailure(err: String?) = done.countDown()
    }

    private fun runThroughEngine(telemetryEnabled: Boolean): List<String> {
        val server = MockWebServer()
        //recorded requests are held until shutdown; keep only enough to assert on
        server.bodyLimit = 64 * 1024L
        val dispatcher = Recorded()
        server.dispatcher = dispatcher
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val testPoint = TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP")
            val engine = TestEngine(ApplicationProvider.getApplicationContext())
            engine.prepare(listOf(testPoint), testPoint, telemetryEnabled, configOverrides = quickTimes)
            val done = CountDownLatch(1)
            engine.start(NoOpHandler(done))
            assertTrue("engine did not finish in time", done.await(360, TimeUnit.SECONDS))
            return dispatcher.paths.toList()
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `the app default sends no telemetry`() {
        val paths = runThroughEngine(telemetryEnabled = false)
        assertEquals(emptyList<String>(), paths.filter { it.contains("telemetry") })
    }

    @Test
    fun `enabling the switch submits exactly once to the tested server`() {
        val paths = runThroughEngine(telemetryEnabled = true)
        val telemetry = paths.filter { it.contains("telemetry") }
        assertEquals(1, telemetry.size)
        assertTrue(telemetry.single().contains("/results/telemetry.php"))
    }

    @Test
    fun `abort before start is safe and the engine can run afterwards`() {
        val server = MockWebServer()
        server.bodyLimit = 64 * 1024L
        server.dispatcher = Recorded()
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val testPoint = TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP")
            val engine = TestEngine(ApplicationProvider.getApplicationContext())
            engine.abort() //nothing prepared yet — must not throw
            engine.prepare(listOf(testPoint), testPoint, telemetryEnabled = false, configOverrides = quickTimes)
            val done = CountDownLatch(1)
            engine.start(NoOpHandler(done))
            assertTrue(done.await(360, TimeUnit.SECONDS))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `abort during the run finishes promptly`() {
        val server = MockWebServer()
        server.bodyLimit = 64 * 1024L
        server.dispatcher = Recorded()
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val testPoint = TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP")
            val engine = TestEngine(ApplicationProvider.getApplicationContext())
            engine.prepare(listOf(testPoint), testPoint, telemetryEnabled = false, configOverrides = quickTimes)
            val done = CountDownLatch(1)
            engine.start(NoOpHandler(done))
            Thread.sleep(500)
            engine.abort()
            assertTrue("aborted engine must still finish", done.await(120, TimeUnit.SECONDS))
        } finally {
            server.shutdown()
        }
    }

}
