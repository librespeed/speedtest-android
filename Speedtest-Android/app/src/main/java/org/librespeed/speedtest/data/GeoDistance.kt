package org.librespeed.speedtest.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {

    fun km(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthRadiusKm * asin(sqrt(a))
    }

    /** "Prague, Czech Republic (CESNET)" -> "Prague, Czech Republic" */
    fun cleanName(name: String): String = name.substringBefore("(").trim().trimEnd(',')

    /** "Prague, Czech Republic (CESNET)" -> "CESNET" or null */
    fun sponsor(name: String): String? =
        name.substringAfter("(", "").removeSuffix(")").trim().ifEmpty { null }

    /** "https://librespeed.org/backend/" -> "librespeed.org/backend" */
    fun hostLabel(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("//")
        .trimEnd('/')

}
