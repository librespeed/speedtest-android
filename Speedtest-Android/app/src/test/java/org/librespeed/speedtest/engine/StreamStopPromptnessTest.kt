package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.download.DownloadStream
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * stopASAP() closes the underlying connection so a thread parked in a blocking
 * read (or write) stops promptly instead of waiting out the socket timeout,
 * and the deliberate close must not surface as a stream error.
 */
class StreamStopPromptnessTest {

    @Test(timeout = 30_000)
    fun stopUnblocksADownloaderParkedInRead() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            //accept the request, never answer: the downloader parks in read()
            override fun dispatch(request: RecordedRequest) =
                MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        }
        server.start()
        try {
            val error = CountDownLatch(1)
            val stream = object : DownloadStream(
                server.url("/").toString().trimEnd('/'), "/garbage", 1,
                SpeedtestConfig.ONERROR_ATTEMPT_RESTART, 2000, 15000, -1, -1, null
            ) {
                override fun onError(err: String?) = error.countDown()
            }
            Thread.sleep(500) //let the downloader connect and block on the response
            val stopAt = System.currentTimeMillis()
            stream.stopASAP()
            stream.join()
            val elapsed = System.currentTimeMillis() - stopAt
            assertTrue(
                "stop took ${elapsed}ms; closing the connection should unblock the read well before the 15s soTimeout",
                elapsed < 5000
            )
            assertFalse("a deliberate stop must not surface as an error", error.await(200, TimeUnit.MILLISECONDS))
        } finally {
            server.shutdown()
        }
    }

}
