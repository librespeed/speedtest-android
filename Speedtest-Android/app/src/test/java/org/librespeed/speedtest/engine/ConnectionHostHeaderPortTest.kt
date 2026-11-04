package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

//RFC 7230 section 5.4: the Host header must carry the port when it is not the scheme default
class ConnectionHostHeaderPortTest {

    @Test
    fun hostHeaderCarriesTheNonDefaultPort() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()
        try {
            val url = server.url("/")
            val connection = Connection("http://${url.host}:${url.port}")
            connection.GET("/empty", true)
            val recorded = server.takeRequest(10, TimeUnit.SECONDS)!!
            assertEquals("${url.host}:${url.port}", recorded.getHeader("Host"))
            connection.close()
        } finally {
            server.shutdown()
        }
    }

}
