package ru.newton.fieldapp.gnss.command

/**
 * Type-safe builder for Newton receiver commands.
 *
 * ALL Newton command strings in this codebase must be produced by this object.
 * Direct concatenation (`"mode set base $lat $lon $h"`) in feature or domain
 * code is a protocol-correctness bug — it bypasses escaping, case handling,
 * and the critical U+2502 source separator.
 *
 * See `docs/protocol-newton.md` and `reference/OSdoc_command_port_*.docx` for
 * the full command reference.
 */
object NewtonCommandBuilder {
    /**
     * The ONLY correct source separator. This is U+2502 (BOX DRAWINGS LIGHT
     * VERTICAL), NOT U+007C (ASCII pipe |). Hard-coding `|` causes the
     * receiver to silently reject the stream/message command.
     */
    const val SRC_SEP = '│'

    // ---- system ----
    fun systemHelp(): String = "system help"

    fun systemList(): String = "system list"

    fun systemRemove(index: Int): String = "system remove $index"

    fun systemClear(): String = "system clear"

    fun systemSave(): String = "system save"

    // ---- command mode ----
    fun at(): String = "AT"

    fun getCommandMode(): String = "get command mode"

    fun setCommandModeOn(): String = "set command mode on"

    fun setCommandModeOff(): String = "set command mode off"

    // ---- eth0 (receiver-side network config) ----
    fun eth0SetDhcp(): String = "eth0 set dhcp"

    fun eth0SetStatic(
        host: String,
        gateway: String,
        mask: String,
        dns1: String,
        dns2: String,
        mtu: Int,
    ): String = "eth0 set static $host $gateway $mask $dns1 $dns2 $mtu"

    // ---- mode ----
    fun modeSetRover(): String = "mode set rover"

    fun modeSetRoverBase(): String = "mode set roverbase"

    fun modeSetRoverMaster(): String = "mode set rovermaster"

    fun modeSetBase(
        lat: Double,
        lon: Double,
        heightM: Double,
    ): String = "mode set base $lat $lon $heightM"

    fun modeRtcmId(value: Int): String = "mode rtcmid $value"

    fun modeCmrId(value: Int): String = "mode cmrid $value"

    // ---- ppp ----
    fun pppSet(type: PppType, on: Boolean): String =
        "ppp ${type.code} ${if (on) "on" else "off"}"

    /** SBAS is the special case — the ppp doc shows `ppp sbas <on|off> <SBAS_SYSTEM>`. */
    fun pppSbas(on: Boolean, system: SbasSystem): String =
        "ppp sbas ${if (on) "on" else "off"} ${system.code}"

    // ---- survey ----
    fun surveySetMask(elevationDeg: Int): String = "survey set mask $elevationDeg"

    // ---- coordsystem ----
    fun coordsystemSetGeoid(geoid: Geoid): String = "coordsystem set geoid ${geoid.code}"

    fun coordsystemSetUndulation(valueM: Double): String = "coordsystem set undulation $valueM"

    // ---- input (rover correction source) ----
    fun inputSetBluetooth(): String = "input set bluetooth"

    fun inputSetTcpClient(host: String, port: Int): String =
        "input set tcpclient $host $port"

    fun inputSetTcpServer(port: Int): String = "input set tcpserver $port"

    fun inputSetNtripClient(
        host: String,
        port: Int,
        endpoint: String,
        login: String,
        pass: String,
    ): String =
        "input set ntripclient $host $port ${quoteIfNeeded(endpoint)} " +
            "${quoteIfNeeded(login)} ${quoteIfNeeded(pass)} gppga"

    fun inputSetCom(port: ComPort, baudrate: Int): String =
        "input set com${port.index} $baudrate"

    fun inputSetUhf(frequencyMHz: Double, protocol: UhfProtocol, baudrate: Int): String =
        "input set uhf $frequencyMHz ${protocol.code} $baudrate"

    fun inputSetGsmTcpClient(host: String, port: Int): String =
        "input set gsmtcpclient $host $port"

    fun inputSetGsmNtripClient(
        host: String,
        port: Int,
        endpoint: String,
        login: String,
        pass: String,
    ): String =
        "input set gsmntripclient $host $port ${quoteIfNeeded(endpoint)} " +
            "${quoteIfNeeded(login)} ${quoteIfNeeded(pass)} gppga"

    // ---- output: list / clear / remove ----
    fun outputListStream(): String = "output list stream"

    fun outputListMessage(): String = "output list message"

    fun outputClearMessage(): String = "output clear message"

    fun outputClearStream(): String = "output clear stream"

    fun outputRemoveStream(index: Int): String = "output remove stream $index"

    fun outputRemoveMessage(index: Int): String = "output remove message $index"

    // ---- output add message ----
    fun outputAddMessage(
        source: NewtonSource,
        type: NewtonMessageType,
        rate: NewtonRate,
        format: NewtonFormat,
    ): String = "output add message ${source.code} ${type.code} ${rate.code} ${format.code}"

    // ---- output add stream <SOURCES> <transport> ----
    fun outputAddStreamBluetooth(sources: List<NewtonSource>): String =
        "output add stream ${sources.toSrcField()} bluetooth"

    fun outputAddStreamTcpClient(sources: List<NewtonSource>, host: String, port: Int): String =
        "output add stream ${sources.toSrcField()} tcpclient $host $port"

    fun outputAddStreamTcpServer(sources: List<NewtonSource>, port: Int): String =
        "output add stream ${sources.toSrcField()} tcpserver $port"

    fun outputAddStreamGsmTcpClient(sources: List<NewtonSource>, host: String, port: Int): String =
        "output add stream ${sources.toSrcField()} gsmtcpclient $host $port"

    fun outputAddStreamCom(sources: List<NewtonSource>, port: ComPort, baudrate: Int): String =
        "output add stream ${sources.toSrcField()} com${port.index} $baudrate"

    fun outputAddStreamUhf(
        sources: List<NewtonSource>,
        frequencyMHz: Double,
        protocol: UhfProtocol,
        baudrate: Int,
        power: UhfPower,
    ): String =
        "output add stream ${sources.toSrcField()} uhf $frequencyMHz ${protocol.code} " +
            "$baudrate ${power.code}"

    // ---- output set correction (Табл. 2.3) ----
    fun outputSetCorrection(correction: NewtonCorrection): String =
        "output set correction ${correction.id}"

    // ---- bluetooth (radio on the receiver) ----
    fun bluetoothSet(on: Boolean): String = "bluetooth set ${if (on) "on" else "off"}"

    /**
     * Bluetooth bridge — pipe data from a non-BT transport through the BT radio.
     * The doc's table is asymmetric: the `on/off` keyword always comes first,
     * then the optional target transport. We keep all variations as separate
     * builders so the call site reads naturally.
     */
    fun bluetoothBridge(on: Boolean): String =
        "bluetooth bridge ${if (on) "on" else "off"}"

    fun bluetoothBridgeTcpServer(on: Boolean, port: Int): String =
        "bluetooth bridge ${if (on) "on" else "off"} tcpserver $port"

    fun bluetoothBridgeTcpClient(on: Boolean, host: String, port: Int): String =
        "bluetooth bridge ${if (on) "on" else "off"} tcpclient $host $port"

    fun bluetoothBridgeUhf(
        on: Boolean,
        frequencyMHz: Double,
        protocol: UhfProtocol,
        baudrate: Int,
        power: UhfPower,
    ): String =
        "bluetooth bridge ${if (on) "on" else "off"} uhf $frequencyMHz " +
            "${protocol.code} $baudrate ${power.code}"

    fun bluetoothBridgeCom(on: Boolean, port: ComPort, baudrate: Int): String =
        "bluetooth bridge ${if (on) "on" else "off"} com${port.index} $baudrate"

    // ---- gsm ----
    fun gsmSet(
        on: Boolean,
        apn1: String,
        apn2: String? = null,
    ): String =
        buildString {
            append("gsm set ${if (on) "on" else "off"} ")
            append(quoteIfNeeded(apn1))
            if (apn2 != null) append(" ").append(quoteIfNeeded(apn2))
        }

    // ---- config ----
    fun configReset(): String = "config reset"   // immediate, not queued

    // ---- helpers ----
    private fun List<NewtonSource>.toSrcField(): String = joinToString(SRC_SEP.toString()) { it.code }

    private fun quoteIfNeeded(value: String): String =
        if (value.contains(' ') || value.isEmpty()) "\"$value\"" else value
}
