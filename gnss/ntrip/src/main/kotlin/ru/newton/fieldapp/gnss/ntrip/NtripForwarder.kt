package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.bluetooth.CommandSpp
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the NTRIP RTCM stream to the receiver via [CommandSpp]`.write(bytes)`.
 *
 * The Newton receiver has a single SPP socket; the `@CommandSpp` qualifier is a
 * semantic role tag, not a separate channel. The receiver still expects RTCM
 * to arrive after `input set bluetooth` — see `docs/protocol-newton.md`
 * § RTCM flow.
 *
 * NTR-004 (GPGGA upstream): we use [NtripRawStreamer] (raw socket) instead of
 * OkHttp so the same TCP connection can carry our synthesised GPGGA upstream
 * — VRS casters require this to drive the virtual reference station. The
 * sentence is built from [GnssStatusStore.status] every 10s; if there is no
 * fix yet, no GPGGA is sent and the caster falls back to a generic mountpoint.
 *
 * Lifecycle: [start] is idempotent — calling with a different profile cancels
 * the previous stream first. [stop] tears down the forwarder without touching
 * the underlying transports.
 */
@Singleton
class NtripForwarder(
    private val commandSpp: SppTransport,
    private val statusStore: GnssStatusStore,
    private val log: AppLog,
    ioDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(
        @CommandSpp commandSpp: SppTransport,
        statusStore: GnssStatusStore,
        log: AppLog,
    ) : this(commandSpp, statusStore, log, Dispatchers.IO)

    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(parentJob + ioDispatcher)
    private var streamJob: Job? = null
    private val streamer = NtripRawStreamer(
        ioDispatcher = ioDispatcher,
        log = log,
        gpggaProvider = { GpggaBuilder.fromStatus(statusStore.status.value) },
    )

    val state: StateFlow<NtripState> = streamer.state

    @Volatile
    var activeProfile: NtripProfile? = null
        private set

    fun start(profile: NtripProfile) {
        if (activeProfile?.id == profile.id && streamJob?.isActive == true) return
        // Cancel AND force-close the old socket so the previous TCP connection
        // tears down immediately. Otherwise it lingers up to the 30 s read timeout
        // and the caster sees two logins with the same credentials — VRS casters
        // reject the second, so the *new* profile inexplicably fails to stream.
        streamJob?.cancel()
        streamer.closeActiveSocket()
        activeProfile = profile
        streamJob = scope.launch {
            log.ntrip("Forwarding ${profile.host}/${profile.mountpoint} → SPP (GPGGA upstream on)")
            streamer.streamMountpoint(profile).collect { chunk ->
                if (commandSpp.linkState.value is LinkState.Connected) {
                    runCatching { commandSpp.write(chunk) }
                        .onFailure { log.ntrip("SPP.write failed: ${it.message}", it) }
                } else {
                    // Drop — when the BT link comes back, future chunks land on it.
                    log.ntrip("Dropped ${chunk.size}B RTCM: link=${commandSpp.linkState.value}")
                }
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamer.closeActiveSocket()
        streamJob = null
        activeProfile = null
        streamer.resetState()
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }
}
