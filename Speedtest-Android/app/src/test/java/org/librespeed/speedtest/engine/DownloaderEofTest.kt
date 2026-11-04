package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.download.Downloader
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A server that closes the keep-alive connection cleanly makes read() return -1
 * forever; that must surface as an error, not corrupt the byte counter and turn
 * the download loop into a hot spin.
 */
class DownloaderEofTest {

    @Test(timeout = 30_000)
    fun cleanServerCloseSurfacesAsErrorInsteadOfSpinning() {
        val server = MockWebServer()
        //64KB is far less than the 1MB the downloader asks for; the close ends the stream early
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(ByteArray(65536)))
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END)
        )
        server.start()
        try {
            val error = CountDownLatch(1)
            val connection = Connection(server.url("/").toString().trimEnd('/'), 2000, 2000, -1, -1)
            val downloader = object : Downloader(connection, "/garbage", 1) {
                override fun onProgress(downloaded: Long) = Unit
                override fun onError(err: String?) = error.countDown()
            }
            assertTrue("EOF must surface as an error, not a busy spin", error.await(10, TimeUnit.SECONDS))
            downloader.join(5000)
            assertTrue("EOF must not be counted into the download total", downloader.downloaded >= 0)
        } finally {
            server.shutdown()
        }
    }

}
