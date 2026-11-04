package org.librespeed.speedtest.data

import com.fdossena.speedtest.core.serverSelector.TestPoint
import java.net.URI

object CustomServerFactory {

    /**
     * Builds a TestPoint from a user supplied URL. Any sub-path moves into the endpoint
     * fields, which keeps the entry independent of how the engine resolves a base path.
     * Without a scheme the engine tries HTTPS first and falls back to HTTP ("//host").
     */
    @Throws(IllegalArgumentException::class)
    fun create(name: String, url: String): TestPoint {
        require(name.isNotBlank()) { "Name cannot be empty" }
        //decide about the scheme before touching slashes, otherwise "https://" degenerates
        val cleaned = url.trim()
        val trimmed = (if (cleaned.contains("://")) cleaned else "https://$cleaned").trimEnd('/')
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL", e)
        }
        val host = uri.host?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Invalid URL")
        //IPv6 literals must stay bracketed inside the URL
        val bracketedHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        //the scheme the user actually typed; no scheme means protocol relative,
        //and anything the engine does not speak must be rejected, not downgraded
        val scheme = when {
            !cleaned.contains("://") -> "//"
            cleaned.startsWith("http://", ignoreCase = true) -> "http://"
            cleaned.startsWith("https://", ignoreCase = true) -> "https://"
            else -> throw IllegalArgumentException("Only http(s) URLs are supported")
        }
        val server = buildString {
            append(scheme)
            append(bracketedHost)
            if (uri.port != -1) append(":${uri.port}")
        }
        val basePath = uri.path.trim('/')
        fun endpoint(file: String) = if (basePath.isEmpty()) file else "$basePath/$file"
        return TestPoint(
            name.trim(),
            server,
            endpoint("garbage.php"),
            endpoint("empty.php"),
            endpoint("empty.php"),
            endpoint("getIP.php")
        )
    }

}
