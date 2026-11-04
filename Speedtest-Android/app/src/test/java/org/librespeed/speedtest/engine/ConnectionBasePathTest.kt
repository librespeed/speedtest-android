package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.base.Connection
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

//the "server" field of a test point may carry a path (e.g. "https://host/backend");
//relative endpoint paths must resolve under it instead of being root-relativized
class ConnectionBasePathTest {

    private fun requestedPath(serverSuffix: String, endpoint: String): String {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()
        try {
            val connection = Connection(server.url("/").toString().trimEnd('/') + serverSuffix)
            connection.GET(endpoint, true)
            val recorded = server.takeRequest(10, TimeUnit.SECONDS)!!
            connection.close()
            return recorded.path!!
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun relativeEndpointResolvesUnderTheServerPath() {
        assertEquals("/backend/garbage.php", requestedPath("/backend", "garbage.php"))
    }

    @Test
    fun trailingSlashOnTheServerPathIsNormalized() {
        assertEquals("/librespeed/backend/garbage.php", requestedPath("/librespeed/", "backend/garbage.php"))
    }

    @Test
    fun rootRelativeEndpointIgnoresTheServerPath() {
        assertEquals("/garbage.php", requestedPath("/backend", "/garbage.php"))
    }

    @Test
    fun pathlessServerKeepsRootRelativization() {
        assertEquals("/garbage.php", requestedPath("", "garbage.php"))
    }

}
