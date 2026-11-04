package org.librespeed.speedtest.data

import kotlin.math.roundToInt
import kotlin.math.sqrt

object TestStats {

    /** Bufferbloat grade from the latency increase under load, using the common Waveform buckets. */
    fun bufferbloatGrade(idleMs: Double, loadedMs: Double): String? {
        if (!idleMs.isFinite() || !loadedMs.isFinite()) return null
        if (idleMs < 0 || loadedMs < 0) return null
        val delta = loadedMs - idleMs
        return when {
            delta <= 5.0 -> "A+"
            delta <= 30.0 -> "A"
            delta <= 60.0 -> "B"
            delta <= 200.0 -> "C"
            delta <= 400.0 -> "D"
            else -> "F"
        }
    }

    data class Stability(val min: Double, val max: Double, val average: Double, val variationPct: Int)

    /** Spread of the sampled speeds; null when there are not enough usable samples to say anything. */
    fun stability(samples: List<Double>): Stability? {
        val finite = samples.filter { it.isFinite() && it >= 0 }
        if (finite.size < 5) return null
        val average = finite.average()
        if (average <= 0) return null
        val deviation = sqrt(finite.sumOf { (it - average) * (it - average) } / finite.size)
        return Stability(
            min = finite.min(),
            max = finite.max(),
            average = average,
            variationPct = (deviation / average * 100).roundToInt()
        )
    }

}
