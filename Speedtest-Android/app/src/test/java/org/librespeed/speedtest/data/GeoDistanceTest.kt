package org.librespeed.speedtest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun `distance between Prague and Brno is about 185 km`() {
        val km = GeoDistance.km(50.0755, 14.4378, 49.1951, 16.6068)
        assertEquals(185.0, km, 10.0)
    }

    @Test
    fun `distance to the same point is zero`() {
        assertEquals(0.0, GeoDistance.km(50.0, 14.0, 50.0, 14.0), 0.001)
    }

    @Test
    fun `clean name strips the sponsor`() {
        assertEquals("Prague, Czech Republic", GeoDistance.cleanName("Prague, Czech Republic (CESNET)"))
    }

    @Test
    fun `clean name keeps names without sponsor`() {
        assertEquals("Helsinki, Finland", GeoDistance.cleanName("Helsinki, Finland"))
    }

    @Test
    fun `sponsor is extracted from parentheses`() {
        assertEquals("CESNET", GeoDistance.sponsor("Prague, Czech Republic (CESNET)"))
    }

    @Test
    fun `sponsor is null without parentheses`() {
        assertNull(GeoDistance.sponsor("Helsinki, Finland"))
    }

}
