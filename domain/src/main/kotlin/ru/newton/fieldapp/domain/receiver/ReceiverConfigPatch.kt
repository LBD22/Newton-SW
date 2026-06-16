package ru.newton.fieldapp.domain.receiver

import kotlinx.serialization.Serializable

/**
 * Unified model of pending changes to the receiver configuration.
 *
 * Single source of truth: every settings screen that modifies receiver config
 * (rover/base mode, NTRIP, output messages, geoid, ...) adds to this via
 * [PendingChangesService], never sending commands directly.
 *
 * When the user taps Apply, the patch is turned into a sequence of Newton
 * commands (via `toCommands()` in the implementation module) and sent through
 * the `CommandQueue` → `ApplyReceiverConfigUseCase` pipeline.
 *
 * Fields are nullable: null means "don't touch this on apply".
 *
 * `@Serializable` is required for CMD-004 — the patch is persisted in Room as
 * a single JSON blob so pending changes survive process death.
 */
@Serializable
data class ReceiverConfigPatch(
    val roverMode: RoverMode? = null,
    val ppp: PppSetting? = null,
    val surveyMaskDeg: Int? = null,
    val baseMode: BaseMode? = null,
    val rtcmId: Int? = null,
    val cmrId: Int? = null,
    val correctionType: Int? = null,
    val input: InputConfig? = null,
    val outputMessages: List<OutputMessageConfig>? = null,
    val outputStreams: List<OutputStreamConfig>? = null,
    val bluetoothReceiver: Boolean? = null,
    val geoid: GeoidSetting? = null,
    val undulationM: Double? = null,
    /** GSM модем приёмника: вкл/выкл + APN. */
    val gsm: GsmSetting? = null,
    /** Bluetooth bridge (раздел `bluetooth bridge`). */
    val bluetoothBridge: BluetoothBridgeSetting? = null,
)

@Serializable
data class GsmSetting(
    val enabled: Boolean,
    val apn1: String,
    val apn2: String? = null,
)

@Serializable
data class BluetoothBridgeSetting(
    val enabled: Boolean,
    /** Optional target — `null` means just toggle the bridge. */
    val target: BluetoothBridgeTarget? = null,
)

@Serializable
sealed interface BluetoothBridgeTarget {
    @Serializable
    data class TcpServer(val port: Int) : BluetoothBridgeTarget

    @Serializable
    data class TcpClient(val host: String, val port: Int) : BluetoothBridgeTarget

    @Serializable
    data class Com(val index: Int, val baudrate: Int) : BluetoothBridgeTarget

    @Serializable
    data class Uhf(
        val frequencyMHz: Double,
        val protocol: String,
        val baudrate: Int,
        val power: String,
    ) : BluetoothBridgeTarget
}

@Serializable
enum class RoverMode { ROVER, ROVER_BASE, ROVER_MASTER }

@Serializable
data class PppSetting(
    val type: String,
    val enabled: Boolean,
    val sbasSystem: String? = null,
)

@Serializable
data class BaseMode(
    val latitude: Double,
    val longitude: Double,
    val heightM: Double,
)

@Serializable
sealed interface InputConfig {
    @Serializable
    data object Bluetooth : InputConfig

    @Serializable
    data class TcpClient(
        val host: String,
        val port: Int,
    ) : InputConfig

    @Serializable
    data class TcpServer(
        val port: Int,
    ) : InputConfig

    @Serializable
    data class NtripClient(
        val host: String,
        val port: Int,
        val endpoint: String,
        val login: String,
        val password: String,
    ) : InputConfig

    /**
     * NTRIP pulled by the receiver's OWN GSM modem (`input set gsmntripclient`)
     * — no controller internet, no Bluetooth RTCM forwarding. Requires the GSM
     * modem to be enabled with an APN (`gsm set on <apn>`, see [ReceiverConfigPatch.gsm]).
     */
    @Serializable
    data class GsmNtripClient(
        val host: String,
        val port: Int,
        val endpoint: String,
        val login: String,
        val password: String,
    ) : InputConfig

    @Serializable
    data class Com(
        val index: Int,
        val baudrate: Int,
    ) : InputConfig

    @Serializable
    data class Uhf(
        val frequencyMHz: Double,
        val protocol: String,
        val baudrate: Int,
    ) : InputConfig
}

@Serializable
data class OutputMessageConfig(
    val source: String, // M | R1 | R2 | PPP | IMU
    val type: String, // e.g. "GPGGA"
    val rate: String, // "1HZ", "5S", ...
    val format: String, // "A" | "B" | "AB"
)

@Serializable
data class OutputStreamConfig(
    val sources: List<String>,
    val target: StreamTarget,
)

@Serializable
sealed interface StreamTarget {
    @Serializable
    data object Bluetooth : StreamTarget

    @Serializable
    data class TcpClient(
        val host: String,
        val port: Int,
    ) : StreamTarget

    @Serializable
    data class TcpServer(
        val port: Int,
    ) : StreamTarget

    @Serializable
    data class GsmTcpClient(
        val host: String,
        val port: Int,
    ) : StreamTarget

    /** [index] is 1 or 2 — receiver-side COM port number. */
    @Serializable
    data class Com(
        val index: Int,
        val baudrate: Int,
    ) : StreamTarget

    @Serializable
    data class Uhf(
        val frequencyMHz: Double,
        /** Protocol code from [NewtonCommandBuilder.UhfProtocol] (e.g. "trimtalk"). */
        val protocol: String,
        val baudrate: Int,
        /** Power code from [NewtonCommandBuilder.UhfPower] (e.g. "high", "0.5w"). */
        val power: String,
    ) : StreamTarget
}

@Serializable
enum class GeoidSetting { WGS84, EGM86, USER }
