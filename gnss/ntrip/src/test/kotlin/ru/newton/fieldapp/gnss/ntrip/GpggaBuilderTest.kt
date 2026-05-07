package ru.newton.fieldapp.gnss.ntrip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus

class GpggaBuilderTest {
    private val now =
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            .apply { set(2026, 4, 3, 12, 34, 56); set(java.util.Calendar.MILLISECOND, 0) }
            .timeInMillis

    private fun status(
        fix: FixQuality = FixQuality.FixedRtk,
        lat: Double? = 55.7558,
        lon: Double? = 37.6173,
        height: Double? = 154.32,
    ) = GnssStatus(
        fix = fix,
        latitude = lat,
        longitude = lon,
        ellipsoidalHeight = height,
        n = null,
        e = null,
        h = null,
        sigmaN = null,
        sigmaE = null,
        sigmaH = null,
        hdop = 0.9,
        pdop = null,
        vdop = null,
        satsUsed = 14,
        satsVisible = 16,
        correctionAgeSec = 1.2,
        headingDeg = null,
        pitchDeg = null,
        rollDeg = null,
        imuValid = false,
        timestampUtc = now,
    )

    @Test
    fun `returns null when no fix`() {
        assertNull(GpggaBuilder.fromStatus(status(fix = FixQuality.NoFix), now))
    }

    @Test
    fun `returns null when latitude missing`() {
        assertNull(GpggaBuilder.fromStatus(status(lat = null), now))
    }

    @Test
    fun `produces valid GPGGA prefix and CRLF terminator`() {
        val gga = GpggaBuilder.fromStatus(status(), now)
        assertNotNull(gga)
        gga!!
        assertTrue(gga.startsWith("\$GPGGA,"))
        assertTrue(gga.endsWith("\r\n"))
    }

    @Test
    fun `checksum field matches XOR of body bytes`() {
        val gga = GpggaBuilder.fromStatus(status(), now)!!
        val body = gga.removePrefix("$").substringBefore("*")
        val embedded = gga.substringAfter("*").removeSuffix("\r\n")
        val expected = body.fold(0) { acc, c -> acc xor c.code }
        assertEquals("%02X".format(expected), embedded)
    }

    @Test
    fun `formats Moscow position with N E hemispheres`() {
        val gga = GpggaBuilder.fromStatus(status(lat = 55.7558, lon = 37.6173), now)!!
        val parts = gga.removePrefix("$").substringBefore("*").split(",")
        assertEquals("GPGGA", parts[0])
        assertEquals("123456.00", parts[1])
        // 55.7558° → 55°45.348'  (55 + 45.348/60 ≈ 55.7558)
        assertEquals("5545.3480", parts[2])
        assertEquals("N", parts[3])
        // 37.6173° → 037°37.038'
        assertEquals("03737.0380", parts[4])
        assertEquals("E", parts[5])
        assertEquals("4", parts[6]) // FixedRtk
        assertEquals("14", parts[7])
        assertEquals("0.9", parts[8])
        assertEquals("154.32", parts[9])
        assertEquals("M", parts[10])
    }

    @Test
    fun `negative coordinates use S W hemispheres`() {
        val gga = GpggaBuilder.fromStatus(status(lat = -33.8688, lon = -70.6483), now)!!
        val parts = gga.removePrefix("$").substringBefore("*").split(",")
        assertEquals("S", parts[3])
        assertEquals("W", parts[5])
    }

    @Test
    fun `forces dot decimal even under ru locale`() {
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"))
            val gga = GpggaBuilder.fromStatus(status(), now)!!
            // No comma in numeric fields — splits would otherwise overcount
            assertTrue(gga.contains("154.32"))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }
}
