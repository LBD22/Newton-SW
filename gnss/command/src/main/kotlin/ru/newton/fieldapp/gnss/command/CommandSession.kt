package ru.newton.fieldapp.gnss.command

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import ru.newton.fieldapp.core.bluetooth.CommandSpp
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.core.logging.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bidirectional Newton command port session over the shared SPP transport.
 *
 * The receiver multiplexes NMEA frames and command replies on the same socket,
 * so this session sees both. `awaitOk`/`awaitInfoLine` ignore NMEA noise (lines
 * starting with `$`) and only honour the four OK-class tokens (CMD-002) or
 * plain-text info lines.
 *
 * Serialises outbound commands through [sendMutex] so callers cannot
 * interleave a `system save` between an `output add message …` and its `OK!`
 * reply.
 *
 * Threading: read-loop runs on `Dispatchers.IO` via [scope]; [send] suspends
 * the calling coroutine until a reply is matched or [REPLY_TIMEOUT_MS]
 * elapses.
 */
@Singleton
class CommandSession(
    private val transport: SppTransport,
    private val log: AppLog,
    ioDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(
        @CommandSpp transport: SppTransport,
        log: AppLog,
    ) : this(transport, log, Dispatchers.IO)

    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(parentJob + ioDispatcher)
    private val sendMutex = Mutex()
    private val replyChannel = Channel<String>(capacity = Channel.UNLIMITED)
    private val aggregator = CommandLineAggregator()

    private val _state = MutableStateFlow<CommandSessionState>(CommandSessionState.Idle)
    val state: StateFlow<CommandSessionState> = _state.asStateFlow()

    private var readerJob: Job? = null

    /** Start consuming bytes from the transport. Idempotent. */
    fun start() {
        if (readerJob?.isActive == true) return
        readerJob = scope.launch {
            transport.incoming.collect { chunk ->
                aggregator.feed(chunk).forEach { line ->
                    // Log every command-port line (NMEA `$…` excluded) so the
                    // diagnostics log shows exactly what the receiver replies
                    // during a handshake — invaluable when AT appears to hang.
                    if (line.isNotBlank() && !line.startsWith("$")) log.cmd("← $line")
                    replyChannel.trySend(line)
                }
            }
        }
    }

    /** Stop consuming and return to Idle. The caller usually triggers this on disconnect. */
    fun stop() {
        readerJob?.cancel()
        readerJob = null
        aggregator.reset()
        // Drain leftover replies — they belong to the previous session.
        while (replyChannel.tryReceive().isSuccess) Unit
        _state.value = CommandSessionState.Idle
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    /**
     * Run the canonical handshake:
     *
     *   AT → OK
     *   get command mode → "on" or "off"
     *   if off → set command mode on → OK+
     *
     * Throws on transport error or timeout. On success [state] becomes [CommandSessionState.Ready].
     */
    suspend fun handshake() {
        check(transport.linkState.value is LinkState.Connected) {
            "CommandSPP not connected — link=${transport.linkState.value}"
        }
        _state.value = CommandSessionState.Handshaking
        try {
            // Hold the mutex across the entire handshake so other senders
            // can't slip a command between AT and `set command mode on`.
            sendMutex.withLock {
                require(sendUnderLock(NewtonCommandBuilder.at()) == OkKind.OK_HANDSHAKE) {
                    "AT did not return OK"
                }
                val modeText = sendForLineUnderLock(NewtonCommandBuilder.getCommandMode())
                if (!modeText.contains("on", ignoreCase = true)) {
                    require(sendUnderLock(NewtonCommandBuilder.setCommandModeOn()) == OkKind.OK_MODE_ON) {
                        "set command mode on did not return OK+"
                    }
                }
            }
            _state.value = CommandSessionState.Ready
        } catch (t: Throwable) {
            log.cmd("Handshake failed: ${t.message}", t)
            _state.value = CommandSessionState.Failed(t.message ?: "Handshake error")
            throw t
        }
    }

    /**
     * Send [command] (without trailing CRLF) and wait for the next OK-class reply.
     * Returns the [OkKind] variant the receiver replied with.
     */
    suspend fun send(command: String): OkKind = sendMutex.withLock { sendUnderLock(command) }

    /**
     * Send a command that is expected to reply with an info line (e.g. `get command mode`).
     * Returns the first non-blank line received before any OK token.
     */
    suspend fun sendForLine(command: String): String =
        sendMutex.withLock { sendForLineUnderLock(command) }

    /** Caller MUST already hold [sendMutex]. */
    private suspend fun sendUnderLock(command: String): OkKind {
        sendInternal(command)
        return awaitOk(command) ?: error("No reply within ${REPLY_TIMEOUT_MS}ms for: $command")
    }

    /** Caller MUST already hold [sendMutex]. */
    private suspend fun sendForLineUnderLock(command: String): String {
        sendInternal(command)
        return awaitInfoLine(command)
            ?: error("No info line within ${REPLY_TIMEOUT_MS}ms for: $command")
    }

    private suspend fun sendInternal(command: String) {
        log.cmd("→ $command")
        transport.write((command + "\r\n").toByteArray(Charsets.US_ASCII))
    }

    private suspend fun awaitOk(originalCommand: String): OkKind? = withTimeoutOrNull(REPLY_TIMEOUT_MS) {
        while (true) {
            val line = replyChannel.receive().trim()
            if (line.isEmpty() || line == originalCommand || line.startsWith("$")) continue
            val ok = OkKind.fromToken(line)
            if (ok != null) return@withTimeoutOrNull ok
            // Otherwise a leading info line — keep waiting for the OK that follows.
            log.cmd("← (info) $line")
        }
        @Suppress("UNREACHABLE_CODE")
        null
    }

    private suspend fun awaitInfoLine(originalCommand: String): String? = withTimeoutOrNull(REPLY_TIMEOUT_MS) {
        while (true) {
            val line = replyChannel.receive().trim()
            // Skip blanks, our own echo, and NMEA frames — only command-port
            // text replies count as info lines.
            if (line.isEmpty() || line == originalCommand || line.startsWith("$")) continue
            if (OkKind.fromToken(line) != null) {
                // No info line preceded the OK — return the OK token itself for diagnostics.
                return@withTimeoutOrNull line
            }
            return@withTimeoutOrNull line
        }
        @Suppress("UNREACHABLE_CODE")
        null
    }

    companion object {
        internal const val REPLY_TIMEOUT_MS = 3_000L
    }
}
