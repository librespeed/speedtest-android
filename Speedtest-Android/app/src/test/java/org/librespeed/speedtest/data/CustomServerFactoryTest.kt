package org.librespeed.speedtest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CustomServerFactoryTest {

    @Test
    fun `https url with sub-path moves the path into endpoints`() {
        val testPoint = CustomServerFactory.create("Test", "https://example.com/backend")
        assertEquals("https://example.com", testPoint.server)
        assertEquals("backend/garbage.php", testPoint.dlURL)
        assertEquals("backend/empty.php", testPoint.pingURL)
        assertEquals("backend/getIP.php", testPoint.getIpURL)
    }

    @Test
    fun `url without scheme falls back to protocol relative`() {
        val testPoint = CustomServerFactory.create("Test", "example.com")
        assertEquals("//example.com", testPoint.server)
        assertEquals("garbage.php", testPoint.dlURL)
    }

    @Test
    fun `http scheme and port are preserved`() {
        val testPoint = CustomServerFactory.create("Test", "http://example.com:8080")
        assertEquals("http://example.com:8080", testPoint.server)
    }

    @Test
    fun `ipv6 literal stays bracketed`() {
        val testPoint = CustomServerFactory.create("Test", "https://[2001:db8::1]:8443/speed")
        assertEquals("https://[2001:db8::1]:8443", testPoint.server)
        assertEquals("speed/garbage.php", testPoint.dlURL)
    }

    @Test
    fun `uppercase schemes keep their protocol`() {
        assertEquals("https://example.com", CustomServerFactory.create("Test", "HTTPS://example.com").server)
        assertEquals("http://example.com:8080", CustomServerFactory.create("Test", "HTTP://example.com:8080").server)
    }

    @Test
    fun `non-http schemes are rejected instead of downgraded`() {
        listOf("ftp://example.com", "file:///etc/passwd", "ws://example.com").forEach { url ->
            assertThrows(url, IllegalArgumentException::class.java) {
                CustomServerFactory.create("Test", url)
            }
        }
    }

    @Test
    fun `blank name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CustomServerFactory.create("   ", "https://example.com")
        }
    }

    @Test
    fun `invalid url is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CustomServerFactory.create("Test", "not a url")
        }
    }

    @Test
    fun `urls without a usable host are rejected`() {
        listOf("https://", "http:///path", "https://:8080", "%%%").forEach { url ->
            assertThrows(url, IllegalArgumentException::class.java) {
                CustomServerFactory.create("Test", url)
            }
        }
    }

    @Test
    fun `whitespace and trailing slashes are trimmed`() {
        val testPoint = CustomServerFactory.create("Test", "  https://example.com/backend///  ")
        assertEquals("https://example.com", testPoint.server)
        assertEquals("backend/garbage.php", testPoint.dlURL)
    }

}
