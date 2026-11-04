package org.librespeed.speedtest.data

import com.fdossena.speedtest.core.serverSelector.TestPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ServerKeyTest {

    private fun testPoint(name: String, server: String) =
        TestPoint(name, server, "garbage.php", "empty.php", "empty.php", "getIP.php")

    @Test
    fun `servers without list id fall back to name and host`() {
        assertEquals("Prague|//a.example", testPoint("Prague", "//a.example").key())
    }

    @Test
    fun `different hosts give different keys`() {
        assertNotEquals(
            testPoint("Prague", "//a.example").key(),
            testPoint("Prague", "//b.example").key()
        )
    }

}
