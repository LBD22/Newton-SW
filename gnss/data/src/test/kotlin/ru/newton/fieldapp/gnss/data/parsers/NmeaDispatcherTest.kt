package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NmeaDispatcherTest {
    private val dispatcher = NmeaDispatcher()

    @Test
    fun `routes GGA GST GSA TRA to correct typed result`() {
        val cases = mapOf(
            "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,," to NmeaParsed.Gga::class.java,
            "GPGST,172814.0,0.006,0.023,0.020,273.6,0.023,0.020,0.031" to NmeaParsed.Gst::class.java,
            "GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1" to NmeaParsed.Gsa::class.java,
            "GPTRA,123520.00,123.45,1.2,-0.5,3,12,1.0" to NmeaParsed.Tra::class.java,
        )
        for ((payload, expectedClass) in cases) {
            val line = withChecksum(payload)
            val parsed = dispatcher.dispatch(line)
            assertEquals(expectedClass, parsed.javaClass) { "for: $line → $parsed" }
        }
    }

    @Test
    fun `Unknown talker returns Unknown not Malformed`() {
        val line = withChecksum("GPRMC,000000,A,5546.0,N,03737.0,E,0.0,0.0,010125,,,A")
        assertEquals(NmeaParsed.Unknown::class.java, dispatcher.dispatch(line).javaClass)
    }

    @Test
    fun `corrupted checksum returns ChecksumError`() {
        val line = "\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*FF"
        assertEquals(NmeaParsed.ChecksumError::class.java, dispatcher.dispatch(line).javaClass)
    }

    @Test
    fun `missing star returns ChecksumError`() {
        val parsed = dispatcher.dispatch("\$GPGGA,no,star,here")
        assertEquals(NmeaParsed.ChecksumError::class.java, parsed.javaClass)
    }

    @Test
    fun `non-NMEA prefix becomes Unknown`() {
        assertEquals(NmeaParsed.Unknown::class.java, dispatcher.dispatch("hello world").javaClass)
        assertEquals(NmeaParsed.Unknown::class.java, dispatcher.dispatch("").javaClass)
    }

    @Test
    fun `dispatch handles trailing CRLF`() {
        val payload = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"
        val line = withChecksum(payload) + "\r\n"
        assertNotNull(dispatcher.dispatch(line) as? NmeaParsed.Gga)
    }

    private fun withChecksum(payload: String): String {
        val xor = payload.fold(0) { acc, c -> acc xor c.code }
        return "\$$payload*${"%02X".format(xor)}"
    }
}
