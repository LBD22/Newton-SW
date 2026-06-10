package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.newton.fieldapp.core.logging.AppLog
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

/**
 * NTRIP client (NTR-001/002/003) over OkHttp 5.
 *
 * Two surfaces:
 *  - [fetchSourceTable] — one-shot GET against the caster root, returns the
 *    raw text body that [SourceTableParser] consumes.
 *  - [streamMountpoint] — long-running GET against `host:port/<mountpoint>`,
 *    emits raw RTCM bytes as `Flow<ByteArray>` chunks.
 *
 * Reconnect (NTR-003): on socket exception we transition through
 * `Reconnecting(nextAttemptInMs)` and retry with exponential backoff
 * 1s..30s ceiling, until the consumer cancels the flow.
 *
 * Per `docs/protocol-newton.md` § RTCM flow, the receiver only accepts RTCM
 * after `input set bluetooth` is configured. The byte stream from this client
 * is handed to [NtripForwarder], which writes to the shared SPP transport.
 */
@Singleton
class NtripClient(
    private val httpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val log: AppLog,
) {
    @Inject
    constructor(log: AppLog) : this(buildHttpClient(), Dispatchers.IO, log)

    private val _state = MutableStateFlow<NtripState>(NtripState.Idle)
    val state: StateFlow<NtripState> = _state.asStateFlow()

    /**
     * One-shot GET of the caster source-table, returned raw for
     * [SourceTableParser.parse]. Throws on transport failure.
     *
     * Uses a raw socket rather than OkHttp on purpose: Ntrip/1.0 casters answer
     * the source-table request with a `SOURCETABLE 200 OK` status line, which is
     * NOT valid HTTP — OkHttp rejects it ("connection closed") before we can read
     * the body. The raw socket reads whatever the caster sends; the parser only
     * keeps `STR;` rows, so the leading status line / HTTP headers are harmless.
     * Basic auth is included when the profile has credentials — some casters
     * close the connection on an unauthenticated source-table request.
     */
    suspend fun fetchSourceTable(
        host: String,
        port: Int,
        useTls: Boolean = true,
        login: String = "",
        password: String = "",
    ): String = withContext(ioDispatcher) {
        _state.value = NtripState.FetchingSourceTable(host, port)
        var socket: Socket? = null
        try {
            val plain = Socket()
            plain.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            plain.soTimeout = SOURCETABLE_TIMEOUT_MS
            socket = if (useTls) {
                (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(plain, host, port, true)
            } else {
                plain
            }
            val auth = if (login.isNotEmpty()) Credentials.basic(login, password) else null
            val request = buildString {
                append("GET / HTTP/1.0\r\n")
                append("Host: ").append(host).append("\r\n")
                append("Ntrip-Version: Ntrip/2.0\r\n")
                append("User-Agent: ").append(USER_AGENT).append("\r\n")
                if (auth != null) append("Authorization: ").append(auth).append("\r\n")
                append("\r\n")
            }
            socket.getOutputStream().apply {
                write(request.toByteArray(Charsets.US_ASCII))
                flush()
            }
            // Caster sends the table then closes (or ENDSOURCETABLE) — read to EOF.
            val body = socket.getInputStream().readBytes().toString(Charsets.ISO_8859_1)
            _state.value = NtripState.Idle
            body
        } catch (t: Throwable) {
            _state.value = NtripState.Failed(t.message ?: "Source-table fetch failed", null)
            log.ntrip("NTRIP source-table $host:$port failed: ${t.message}")
            throw t
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Stream RTCM bytes from a mountpoint. Returns a cold [Flow] that:
     *  1. opens an HTTP connection with Basic auth,
     *  2. reads the response body in [READ_CHUNK]-byte chunks,
     *  3. emits each chunk to the collector.
     *
     * On any IO error the flow waits the exponential-backoff delay then
     * retries — only a coroutine cancellation breaks out.
     */
    fun streamMountpoint(profile: NtripProfile): Flow<ByteArray> = flow {
        var attempt = 0
        while (true) {
            attempt++
            _state.value = NtripState.Connecting(profile.mountpoint, attempt)
            val request = Request.Builder()
                .url("${profile.scheme}://${profile.host}:${profile.port}/${profile.mountpoint}")
                .header("User-Agent", USER_AGENT)
                .header("Ntrip-Version", "Ntrip/2.0")
                .header("Authorization", Credentials.basic(profile.login, profile.password))
                .build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        log.ntrip("NTRIP ${profile.mountpoint} → HTTP ${response.code}")
                        _state.value = NtripState.Failed(
                            "Caster: HTTP ${response.code}",
                            httpCode = response.code,
                        )
                        // 401 / 403 — credentials problem, no reconnect.
                        if (response.code in 400..499 && response.code != 408 && response.code != 429) return@flow
                    } else {
                        attempt = 1 // reset on a successful open
                        var bytesTotal = 0L
                        val input = response.body.byteStream()
                        val buffer = ByteArray(READ_CHUNK)
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
                    }
                }
            } catch (t: Throwable) {
                log.ntrip("NTRIP stream interrupted: ${t.message}", t)
            }
            val delayMs = backoffMs(attempt)
            _state.value = NtripState.Reconnecting(profile.mountpoint, delayMs)
            delay(delayMs)
        }
    }

    fun resetState() { _state.value = NtripState.Idle }

    companion object {
        private const val USER_AGENT = "NTRIP NewtonField/1.0"
        private const val READ_CHUNK = 1024
        private const val MAX_BACKOFF_ATTEMPT = 6 // 1, 2, 4, 8, 16, 30s ceiling
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val SOURCETABLE_TIMEOUT_MS = 15_000

        fun buildHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // streaming response — no read timeout
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        private fun backoffMs(attempt: Int): Long {
            val capped = attempt.coerceAtMost(MAX_BACKOFF_ATTEMPT)
            val base = 1_000L shl (capped - 1).coerceAtLeast(0)
            return base.coerceAtMost(30_000L)
        }
    }
}
