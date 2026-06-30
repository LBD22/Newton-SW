package ru.newton.fieldapp.gnss.command

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
 * Bytes are processed one at a time. Non-printable, non-CR/LF bytes (binary
 * GSOF/NovAtel frames) taint the current line: the line is discarded and the
 * buffer is cleared. If the taint carries over to the next [feed] call, the
 * buffer is cleared before processing new bytes. This prevents binary frame
 * fragments from being silently concatenated with the next command reply (e.g.
 * a GSOF frame arriving just before the "OK" response would produce "[gsof]OK",
 * which [OkKind.fromToken] cannot match — field symptom: "No reply for: AT").
 *
 * Not thread-safe by design — feed it sequentially inside one collector.
 */
internal class CommandLineAggregator {
    private val buffer = StringBuilder()

    /**
     * True when the current partial line contains at least one non-printable,
     * non-CR/LF byte. The tainted line is discarded at the next CR/LF or at
     * the start of the next [feed] call (whichever comes first).
     */
    private var lineTainted = false

    fun feed(chunk: ByteArray): List<String> {
        // If the previous feed ended mid-binary-frame (no CR/LF to close it),
        // throw away the leftover bytes now. If we kept them, a clean reply like
        // "OK\r\n" in this feed would be appended to garbage and unrecognisable.
        if (lineTainted) {
            buffer.setLength(0)
            lineTainted = false
        }
        if (buffer.length + chunk.size > MAX_PENDING_BYTES) {
            buffer.setLength(0)
        }
        for (b in chunk) {
            val c = b.toInt() and 0xFF
            when {
                c == '\r'.code || c == '\n'.code -> {
                    if (lineTainted) {
                        // End of a tainted line — discard it and reset for the next.
                        buffer.setLength(0)
                        lineTainted = false
                    } else {
                        buffer.append(c.toChar())
                    }
                }
                c in 0x20..0x7E -> {
                    // Printable ASCII — accumulate only when not tainted.
                    if (!lineTainted && buffer.length < MAX_PENDING_BYTES) {
                        buffer.append(c.toChar())
                    }
                }
                else -> {
                    // Non-printable, non-CR/LF byte: binary protocol data (GSOF,
                    // NovAtel binary). Taint this line and discard what we have.
                    lineTainted = true
                    buffer.setLength(0)
                }
            }
        }
        return drain()
    }

    fun reset() {
        buffer.setLength(0)
        lineTainted = false
    }

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
