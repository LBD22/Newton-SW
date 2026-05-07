package ru.newton.fieldapp.gnss.data.parsers

import java.nio.charset.StandardCharsets

/**
 * Splits an inbound byte stream from DataSPP into complete NMEA lines.
 *
 * Bytes arrive in arbitrary chunk sizes (typical Bluetooth read is ~32 bytes
 * per `read()`); the receiver guarantees only that lines are terminated with
 * `\r\n` (or `\n`). This aggregator buffers partial lines across [feed] calls.
 *
 * Not thread-safe by design — wire it inside one Flow collector and feed it
 * sequentially. Memory cap [MAX_PENDING_BYTES] guards against a stuck stream
 * with no terminator chewing through phone RAM.
 */
class NmeaLineAggregator {
    private val buffer = StringBuilder()

    /** Push a chunk; emits all newly-complete lines (without the terminator). */
    fun feed(chunk: ByteArray): List<String> {
        if (buffer.length + chunk.size > MAX_PENDING_BYTES) {
            // Stream went rogue — drop the partial buffer and surface the most
            // recent fragment so the caller can log a malformed line.
            buffer.setLength(0)
        }
        buffer.append(String(chunk, StandardCharsets.US_ASCII))
        return drainLines()
    }

    /** Used by tests and reset paths. */
    fun reset() { buffer.setLength(0) }

    private fun drainLines(): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var lineStart = 0
        var i = 0
        while (i < buffer.length) {
            val c = buffer[i]
            if (c == '\n') {
                val endExclusive = if (i > 0 && buffer[i - 1] == '\r') i - 1 else i
                if (endExclusive > lineStart) {
                    lines += buffer.substring(lineStart, endExclusive)
                }
                lineStart = i + 1
            }
            i++
        }
        if (lineStart > 0) buffer.delete(0, lineStart)
        return lines
    }

    companion object {
        const val MAX_PENDING_BYTES = 64 * 1024
    }
}
