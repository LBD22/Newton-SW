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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import ru.newton.fieldapp.core.logging.AppLog
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
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
                // Casters reply either "ICY 200 OK" (Ntrip/1.0) or "HTTP/1.1 200 OK" (Ntrip/2.0).
                val ok = statusLine.contains("200")
                if (!ok) {
                    val httpCode = statusLine.split(" ").getOrNull(1)?.toIntOrNull()
                    _state.value = NtripState.Failed(statusLine, httpCode = httpCode)
                    log.ntrip("NTRIP raw: $statusLine — aborting (likely auth/mountpoint)")
                    socket.closeQuietly()
                    return@flow
                }
                // Drain remaining headers (until blank line). For Ntrip/1.0 there are none.
                if (statusLine.startsWith("HTTP/")) {
                    while (true) {
                        val line = readLine(input) ?: break
                        if (line.isEmpty()) break
                    }
                }
                attempt = 1 // reset backoff on a successful open

                // Spawn the upstream GPGGA writer; cancellation propagates from caller.
                coroutineScope {
                    val writer = launch(ioDispatcher) {
                        runCatching {
                            while (isActive) {
                                gpggaProvider()?.let { sentence ->
                                    out.write(sentence.toByteArray(Charsets.US_ASCII))
                                    out.flush()
                                }
                                delay(gpggaIntervalMs)
                            }
                        }.onFailure { log.ntrip("GPGGA writer stopped: ${it.message}") }
                    }
                    try {
                        val buffer = ByteArray(1024)
                        while (true) {
                            val read = withContext(ioDispatcher) { input.read(buffer) }
                            if (read <= 0) break
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
            } catch (t: Throwable) {
                log.ntrip("NTRIP raw stream error: ${t.message}", t)
            } finally {
                socket.closeQuietly()
            }
            val delayMs = backoffMs(attempt)
            _state.value = NtripState.Reconnecting(profile.mountpoint, delayMs)
            delay(delayMs)
        }
    }

    private fun openSocket(profile: NtripProfile): Socket? = runCatching {
        val plain = Socket()
        plain.connect(InetSocketAddress(profile.host, profile.port), 10_000)
        if (profile.useTls) {
            sslSocketFactory.createSocket(plain, profile.host, profile.port, true).also {
                it.soTimeout = 0
            }
        } else {
            plain.also { it.soTimeout = 0 }
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

        private fun backoffMs(attempt: Int): Long {
            val capped = attempt.coerceAtMost(MAX_BACKOFF_ATTEMPT)
            val base = 1_000L shl (capped - 1).coerceAtLeast(0)
            return base.coerceAtMost(30_000L)
        }
    }
}
