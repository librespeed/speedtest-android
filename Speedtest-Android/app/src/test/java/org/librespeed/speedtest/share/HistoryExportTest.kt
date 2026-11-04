package org.librespeed.speedtest.share

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librespeed.speedtest.data.HistoryEntry

class HistoryExportTest {

    private val plain = HistoryEntry(
        id = 1, date = 1_700_000_000_000, server = "Prague, Czech Republic (CESNET)",
        ping = 8.5, jitter = 1.25, download = 512.256, upload = 256.5, loss = 0.5,
        ipInfo = null, ipVersion = 4, shareUrl = null, networkType = "Wi-Fi 6",
        durationMs = 20_500, mode = "standard", loadedDown = 12.0, loadedUp = 34.5
    )

    //commas, quotes and Czech diacritics in one name
    private val tricky = plain.copy(
        id = 2,
        server = "Ústí nad Labem, \"CZ\", žluťoučký",
        networkType = null,
        ipVersion = 0,
        loss = -1.0,
        loadedDown = -1.0,
        loadedUp = -1.0,
        durationMs = 0,
        mode = null
    )

    @Test
    fun `csv escapes quotes and commas and keeps unicode`() {
        val csv = HistoryExport.buildCsv(listOf(tricky))
        val dataLine = csv.lines()[1]
        assertTrue(dataLine.contains("\"Ústí nad Labem, \"\"CZ\"\", žluťoučký\""))
    }

    @Test
    fun `csv quotes embedded newlines so one entry stays one record`() {
        val csv = HistoryExport.buildCsv(listOf(plain.copy(server = "Line one\nline two", networkType = "Wi\r\nFi")))
        assertTrue(csv.contains("\"Line one\nline two\""))
        assertTrue(csv.contains("\"Wi\r\nFi\""))
    }

    @Test
    fun `csv neutralizes formula prefixes`() {
        val csv = HistoryExport.buildCsv(listOf(plain.copy(server = "=SUM(A1)", networkType = "@cmd", mode = "+plus")))
        val dataLine = csv.lines()[1]
        assertTrue(dataLine.contains("'=SUM(A1)"))
        assertTrue(dataLine.contains("'@cmd"))
        assertTrue(dataLine.contains("'+plus"))
        assertFalse(dataLine.contains(",=SUM"))
    }

    @Test
    fun `csv writes sentinel values as empty fields`() {
        val csv = HistoryExport.buildCsv(listOf(tricky))
        val fields = csv.lines()[1].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
        assertEquals("", fields[2])  //network
        assertEquals("", fields[3])  //protocol
        assertEquals("", fields[8])  //loss
        assertEquals("", fields[9])  //loaded down
        assertEquals("", fields[10]) //loaded up
        assertEquals("", fields[11]) //duration
        assertEquals("", fields[12]) //mode
    }

    @Test
    fun `csv formats numbers with two decimals and a dot`() {
        val csv = HistoryExport.buildCsv(listOf(plain))
        val fields = csv.lines()[1].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
        assertEquals("512.26", fields[4])
        assertEquals("256.50", fields[5])
        assertEquals("8.50", fields[6])
        assertEquals("20.50", fields[11])
    }

    @Test
    fun `csv has a header and one line per entry`() {
        val csv = HistoryExport.buildCsv(listOf(plain, tricky))
        val lines = csv.trimEnd().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("date,server,network,protocol,download_mbps"))
    }

    @Test
    fun `json is valid and omits absent fields`() {
        val parsed = JSONArray(HistoryExport.buildJson(listOf(plain, tricky)))
        assertEquals(2, parsed.length())
        val first = parsed.getJSONObject(0)
        assertEquals("Prague, Czech Republic (CESNET)", first.getString("server"))
        assertEquals("IPv4", first.getString("protocol"))
        assertEquals(0.5, first.getDouble("loss_pct"), 0.0)
        val second = parsed.getJSONObject(1)
        assertEquals("Ústí nad Labem, \"CZ\", žluťoučký", second.getString("server"))
        assertFalse(second.has("protocol"))
        assertFalse(second.has("loss_pct"))
        assertFalse(second.has("network"))
        assertFalse(second.has("loaded_down_ms"))
        assertFalse(second.has("duration_ms"))
    }

}
