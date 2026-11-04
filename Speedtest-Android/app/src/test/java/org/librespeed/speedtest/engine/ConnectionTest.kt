package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.ping.Pinger
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConnectionTest {

    @Test
    fun userAgentHeaderIsSingleLine() {
        Connection.setUserAgent("librespeed-android/2.0.0 (android 16; arm64-v8a; raven)")
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()
        try {
            val connection = Connection(server.url("/").toString().trimEnd('/'))
            connection.GET("/empty", true)
            val recorded = server.takeRequest(10, TimeUnit.SECONDS)!!
            val userAgent = recorded.getHeader("User-Agent")
            assertEquals("librespeed-android/2.0.0 (android 16; arm64-v8a; raven)", userAgent)
            assertFalse("User-Agent must not contain line breaks", userAgent!!.contains("\r") || userAgent.contains("\n"))
            connection.close()
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun pingerAcceptsAnySuccessStatus() {
        val server = MockWebServer()
        //204 No Content is a perfectly fine answer for an empty ping endpoint
        server.enqueue(MockResponse().setResponseCode(204))
        server.start()
        try {
            val pong = CountDownLatch(1)
            object : Pinger(Connection(server.url("/").toString().trimEnd('/')), "/empty") {
                override fun onPong(ns: Long): Boolean {
                    pong.countDown()
                    return false
                }

                override fun onError(err: String?) = Unit
            }
            assertTrue("2xx response should count as a pong", pong.await(10, TimeUnit.SECONDS))
        } finally {
            server.shutdown()
        }
    }

}
