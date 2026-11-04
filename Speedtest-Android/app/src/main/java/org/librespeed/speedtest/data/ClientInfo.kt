package org.librespeed.speedtest.data

import android.os.Build
import org.librespeed.speedtest.BuildConfig

object ClientInfo {

    val client: String = "LibreSpeed Android ${BuildConfig.VERSION_NAME}"

    /**
     * Product and version, then the platform -- the shape the LibreSpeed CLIs
     * send, so a server sees one family across the clients and its telemetry
     * can tell which kind of machine measured. The device product is part of
     * it because the hardware bounds what a connection can show.
     */
    val userAgent: String =
        "librespeed-android/${BuildConfig.VERSION_NAME} " +
            "(android ${tag(Build.VERSION.RELEASE)}; " +
            "${tag(Build.SUPPORTED_ABIS?.firstOrNull())}; ${tag(Build.PRODUCT)})"

    /**
     * Build properties are set by whoever built the ROM, so they are bounded
     * and reduced to a conservative alphabet before going into a header: a
     * stray line break there would split the request itself.
     */
    private fun tag(value: String?): String =
        value.orEmpty()
            .filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "._-" }
            .take(24)
            .ifEmpty { "unknown" }
}
