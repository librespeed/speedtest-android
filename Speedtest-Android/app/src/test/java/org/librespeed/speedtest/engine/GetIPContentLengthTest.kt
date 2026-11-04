package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import com.fdossena.speedtest.core.getIP.GetIP
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//content-length counts bytes but the body is read as chars: multi-byte UTF-8 must not
//leave NUL padding, and a body arriving in several TCP segments must be read completely
class GetIPContentLengthTest {

    private fun fetchBody(response: MockResponse): String? {
        val server = MockWebServer()
        server.enqueue(response)
        server.start()
        try {
            val received = arrayOfNulls<String>(1)
            val done = CountDownLatch(1)
            object : GetIP(Connection(server.url("/").toString().trimEnd('/')), "/getIP.php", false, null) {
                override fun onDataReceived(data: String?) {
                    received[0] = data
                    done.countDown()
                }

                override fun onError(err: String?) {
                    done.countDown()
                }
            }
            assertTrue("GetIP did not finish", done.await(10, TimeUnit.SECONDS))
            return received[0]
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun multiByteBodyArrivesWithoutNulPadding() {
        val body = """{"processedString":"1.2.3.4 - Türk Telekom","rawIspInfo":""}"""
        val received = fetchBody(MockResponse().setResponseCode(200).setBody(body))
        assertEquals(body, received)
        assertFalse("body must not carry NUL padding", received!!.contains('\u0000'))
    }

    @Test
    fun slowlyDeliveredBodyIsReadCompletely() {
        val body = """{"processedString":"203.0.113.7 - Example Carrier International"}"""
        val received = fetchBody(
            MockResponse().setResponseCode(200).setBody(body)
                .throttleBody(16, 50, TimeUnit.MILLISECONDS)
        )
        assertEquals(body, received)
    }

}
