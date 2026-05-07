package ru.newton.fieldapp.gnss.data.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NmeaLineAggregatorTest {
    @Test
    fun `splits on CRLF`() {
        val agg = NmeaLineAggregator()
        val out = agg.feed("\$GPGGA,1,2,3*FF\r\n\$GPGST,1,2*EE\r\n".toByteArray())
        assertEquals(2, out.size)
        assertEquals("\$GPGGA,1,2,3*FF", out[0])
        assertEquals("\$GPGST,1,2*EE", out[1])
    }

    @Test
    fun `holds partial line across feed calls`() {
        val agg = NmeaLineAggregator()
        // First chunk: partial, no terminator.
        var out = agg.feed("\$GPGGA,1,2".toByteArray())
        assertEquals(0, out.size)
        // Second chunk completes the line and starts a new one.
        out = agg.feed(",3*FF\r\n\$GPGST".toByteArray())
        assertEquals(1, out.size)
        assertEquals("\$GPGGA,1,2,3*FF", out[0])
        // Third chunk completes the second line.
        out = agg.feed(",1,2*EE\n".toByteArray())
        assertEquals(1, out.size)
        assertEquals("\$GPGST,1,2*EE", out[0])
    }

    @Test
    fun `bare LF without CR also delimits`() {
        val agg = NmeaLineAggregator()
        val out = agg.feed("\$A*00\n\$B*00\n".toByteArray())
        assertEquals(listOf("\$A*00", "\$B*00"), out)
    }

    @Test
    fun `oversize buffer without terminator is dropped`() {
        val agg = NmeaLineAggregator()
        // Feed over the cap with no newline.
        val flood = ByteArray(NmeaLineAggregator.MAX_PENDING_BYTES + 100) { 'X'.code.toByte() }
        val out = agg.feed(flood)
        assertEquals(0, out.size)
        // Buffer should have reset; a subsequent valid chunk parses cleanly.
        val recovered = agg.feed("\$GPGGA,1*FF\n".toByteArray())
        assertEquals(1, recovered.size)
    }
}
