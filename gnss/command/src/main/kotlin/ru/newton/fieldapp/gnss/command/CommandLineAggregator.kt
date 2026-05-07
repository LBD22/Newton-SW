package ru.newton.fieldapp.gnss.command

import java.nio.charset.StandardCharsets

/**
 * Splits the inbound byte stream from CommandSPP into complete reply lines.
 *
 * Functionally identical to the NMEA aggregator, but lives here so `:gnss:data`
 * doesn't leak into `:gnss:command` (and so changes to one pipeline don't
 * accidentally affect the other). Newton replies terminate with `\r\n`; the
 * aggregator also accepts a bare `\n` to be defensive on flaky USB-Serial
 * adaptors that occasionally drop the carriage return.
 *
 * Not thread-safe by design — feed it sequentially inside one collector.
 */
internal class CommandLineAggregator {
    private val buffer = StringBuilder()

    fun feed(chunk: ByteArray): List<String> {
        if (buffer.length + chunk.size > MAX_PENDING_BYTES) {
            buffer.setLength(0)
        }
        buffer.append(String(chunk, StandardCharsets.US_ASCII))
        return drain()
    }

    fun reset() { buffer.setLength(0) }

    private fun drain(): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var lineStart = 0
        var i = 0
        while (i < buffer.length) {
            if (buffer[i] == '\n') {
                val endExclusive = if (i > 0 && buffer[i - 1] == '\r') i - 1 else i
                if (endExclusive > lineStart) {
                    out += buffer.substring(lineStart, endExclusive)
                }
                lineStart = i + 1
            }
            i++
        }
        if (lineStart > 0) buffer.delete(0, lineStart)
        return out
    }

    companion object {
        const val MAX_PENDING_BYTES = 16 * 1024 // commands are small; 16 KiB ≫ any reply
    }
}
