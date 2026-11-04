package org.librespeed.speedtest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestStatsTest {

    private fun grade(delta: Double): String? = TestStats.bufferbloatGrade(10.0, 10.0 + delta)

    @Test
    fun `grade boundaries fall into the documented buckets`() {
        assertEquals("A+", grade(0.0))
        assertEquals("A+", grade(5.0))
        assertEquals("A", grade(5.001))
        assertEquals("A", grade(30.0))
        assertEquals("B", grade(30.001))
        assertEquals("B", grade(60.0))
        assertEquals("C", grade(60.001))
        assertEquals("C", grade(200.0))
        assertEquals("D", grade(200.001))
        assertEquals("D", grade(400.0))
        assertEquals("F", grade(400.001))
    }

    @Test
    fun `loaded latency below idle is still an A+`() {
        assertEquals("A+", TestStats.bufferbloatGrade(50.0, 20.0))
    }

    @Test
    fun `sentinel and invalid inputs give no grade`() {
        assertNull(TestStats.bufferbloatGrade(-1.0, 100.0))
        assertNull(TestStats.bufferbloatGrade(10.0, -1.0))
        assertNull(TestStats.bufferbloatGrade(-1.0, -1.0))
        assertNull(TestStats.bufferbloatGrade(Double.NaN, 100.0))
        assertNull(TestStats.bufferbloatGrade(10.0, Double.NaN))
        assertNull(TestStats.bufferbloatGrade(10.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun `stability needs at least five usable samples`() {
        assertNull(TestStats.stability(emptyList()))
        assertNull(TestStats.stability(List(4) { 100.0 }))
        assertNull(TestStats.stability(List(4) { 100.0 } + Double.NaN))
    }

    @Test
    fun `stability of a flat line has zero variation`() {
        val stability = TestStats.stability(List(10) { 100.0 })!!
        assertEquals(100.0, stability.min, 0.0)
        assertEquals(100.0, stability.max, 0.0)
        assertEquals(100.0, stability.average, 0.0)
        assertEquals(0, stability.variationPct)
    }

    @Test
    fun `stability reports the spread of a varying line`() {
        val stability = TestStats.stability(listOf(50.0, 100.0, 150.0, 100.0, 100.0))!!
        assertEquals(50.0, stability.min, 0.0)
        assertEquals(150.0, stability.max, 0.0)
        assertEquals(100.0, stability.average, 0.0)
        assertEquals(32, stability.variationPct)
    }

    @Test
    fun `all-zero and non-finite samples give no stability`() {
        assertNull(TestStats.stability(List(10) { 0.0 }))
        assertNull(TestStats.stability(List(10) { Double.NaN }))
        assertNull(TestStats.stability(List(10) { Double.POSITIVE_INFINITY }))
    }

    @Test
    fun `negative samples are ignored, not averaged in`() {
        val stability = TestStats.stability(listOf(-50.0, 100.0, 100.0, 100.0, 100.0, 100.0))!!
        assertEquals(100.0, stability.average, 0.0)
        assertEquals(100.0, stability.min, 0.0)
    }

}
