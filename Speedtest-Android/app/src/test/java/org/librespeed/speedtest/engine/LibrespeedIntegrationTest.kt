package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runs the whole engine against a real LibreSpeed backend. Skipped unless the
 * LIBRESPEED_URL environment variable points to one (CI starts the official
 * docker image for this).
 */
class LibrespeedIntegrationTest {

    @Test
    fun fullTestAgainstRealBackend() {
        val base = System.getenv("LIBRESPEED_URL")
        assumeTrue("LIBRESPEED_URL not set; skipping the integration test", !base.isNullOrBlank())

        val testPoint = TestPoint(
            "docker", base!!.trimEnd('/'),
            "/backend/garbage.php", "/backend/empty.php", "/backend/empty.php", "/backend/getIP.php"
        )
        val config = SpeedtestConfig(
            JSONObject()
                .put("test_order", "IP_D_U")
                .put("time_dl_max", 3)
                .put("time_ul_max", 3)
                .put("time_auto", false)
                .put("count_ping", 3)
        )
        var download = -1.0
        var upload = -1.0
        var ping = -1.0
        var failure: String? = null
        val done = CountDownLatch(1)
        object : SpeedtestWorker(testPoint, config, TelemetryConfig()) {
            override fun onDownloadUpdate(dl: Double, progress: Double) {
                if (dl > 0) download = dl
            }

            override fun onUploadUpdate(ul: Double, progress: Double) {
                if (ul > 0) upload = ul
            }

            override fun onPingJitterUpdate(p: Double, jitter: Double, progress: Double) {
                if (p > 0) ping = p
            }

            override fun onLossUpdate(loss: Double) = Unit
            override fun onIPInfoUpdate(ipInfo: String?) = Unit
            override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
            override fun onEnd() = done.countDown()

            override fun onCriticalFailure(err: String?) {
                failure = err
                done.countDown()
            }
        }
        assertTrue("engine did not finish in time", done.await(120, TimeUnit.SECONDS))
        assertTrue("critical failure: $failure", failure == null)
        assertTrue("download should be measured, got $download", download > 0)
        assertTrue("upload should be measured, got $upload", upload > 0)
        assertTrue("ping should be measured, got $ping", ping > 0)
    }

}
