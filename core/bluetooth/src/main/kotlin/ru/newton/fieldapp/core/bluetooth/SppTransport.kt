package ru.newton.fieldapp.core.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracted Bluetooth SPP (Serial Port Profile) transport.
 *
 * This interface has two named instances in the Hilt graph:
 *  - `@DataSpp`    — receiver → phone stream of NMEA/RTCM sentences
 *  - `@CommandSpp` — bidirectional channel for Newton commands AND RTCM writes
 *
 * See `docs/protocol-newton.md` § Bluetooth channels for why there are two,
 * and `docs/protocol-newton.md` § RTCM flow for why RTCM goes to CommandSpp
 * and not DataSpp.
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
 * Hilt qualifier for the data SPP channel (receiver → phone).
 * Use when you need to inject the phone-receiving side of the link.
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DataSpp

/**
 * Hilt qualifier for the command SPP channel (bidirectional; also carries RTCM
 * from phone to receiver — see `docs/protocol-newton.md`).
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CommandSpp
