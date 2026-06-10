package ru.newton.fieldapp.core.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracted Bluetooth SPP (Serial Port Profile) transport.
 *
 * The Newton receiver exposes a **single** RFCOMM channel — NMEA/RTCM downstream
 * and the text command pipe share that one socket; the firmware multiplexes by
 * frame type. There is one transport singleton in the Hilt graph, surfaced
 * under two semantic qualifiers:
 *
 *  - `@DataSpp`    — semantic role for "I'm a downstream NMEA/RTCM consumer".
 *  - `@CommandSpp` — semantic role for "I'm the command/RTCM-upstream producer".
 *
 * Both qualifiers resolve to the same instance. The split exists only as a
 * call-site readability convention and so that future re-architecture can wrap
 * each role behind its own facade without touching every consumer.
 *
 * See `docs/protocol-newton.md` § Bluetooth channel for why there is one socket
 * and how frames are demuxed by the parsers above this layer.
 *
 * Implementations handle reconnect with exponential backoff, foreground service
 * coordination, and Android 12+ runtime permissions.
 */
interface SppTransport {
    /** Live state of the transport. Re-emits on every transition. */
    val linkState: StateFlow<LinkState>

    /** Raw bytes as they arrive. Consumers parse into whatever protocol they expect. */
    val incoming: Flow<ByteArray>

    /**
     * Connect to a paired device by MAC address. Suspends until connected or
     * throws if the underlying socket cannot be opened (after retries).
     * Idempotent on repeated calls with the same address.
     */
    suspend fun connect(deviceAddress: String)

    /** Disconnect cleanly. Idempotent. */
    suspend fun disconnect()

    /**
     * Write bytes to the channel.
     * Throws `IllegalStateException` if the link is not Connected.
     */
    suspend fun write(bytes: ByteArray)
}

/**
 * Hilt qualifier marking the downstream NMEA/RTCM role. Resolves to the same
 * underlying transport as [CommandSpp]; the tag is for call-site readability.
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DataSpp

/**
 * Hilt qualifier marking the command + RTCM-upstream role. Resolves to the same
 * underlying transport as [DataSpp]; the tag is for call-site readability.
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CommandSpp
