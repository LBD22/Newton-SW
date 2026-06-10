package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import ru.newton.fieldapp.core.logging.AppLog
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocketFactory

/**
 * Raw-socket NTRIP streamer (NTR-004) — provides bidirectional traffic on a
 * single TCP connection: RTCM in (caster → client) and GPGGA out (client →
 * caster) for VRS support.
 *
 * Why not OkHttp: OkHttp's duplex requests require HTTP/2, but the vast
 * majority of NTRIP casters speak HTTP/1.1 only. Owning the socket lets us
 * write GPGGA bytes whenever we want without tripping the "request body
 * complete" assumption. Reconnect/backoff matches [NtripClient] (1s..30s).
 *
 * Lifecycle: [streamMountpoint] returns a cold flow of RTCM `ByteArray`
 * chunks. Cancelling the collector tears down the socket and the GPGGA
 * upload coroutine. The flow runs forever otherwise — auth failures are
 * fatal (no reconnect), socket errors trigger backoff.
 */
class NtripRawStreamer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val log: AppLog,
    private val gpggaProvider: () -> String?,
    private val gpggaIntervalMs: Long = 10_000L,
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
) {
    private val _state = MutableStateFlow<NtripState>(NtripState.Idle)
    val state: StateFlow<NtripState> = _state.asStateFlow()

    fun resetState() { _state.value = NtripState.Idle }

    fun streamMountpoint(profile: NtripProfile): Flow<ByteArray> = flow {
        var attempt = 0
        var bytesTotal = 0L
        while (true) {
            attempt++
            _state.value = NtripState.Connecting(profile.mountpoint, attempt)
            val socket = openSocket(profile)
            if (socket == null) {
                val delayMs = backoffMs(attempt)
                _state.value = NtripState.Reconnecting(profile.mountpoint, delayMs)
                log.ntrip("NTRIP raw connect ${profile.host}:${profile.port} failed; retry in ${delayMs}ms")
                delay(delayMs)
                continue
            }
            var bytesThisConn = 0L
            var noDataReason: String? = null
            try {
                val auth = if (profile.login.isNotEmpty()) {
                    Credentials.basic(profile.login, profile.password)
                } else {
                    null
                }
                val request = buildString {
                    append("GET /").append(profile.mountpoint).append(" HTTP/1.1\r\n")
                    append("Host: ").append(profile.host).append("\r\n")
                    append("Ntrip-Version: Ntrip/2.0\r\n")
                    append("User-Agent: NTRIP NewtonField/1.0\r\n")
                    if (auth != null) append("Authorization: ").append(auth).append("\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
                val out = socket.getOutputStream()
                out.write(request.toByteArray(Charsets.US_ASCII))
                out.flush()

                val input = socket.getInputStream()
                val statusLine = readLine(input)
                if (statusLine == null) {
                    log.ntrip("NTRIP raw: empty response from ${profile.host}")
                    socket.closeQuietly()
                    delay(backoffMs(attempt))
                    continue
                }
                // A wrong mountpoint makes the caster answer with its source
                // table instead of a stream — "SOURCETABLE 200 OK" (Ntrip/1.0)
                // or HTTP 200 with Content-Type: gnss/sourcetable (Ntrip/2.0).
                // The old `contains("200")` check accepted that and streamed the
                // table into the receiver as if it were RTCM. Treat it as a fatal
                // config error so the user picks a valid mountpoint instead.
                if (statusLine.startsWith("SOURCETABLE", ignoreCase = true)) {
                    _state.value = NtripState.Failed(wrongMountpointMessage(profile.mountpoint))
                    log.ntrip("NTRIP raw: SOURCETABLE for /${profile.mountpoint} — mountpoint not found")
                    socket.closeQuietly()
                    return@flow
                }
                // Casters reply either "ICY 200 OK" (Ntrip/1.0) or "HTTP/1.1 200 OK" (Ntrip/2.0).
                if (!statusLine.contains("200")) {
                    val httpCode = statusLine.split(" ").getOrNull(1)?.toIntOrNull()
                    _state.value = NtripState.Failed(statusLine, httpCode = httpCode)
                    log.ntrip("NTRIP raw: $statusLine — aborting (likely auth/mountpoint)")
                    socket.closeQuietly()
                    return@flow
                }
                // Drain remaining headers (until blank line). For Ntrip/1.0 there are none.
                if (statusLine.startsWith("HTTP/")) {
                    var isSourceTable = false
                    while (true) {
                        val line = readLine(input) ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Type:", ignoreCase = true) &&
                            line.contains("sourcetable", ignoreCase = true)
                        ) {
                            isSourceTable = true
                        }
                    }
                    if (isSourceTable) {
                        _state.value = NtripState.Failed(wrongMountpointMessage(profile.mountpoint))
                        log.ntrip("NTRIP raw: gnss/sourcetable for /${profile.mountpoint} — mountpoint not found")
                        socket.closeQuietly()
                        return@flow
                    }
                }
                attempt = 1 // reset backoff on a successful open
                _state.value = NtripState.AwaitingCorrections(profile.mountpoint)

                // Spawn the upstream GPGGA writer; cancellation propagates from caller.
                coroutineScope {
                    val writer = launch(ioDispatcher) { pumpGpgga(out) }
                    try {
                        val buffer = ByteArray(1024)
                        while (true) {
                            val read = withContext(ioDispatcher) { input.read(buffer) }
                            if (read <= 0) break
                            bytesThisConn += read
                            bytesTotal += read
                            _state.value = NtripState.Streaming(
                                mountpoint = profile.mountpoint,
                                bytesReceived = bytesTotal,
                                lastByteAtMs = System.currentTimeMillis(),
                            )
                            emit(buffer.copyOf(read))
                        }
                    } finally {
                        writer.cancel()
                    }
                }
                // Clean EOF with nothing received = caster accepted us but never
                // sent corrections. For a VRS that means it's still waiting for a
                // valid position from us.
                if (bytesThisConn == 0L) noDataReason = noCorrectionsMessage()
            } catch (t: SocketTimeoutException) {
                // soTimeout fired: the stream stalled. Only meaningful as "no
                // data" if we never received any corrections on this connection.
                if (bytesThisConn == 0L) noDataReason = noCorrectionsMessage()
                log.ntrip("NTRIP raw read timeout (${bytesThisConn}B this conn): ${t.message}")
            } catch (t: Throwable) {
                log.ntrip("NTRIP raw stream error: ${t.message}", t)
            } finally {
                socket.closeQuietly()
            }
            val delayMs = backoffMs(attempt)
            // Keep the diagnostic visible during the wait instead of overwriting
            // it with a bare "Reconnecting…" — otherwise the user just sees an
            // endless connect loop with no explanation (the field-report symptom).
            _state.value = if (noDataReason != null) {
                NtripState.Failed(noDataReason)
            } else {
                NtripState.Reconnecting(profile.mountpoint, delayMs)
            }
            delay(delayMs)
        }
    }

    /**
     * Upstream GPGGA pump for VRS. Polls [gpggaProvider] on a short cadence so
     * the first valid position is forwarded within ~1s of a fix appearing
     * (the old fixed 10s tick could skip the caster's initial window and the
     * VRS would drop us after ~30s), then resends every [gpggaIntervalMs].
     */
    private suspend fun pumpGpgga(out: OutputStream) {
        runCatching {
            var lastSentAtMs = 0L
            while (true) {
                val sentence = gpggaProvider()
                val now = System.currentTimeMillis()
                if (sentence != null && (lastSentAtMs == 0L || now - lastSentAtMs >= gpggaIntervalMs)) {
                    out.write(sentence.toByteArray(Charsets.US_ASCII))
                    out.flush()
                    lastSentAtMs = now
                }
                delay(GPGGA_POLL_MS)
            }
        }.onFailure { log.ntrip("GPGGA writer stopped: ${it.message}") }
    }

    private fun wrongMountpointMessage(mountpoint: String) =
        "Mountpoint «$mountpoint» не найден на кастере. Выберите точку из списка."

    private fun noCorrectionsMessage() =
        "Соединение есть, но поправки не поступают. Для VRS нужна позиция (фикс) — дождитесь координат и повторите."

    private fun openSocket(profile: NtripProfile): Socket? = runCatching {
        val plain = Socket()
        plain.connect(InetSocketAddress(profile.host, profile.port), 10_000)
        // Non-zero read timeout so a stalled stream (e.g. a VRS that never gets
        // a position from us) surfaces as a SocketTimeoutException instead of
        // blocking input.read forever and looking "connected" with no data.
        if (profile.useTls) {
            sslSocketFactory.createSocket(plain, profile.host, profile.port, true).also {
                it.soTimeout = READ_TIMEOUT_MS
            }
        } else {
            plain.also { it.soTimeout = READ_TIMEOUT_MS }
        }
    }.getOrNull()

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val ch = input.read()
            if (ch == -1) return if (sb.isEmpty()) null else sb.toString()
            if (ch == '\r'.code) continue
            if (ch == '\n'.code) return sb.toString()
            sb.append(ch.toChar())
        }
    }

    private fun Socket.closeQuietly() = runCatching { close() }

    companion object {
        private const val MAX_BACKOFF_ATTEMPT = 6 // 1, 2, 4, 8, 16, 30s ceiling
        private const val READ_TIMEOUT_MS = 30_000 // stalled-stream detection
        private const val GPGGA_POLL_MS = 1_000L // forward first fix within ~1s

        private fun backoffMs(attempt: Int): Long {
            val capped = attempt.coerceAtMost(MAX_BACKOFF_ATTEMPT)
            val base = 1_000L shl (capped - 1).coerceAtLeast(0)
            return base.coerceAtMost(30_000L)
        }
    }
}
