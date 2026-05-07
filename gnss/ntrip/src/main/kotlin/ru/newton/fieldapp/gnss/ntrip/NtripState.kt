package ru.newton.fieldapp.gnss.ntrip

sealed interface NtripState {
    data object Idle : NtripState

    data class FetchingSourceTable(
        val host: String,
        val port: Int,
    ) : NtripState

    data class Connecting(
        val mountpoint: String,
        val attempt: Int,
    ) : NtripState

    data class Streaming(
        val mountpoint: String,
        val bytesReceived: Long,
        val lastByteAtMs: Long,
    ) : NtripState

    data class Reconnecting(
        val mountpoint: String,
        val nextAttemptInMs: Long,
    ) : NtripState

    data class Failed(
        val reason: String,
        val httpCode: Int? = null,
    ) : NtripState
}

/**
 * Entry in an NTRIP caster source table.
 * Parsed from rows starting with `STR;` in the source-table GET response.
 */
data class MountpointInfo(
    val id: String,
    val format: String, // "RTCM 3.2", "CMR", ...
    val carrier: String,
    val navSystem: String, // "GPS+GLO+GAL+BDS"
    val network: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?,
    val nmea: Boolean, // requires GGA from client
    val solution: Int,            // 0 single, 1 network
)
