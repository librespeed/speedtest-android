package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

//a protocol-relative "//host:port" server on a plain-HTTP port must fall back to HTTP:
//the HTTPS attempt may only be committed once the TLS handshake has actually succeeded,
//not on the bare TCP connect
class ConnectionHttpsFallbackTest {

    @Test
    fun protocolRelativeUrlFallsBackToHttpWhenTlsHandshakeFails() {
        val server = MockWebServer()
        //spares, in case the garbage ClientHello is mistaken for a request and answered
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200)) }
        server.start()
        try {
            val url = server.url("/")
            val connection = Connection("//${url.host}:${url.port}", 800, 5000, -1, -1)
            assertEquals(1, connection.mode) //1=MODE_HTTP
            connection.GET("/empty", true)
            assertNotNull(connection.parseResponseHeaders()) //throws unless an HTTP 2xx came back
            connection.close()
        } finally {
            server.shutdown()
        }
    }

}
