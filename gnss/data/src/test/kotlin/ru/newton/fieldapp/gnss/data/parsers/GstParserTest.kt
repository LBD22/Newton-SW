package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GstParserTest {
    @Test
    fun `canonical example parses sigma values`() {
        val payload = "GPGST,172814.0,0.006,0.023,0.020,273.6,0.023,0.020,0.031"
        val out = GstParser.parse(payload.split(',')) as NmeaParsed.Gst
        assertEquals(0.023, out.sigmaLat)
        assertEquals(0.020, out.sigmaLon)
        assertEquals(0.031, out.sigmaAlt)
    }

    @Test
    fun `truncated payload returns Malformed`() {
        val parsed = GstParser.parse(listOf("GPGST", "1", "2"))
        assert(parsed is NmeaParsed.Malformed)
    }

    @Test
    fun `non-numeric sigma surfaces as Malformed`() {
        val payload = "GPGST,172814.0,0.006,0.023,0.020,273.6,abc,0.020,0.031"
        val parsed = GstParser.parse(payload.split(','))
        assert(parsed is NmeaParsed.Malformed)
    }
}
