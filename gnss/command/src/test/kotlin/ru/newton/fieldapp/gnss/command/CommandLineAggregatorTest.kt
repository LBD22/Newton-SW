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
}
