package org.librespeed.speedtest.engine

import com.fdossena.speedtest.core.config.SpeedtestConfig
import com.fdossena.speedtest.core.config.TelemetryConfig
import com.fdossena.speedtest.core.getIP.IpInfoParser
import com.fdossena.speedtest.core.serverSelector.TestPoint
import com.fdossena.speedtest.core.worker.SpeedtestWorker
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librespeed.speedtest.data.parseIpInfoDisplay
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class IpInfoLocationTest {

    @Test
    fun validLocationAddsPlaceAndCoordinates() {
        val payload = JSONObject()
            .put("processedString", "203.0.113.7 - Example ISP")
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("country", "US")
                .put("countryName", "United States")
                .put("latitude", 38.1234)
                .put("longitude", -77.1234))
            .toString()

        assertEquals(
            "203.0.113.7 - Example ISP\n" +
                "Approximate IP-derived location: Testville, Testshire, United States\n" +
                "Latitude/longitude: 38.1234, -77.1234",
            IpInfoParser.formatDisplayString(payload)
        )
    }

    @Test
    fun countryCodeFallbackAndStringCoordinatesAreAccepted() {
        val payload = JSONObject()
            .put("processedString", "198.51.100.8 - Example ISP")
            .put("location", JSONObject()
                .put("city", "Sample City")
                .put("region", "Sample Region")
                .put("country", "US")
                .put("latitude", "38.1234")
                .put("longitude", "-77.1234"))
            .toString()

        assertEquals(
            "198.51.100.8 - Example ISP\n" +
                "Approximate IP-derived location: Sample City, Sample Region, US\n" +
                "Latitude/longitude: 38.1234, -77.1234",
            IpInfoParser.formatDisplayString(payload)
        )
    }

    @Test
    fun locStringFallbackIsAccepted() {
        val payload = JSONObject()
            .put("processedString", "192.0.2.10 - Example ISP")
            .put("location", JSONObject()
                .put("city", "Locville")
                .put("region", "Lochire")
                .put("countryName", "Testland")
                .put("loc", "36.6676,-78.3875"))
            .toString()

        assertEquals(
            "192.0.2.10 - Example ISP\n" +
                "Approximate IP-derived location: Locville, Lochire, Testland\n" +
                "Latitude/longitude: 36.6676, -78.3875",
            IpInfoParser.formatDisplayString(payload)
        )
    }

    @Test
    fun nonFiniteStringsFallBackToAliasesAndLoc() {
        val aliasPayload = JSONObject()
            .put("processedString", "198.51.100.9 - Example ISP")
            .put("location", JSONObject()
                .put("city", "Aliasville")
                .put("region", "Aliashire")
                .put("countryName", "Testland")
                .put("latitude", "NaN")
                .put("lat", 38.1234)
                .put("longitude", "Infinity")
                .put("lon", -77.1234))
            .toString()

        assertEquals(
            "198.51.100.9 - Example ISP\n" +
                "Approximate IP-derived location: Aliasville, Aliashire, Testland\n" +
                "Latitude/longitude: 38.1234, -77.1234",
            IpInfoParser.formatDisplayString(aliasPayload)
        )

        val locPayload = JSONObject()
            .put("processedString", "198.51.100.10 - Example ISP")
            .put("location", JSONObject()
                .put("city", "Locville")
                .put("region", "Lochire")
                .put("countryName", "Testland")
                .put("latitude", "NaN")
                .put("longitude", "Infinity")
                .put("loc", "36.6676,-78.3875"))
            .toString()

        assertEquals(
            "198.51.100.10 - Example ISP\n" +
                "Approximate IP-derived location: Locville, Lochire, Testland\n" +
                "Latitude/longitude: 36.6676, -78.3875",
            IpInfoParser.formatDisplayString(locPayload)
        )
    }

    @Test
    fun placeMetadataRemainsUsableWithoutCoordinates() {
        val processed = "203.0.113.20 - Legacy ISP"
        val fullPlace = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "Testland"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, Testshire, Testland",
            IpInfoParser.formatDisplayString(fullPlace)
        )

        val countryOnly = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("countryName", "United States"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: United States",
            IpInfoParser.formatDisplayString(countryOnly)
        )

        val regionAndCountry = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("region", "Testshire")
                .put("country", "US"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testshire, US",
            IpInfoParser.formatDisplayString(regionAndCountry)
        )

        val cityAndCountry = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("country", "US"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, US",
            IpInfoParser.formatDisplayString(cityAndCountry)
        )

        val malformedLoc = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "Testland")
                .put("loc", "not-a-coordinate"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, Testshire, Testland",
            IpInfoParser.formatDisplayString(malformedLoc)
        )
    }

    @Test
    fun coordinatesRemainUsableWithoutPlace() {
        val processed = "203.0.113.29 - Legacy ISP"
        val payload = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("latitude", 38.1234)
                .put("longitude", -77.1234))
            .toString()

        assertEquals(
            "$processed\nLatitude/longitude: 38.1234, -77.1234",
            IpInfoParser.formatDisplayString(payload)
        )
    }

    @Test
    fun invalidPrimaryCoordinatesFallBackToAliasesNestedAndLoc() {
        val processed = "203.0.113.30 - Legacy ISP"
        val aliasPayload = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "Testland")
                .put("latitude", 91)
                .put("lat", 38.1234)
                .put("longitude", 181)
                .put("lon", -77.1234))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, Testshire, Testland\n" +
                "Latitude/longitude: 38.1234, -77.1234",
            IpInfoParser.formatDisplayString(aliasPayload)
        )

        val nestedPayload = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "Testland")
                .put("latitude", 91)
                .put("longitude", 181)
                .put("coordinates", JSONObject()
                    .put("latitude", 36.6676)
                    .put("longitude", -78.3875)))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, Testshire, Testland\n" +
                "Latitude/longitude: 36.6676, -78.3875",
            IpInfoParser.formatDisplayString(nestedPayload)
        )

        val locPayload = JSONObject()
            .put("processedString", processed)
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "Testland")
                .put("latitude", 91)
                .put("longitude", 181)
                .put("loc", "35.1,-79.2"))
            .toString()
        assertEquals(
            "$processed\nApproximate IP-derived location: Testville, Testshire, Testland\n" +
                "Latitude/longitude: 35.1, -79.2",
            IpInfoParser.formatDisplayString(locPayload)
        )
    }

    @Test
    fun malformedPayloadKeepsLegacyText() {
        val processed = "203.0.113.20 - Legacy ISP"
        val cases = listOf(
            JSONObject().put("processedString", processed).toString(),
            JSONObject().put("processedString", processed)
                .put("location", JSONObject()
                    .put("latitude", "NaN")
                    .put("longitude", "Infinity"))
                .toString(),
            JSONObject().put("processedString", processed)
                .put("location", JSONObject()
                    .put("latitude", 91)
                    .put("longitude", 181))
                .toString()
        )

        cases.forEach { payload ->
            assertEquals(processed, IpInfoParser.formatDisplayString(payload))
        }
        assertEquals("127.0.0.1 - Plain text", IpInfoParser.formatDisplayString("127.0.0.1 - Plain text"))
        assertEquals("{not-json", IpInfoParser.formatDisplayString("{not-json"))
    }

    @Test
    fun callbackCanBeSplitIntoDisplayParts() {
        val display = parseIpInfoDisplay(
            "203.0.113.7 - Example ISP\n" +
                "Approximate IP-derived location: Testville, Testshire, United States\n" +
                "Latitude/longitude: 38.1234, -77.1234"
        )

        assertEquals("203.0.113.7", display.ip)
        assertEquals("Example ISP", display.isp)
        assertEquals("Testville, Testshire, United States", display.location)
        assertEquals("38.1234, -77.1234", display.coordinates)
    }

    @Test
    fun rawPayloadIsPreservedForTelemetryWhileCallbackIsFormatted() {
        val rawPayload = JSONObject()
            .put("processedString", "203.0.113.7 - Example ISP")
            .put("rawIspInfo", JSONObject()
                .put("ip", "203.0.113.7")
                .put("org", "AS64496 Example ISP"))
            .put("location", JSONObject()
                .put("city", "Testville")
                .put("region", "Testshire")
                .put("countryName", "United States")
                .put("latitude", 38.1234)
                .put("longitude", -77.1234))
            .toString()

        val server = MockWebServer()
        val callback = AtomicReference<String?>()
        val telemetryBody = AtomicReference<String?>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("getIP") -> MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(rawPayload)
                    path.contains("/results/telemetry.php") -> {
                        telemetryBody.set(request.body.readUtf8())
                        MockResponse().setBody("id 1234\n")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            val config = SpeedtestConfig().apply { test_order = "I" }
            val telemetry = TelemetryConfig(TelemetryConfig.LEVEL_FULL, null, null, null)
            val done = CountDownLatch(1)
            object : SpeedtestWorker(TestPoint("test", base, "/garbage", "/empty", "/empty", "/getIP"), config, telemetry) {
                override fun onDownloadUpdate(dl: Double, progress: Double) = Unit
                override fun onUploadUpdate(ul: Double, progress: Double) = Unit
                override fun onPingJitterUpdate(ping: Double, jitter: Double, progress: Double) = Unit
                override fun onLossUpdate(loss: Double) = Unit
                override fun onIPInfoUpdate(ipInfo: String?) = callback.set(ipInfo)
                override fun onTestIDReceived(id: String?, shareURLTemplate: String?) = Unit
                override fun onEnd() = done.countDown()
                override fun onCriticalFailure(err: String?) = done.countDown()
            }

            assertTrue("worker did not finish", done.await(30, TimeUnit.SECONDS))
            assertEquals(
                "203.0.113.7 - Example ISP\n" +
                    "Approximate IP-derived location: Testville, Testshire, United States\n" +
                    "Latitude/longitude: 38.1234, -77.1234",
                callback.get()
            )

            val body = telemetryBody.get()
            assertTrue("telemetry POST body was not received", body != null)
            val encodedIsp = body!!.substringAfter("ispinfo=").substringBefore("&")
            val decodedIsp = URLDecoder.decode(encodedIsp, "UTF-8")
            assertEquals(rawPayload, decodedIsp)
        } finally {
            server.shutdown()
        }
    }
}
