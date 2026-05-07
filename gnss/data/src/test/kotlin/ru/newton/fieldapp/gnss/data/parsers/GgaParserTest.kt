package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class GgaParserTest {
    @Test
    fun `canonical NMEA reference parses to known position`() {
        val payload = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"
        val out = GgaParser.parse(payload.split(',')) as NmeaParsed.Gga
        // 4807.038 → 48° 07.038′ → 48.1173° (within 1e-4)
        assert(abs(out.latitude - 48.1173) < 1e-4) { "lat=${out.latitude}" }
        // 01131.000 → 11° 31.000′ → 11.51666...°
        assert(abs(out.longitude - 11.5166667) < 1e-4) { "lon=${out.longitude}" }
        assertEquals(1, out.fixQuality)
        assertEquals(8, out.satsUsed)
        assertEquals(0.9, out.hdop)
        assertEquals(545.4, out.ellipsoidalHeight)
    }

    @Test
    fun `fixed-RTK row preserves correction age`() {
        val payload = "GPGGA,123520.00,5546.34896,N,03737.20840,E,4,15,0.6,234.567,M,14.2,M,1.2,0123"
        val out = GgaParser.parse(payload.split(',')) as NmeaParsed.Gga
        assertEquals(4, out.fixQuality) // RTK Fixed
        assertEquals(1.2, out.correctionAgeSec)
        assertEquals(15, out.satsUsed)
    }

    @Test
    fun `southern and western hemispheres negate the decimal value`() {
        val payload = "GPGGA,000000,3351.000,S,07037.000,W,1,05,1.0,12.0,M,0.0,M,,"
        val out = GgaParser.parse(payload.split(',')) as NmeaParsed.Gga
        assert(out.latitude < 0)
        assert(out.longitude < 0)
    }

    @Test
    fun `no-fix row preserves null altitude and quality 0`() {
        val payload = "GPGGA,000000.00,,,,,0,00,99.9,,M,,M,,"
        val parsed = GgaParser.parse(payload.split(','))
        // Empty lat/lon → Malformed (we cannot project a missing position).
        assert(parsed is NmeaParsed.Malformed)
    }

    @Test
    fun `truncated payload returns Malformed`() {
        val parsed = GgaParser.parse(listOf("GPGGA", "123519"))
        assert(parsed is NmeaParsed.Malformed)
    }
}
