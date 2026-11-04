package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.getIP.GetIP
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//SpeedtestConfig accepts getIP_distance="no", so GetIP must too, and must skip the
//distance parameter for it just like for null
class GetIPDistanceNoTest {

    private fun requestPath(distance: String?): Pair<String, String?> {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.start()
        try {
            val error = arrayOfNulls<String>(1)
            val done = CountDownLatch(1)
            object : GetIP(Connection(server.url("/").toString().trimEnd('/')), "/getIP.php", true, distance) {
                override fun onDataReceived(data: String?) {
                    done.countDown()
                }

                override fun onError(err: String?) {
                    error[0] = err
                    done.countDown()
                }
            }
            assertTrue("GetIP did not finish", done.await(10, TimeUnit.SECONDS))
            val recorded = server.takeRequest(10, TimeUnit.SECONDS)!!
            return Pair(recorded.path!!, error[0])
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun configuredDistanceNoIsAcceptedAndOmittedFromTheQuery() {
        val (path, error) = requestPath(SpeedtestConfig.DISTANCE_NO)
        assertNull(error)
        assertTrue(path.contains("isp=true"))
        assertFalse(path.contains("distance="))
    }

    @Test
    fun nullDistanceWithIspDoesNotFail() {
        val (path, error) = requestPath(null)
        assertNull(error)
        assertTrue(path.contains("isp=true"))
        assertFalse(path.contains("distance="))
    }

}
