package org.librespeed.speedtest.data

data class IpInfoDisplay(
    val ip: String,
    val isp: String? = null,
    val location: String? = null,
    val coordinates: String? = null
)

fun parseIpInfoDisplay(info: String?): IpInfoDisplay {
    val text = info?.trim().orEmpty()
    if (text.isEmpty()) return IpInfoDisplay("")

    val lines = text.lines()
    val base = lines.firstOrNull()?.trim().orEmpty()
    val ip = base.substringBefore(" - ").trim()
    val isp = base.substringAfter(" - ", "").trim().takeIf { it.isNotEmpty() }

    var location: String? = null
    var coordinates: String? = null
    for (line in lines.drop(1)) {
        val trimmed = line.trim()
        if (trimmed.startsWith("Approximate IP-derived location:")) {
            location = trimmed.substringAfter(":", "").trim().takeIf { it.isNotEmpty() }
        } else if (trimmed.startsWith("Latitude/longitude:")) {
            coordinates = trimmed.substringAfter(":", "").trim().takeIf { it.isNotEmpty() }
        }
    }
    return IpInfoDisplay(ip, isp, location, coordinates)
}
