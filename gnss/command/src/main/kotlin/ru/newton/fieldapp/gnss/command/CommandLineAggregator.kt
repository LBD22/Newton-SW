package ru.newton.fieldapp.gnss.command

import java.nio.charset.StandardCharsets

/**
 * Splits the inbound byte stream from CommandSPP into complete reply lines.
 *
 * Functionally identical to the NMEA aggregator, but lives here so `:gnss:data`
 * doesn't leak into `:gnss:command` (and so changes to one pipeline don't
 * accidentally affect the other). Newton replies terminate with `\r\n`, but the
 * aggregator accepts any of `\r\n`, `\n`, or a bare `\r` as a line terminator —
 * some receivers/serial stacks end command-port replies with a lone CR, which
 * (if we only split on `\n`) would leave the `OK` stuck in the buffer forever
 * and make the handshake time out with "No reply for: AT".
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
            val c = buffer[i]
            if (c == '\n' || c == '\r') {
                // Non-empty line ends here; empty lines (back-to-back terminators)
                // are dropped — command callers never need them.
                if (i > lineStart) out += buffer.substring(lineStart, i)
                // Treat a `\r\n` pair as a single terminator.
                if (c == '\r' && i + 1 < buffer.length && buffer[i + 1] == '\n') i++
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
