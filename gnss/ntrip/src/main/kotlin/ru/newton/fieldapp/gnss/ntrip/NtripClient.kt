package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import ru.newton.fieldapp.core.logging.AppLog
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

/**
 * NTRIP source-table client (NTR-002).
 *
 * [fetchSourceTable] does a one-shot GET against the caster root and returns the
 * raw text body that [SourceTableParser] consumes. RTCM streaming lives in
 * [NtripRawStreamer] (raw socket, so the same connection can upstream GPGGA for
 * VRS) — this class is the mountpoint-list fetcher only.
 */
@Singleton
class NtripClient(
    private val ioDispatcher: CoroutineDispatcher,
    private val log: AppLog,
) {
    @Inject
    constructor(log: AppLog) : this(Dispatchers.IO, log)

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
        useTls: Boolean = false,
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
            val auth = if (login.isNotEmpty()) Credentials.basic(login, password, Charsets.UTF_8) else null
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
            // Decode UTF-8 (strict superset of ASCII): Russian casters publish
            // mountpoint networks/identifiers in UTF-8, which Latin-1 turned into
            // mojibake in the picker.
            val body = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
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

    companion object {
        private const val USER_AGENT = "NTRIP NewtonField/1.0"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val SOURCETABLE_TIMEOUT_MS = 15_000
    }
}
