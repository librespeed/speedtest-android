package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.download.DownloadStream
import com.fdossena.speedtest.core.ping.PingStream
import com.fdossena.speedtest.core.upload.UploadStream
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * When the first connection attempt fails hard, no inner worker is ever created;
 * join() used to spin on the null reference forever, leaking the worker thread.
 * The @Test timeouts turn that former hang into a failure.
 */
class StreamJoinAfterFailureTest {

    //a port with nothing listening: bound once to reserve it, then released
    private fun refusedServer(): String = ServerSocket(0).use { "http://127.0.0.1:${it.localPort}" }

    @Test(timeout = 30_000)
    fun downloadStreamJoinReturnsWhenTheFirstConnectFails() {
        val error = CountDownLatch(1)
        val stream = object : DownloadStream(
            refusedServer(), "/garbage", 1,
            SpeedtestConfig.ONERROR_ATTEMPT_RESTART, 1000, 1000, -1, -1, null
        ) {
            override fun onError(err: String?) = error.countDown()
        }
        assertTrue("hard connect failure must surface as onError", error.await(15, TimeUnit.SECONDS))
        stream.join()
    }

    @Test(timeout = 30_000)
    fun uploadStreamJoinReturnsWhenTheFirstConnectFails() {
        val error = CountDownLatch(1)
        val stream = object : UploadStream(
            refusedServer(), "/empty", 1,
            SpeedtestConfig.ONERROR_ATTEMPT_RESTART, 1000, 1000, -1, -1, null
        ) {
            override fun onError(err: String?) = error.countDown()
        }
        assertTrue("hard connect failure must surface as onError", error.await(15, TimeUnit.SECONDS))
        stream.join()
    }

    @Test(timeout = 30_000)
    fun pingStreamJoinReturnsWhenTheFirstConnectFails() {
        val error = CountDownLatch(1)
        val stream = object : PingStream(
            refusedServer(), "/empty", 3,
            SpeedtestConfig.ONERROR_ATTEMPT_RESTART, 1000, 1000, -1, -1, null
        ) {
            override fun onError(err: String?) = error.countDown()
            override fun onPong(ns: Long) = true
            override fun onDone() = Unit
        }
        assertTrue("hard connect failure must surface as onError", error.await(15, TimeUnit.SECONDS))
        assertTrue("a hard failure is a terminal event", stream.hasEnded())
        stream.join()
    }

}
