package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.Speedtest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

//the server list used to be downloaded twice per load (one stream opened and leaked)
class ServerListLoaderSingleFetchTest {

    @Test
    fun serverListIsFetchedWithASingleRequest() {
        val list = """[{"name":"t","server":"//example.com/","dlURL":"garbage.php","ulURL":"empty.php","pingURL":"empty.php","getIpURL":"getIP.php"}]"""
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(list))
        server.start()
        try {
            val st = Speedtest()
            assertTrue(st.loadServerList(server.url("/list.json").toString()))
            assertEquals(1, st.testPoints.size)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

}
