package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TraParserTest {
    @Test
    fun `extracts heading pitch roll from a populated row`() {
        val payload = "GPTRA,123520.00,123.45,1.2,-0.5,3,12,1.0"
        val out = TraParser.parse(payload.split(',')) as NmeaParsed.Tra
        assertEquals(123.45, out.headingDeg)
        assertEquals(1.2, out.pitchDeg)
        assertEquals(-0.5, out.rollDeg)
    }

    @Test
    fun `IMU not yet ready surfaces nulls instead of zeros`() {
        // Empty fields must NOT collapse to 0.0 — that would mislead the heading
        // arrow into pointing at the equator.
        val payload = "GPTRA,123521.00,,,,0,0,"
        val out = TraParser.parse(payload.split(',')) as NmeaParsed.Tra
        assertNull(out.headingDeg)
        assertNull(out.pitchDeg)
        assertNull(out.rollDeg)
    }

    @Test
    fun `truncated payload returns Malformed`() {
        val parsed = TraParser.parse(listOf("GPTRA", "1", "2"))
        assert(parsed is NmeaParsed.Malformed)
    }
}
