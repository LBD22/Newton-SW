package ru.newton.fieldapp.gnss.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CommandLineAggregatorTest {
    @Test
    fun `splits CRLF replies`() {
        val agg = CommandLineAggregator()
        val out = agg.feed("OK\r\nOK+\r\n".toByteArray())
        assertEquals(listOf("OK", "OK+"), out)
    }

    @Test
    fun `accepts bare LF`() {
        val agg = CommandLineAggregator()
        assertEquals(listOf("OK!"), agg.feed("OK!\n".toByteArray()))
    }

    @Test
    fun `accepts bare CR terminator`() {
        val agg = CommandLineAggregator()
        assertEquals(listOf("OK"), agg.feed("OK\r".toByteArray()))
    }

    @Test
    fun `splits multiple CR-terminated replies`() {
        val agg = CommandLineAggregator()
        assertEquals(listOf("OK", "OK+"), agg.feed("OK\rOK+\r".toByteArray()))
    }

    @Test
    fun `holds partial reply across feeds`() {
        val agg = CommandLineAggregator()
        assertEquals(emptyList<String>(), agg.feed("O".toByteArray()))
        assertEquals(emptyList<String>(), agg.feed("K".toByteArray()))
        assertEquals(listOf("OK"), agg.feed("\r\n".toByteArray()))
    }

    @Test
    fun `oversize buffer without terminator is dropped`() {
        val agg = CommandLineAggregator()
        val flood = ByteArray(CommandLineAggregator.MAX_PENDING_BYTES + 100) { 'X'.code.toByte() }
        assertEquals(emptyList<String>(), agg.feed(flood))
        assertEquals(listOf("OK"), agg.feed("OK\r\n".toByteArray()))
    }

    @Test
    fun `binary frame before reply does not corrupt the reply`() {
        // Binary GSOF frame arrives without a CR/LF terminator (bytes 0x80+ and
        // control chars), followed by the receiver's "OK" in the next feed call.
        // Before the fix, the aggregator kept binary garbage in the buffer and
        // produced "[garbage]OK" — which OkKind could not match, causing the
        // "No reply within 3000ms for: AT" field error.
        val agg = CommandLineAggregator()
        val binaryFrame = byteArrayOf(
            0xD0.toByte(),
            0x2A,
            0x01,
            0x07,  // non-printable: triggers taint
            0x4F,
            0x4B,
            0x3F.toByte(),
            0x44,  // 'O','K','?','D' — skipped while tainted
        )
        assertEquals(emptyList<String>(), agg.feed(binaryFrame))
        assertEquals(listOf("OK"), agg.feed("OK\r\n".toByteArray()))
    }

    @Test
    fun `binary frame with CRLF resets taint mid-frame`() {
        // If binary data happens to contain CR/LF, the taint resets there.
        // Bytes after the CR/LF in the same chunk should be parsed cleanly.
        val agg = CommandLineAggregator()
        val mixed = byteArrayOf(
            0xD0.toByte(),
            0x0A,        // non-printable → taint, then LF resets taint
            'O'.code.toByte(),
            'K'.code.toByte(),
            '\r'.code.toByte(),
            '\n'.code.toByte(),
        )
        assertEquals(listOf("OK"), agg.feed(mixed))
    }
}
