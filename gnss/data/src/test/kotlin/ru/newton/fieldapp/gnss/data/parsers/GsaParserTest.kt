package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GsaParserTest {
    @Test
    fun `extracts DOP and active PRN list`() {
        val payload = "GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1"
        val out = GsaParser.parse(payload.split(',')) as NmeaParsed.Gsa
        assertEquals(2.5, out.pdop)
        assertEquals(1.3, out.hdop)
        assertEquals(2.1, out.vdop)
        // PRN slots collapse empty cells; count what's actually populated.
        assertEquals(listOf(4, 5, 9, 12, 24), out.satPrns)
    }

    @Test
    fun `multi-constellation GNGSA still parses`() {
        val payload = "GNGSA,A,3,01,03,06,12,17,19,28,,,,,,1.4,0.7,1.2"
        val out = GsaParser.parse(payload.split(',')) as NmeaParsed.Gsa
        assertEquals(7, out.satPrns.size)
        assertEquals(0.7, out.hdop)
    }

    @Test
    fun `truncated payload returns Malformed`() {
        val parsed = GsaParser.parse(listOf("GPGSA", "A", "3"))
        assert(parsed is NmeaParsed.Malformed)
    }
}
