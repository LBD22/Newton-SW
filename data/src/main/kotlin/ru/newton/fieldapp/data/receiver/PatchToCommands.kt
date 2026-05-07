package ru.newton.fieldapp.data.receiver

import ru.newton.fieldapp.domain.receiver.BluetoothBridgeSetting
import ru.newton.fieldapp.domain.receiver.BluetoothBridgeTarget
import ru.newton.fieldapp.domain.receiver.GeoidSetting
import ru.newton.fieldapp.domain.receiver.GsmSetting
import ru.newton.fieldapp.domain.receiver.InputConfig
import ru.newton.fieldapp.domain.receiver.OutputMessageConfig
import ru.newton.fieldapp.domain.receiver.OutputStreamConfig
import ru.newton.fieldapp.domain.receiver.PppSetting
import ru.newton.fieldapp.domain.receiver.ReceiverConfigPatch
import ru.newton.fieldapp.domain.receiver.RoverMode
import ru.newton.fieldapp.domain.receiver.StreamTarget
import ru.newton.fieldapp.gnss.command.ComPort
import ru.newton.fieldapp.gnss.command.Geoid
import ru.newton.fieldapp.gnss.command.NewtonCommandBuilder
import ru.newton.fieldapp.gnss.command.NewtonCorrection
import ru.newton.fieldapp.gnss.command.NewtonFormat
import ru.newton.fieldapp.gnss.command.NewtonMessageType
import ru.newton.fieldapp.gnss.command.NewtonRate
import ru.newton.fieldapp.gnss.command.NewtonSource
import ru.newton.fieldapp.gnss.command.PppType
import ru.newton.fieldapp.gnss.command.SbasSystem
import ru.newton.fieldapp.gnss.command.UhfPower
import ru.newton.fieldapp.gnss.command.UhfProtocol

/**
 * Translates a [ReceiverConfigPatch] into the linear sequence of Newton command
 * strings that, sent in order and followed by `system save`, deliver exactly
 * those changes to the receiver.
 *
 * Order matters:
 *  - mode shifts must precede correction / RTCM-id changes
 *  - `output clear` must precede `output add`s (otherwise duplicates accumulate)
 *  - `system save` is always last and persistent
 */
data class PreparedCommand(
    val command: String,
    val description: String,
)

object PatchToCommands {
    fun build(patch: ReceiverConfigPatch): List<PreparedCommand> = buildList {
        patch.roverMode?.let { mode ->
            add(
                PreparedCommand(
                    command = when (mode) {
                        RoverMode.ROVER -> NewtonCommandBuilder.modeSetRover()
                        RoverMode.ROVER_BASE -> NewtonCommandBuilder.modeSetRoverBase()
                        RoverMode.ROVER_MASTER -> NewtonCommandBuilder.modeSetRoverMaster()
                    },
                    description = "Режим: $mode",
                ),
            )
        }
        patch.baseMode?.let { base ->
            add(
                PreparedCommand(
                    command = NewtonCommandBuilder.modeSetBase(base.latitude, base.longitude, base.heightM),
                    description = "База: ${"%.6f".format(base.latitude)}, ${"%.6f".format(base.longitude)}",
                ),
            )
        }
        patch.rtcmId?.let { id ->
            add(PreparedCommand(NewtonCommandBuilder.modeRtcmId(id), "RTCM id $id"))
        }
        patch.cmrId?.let { id ->
            add(PreparedCommand(NewtonCommandBuilder.modeCmrId(id), "CMR id $id"))
        }
        patch.correctionType?.let { id ->
            val correction = NewtonCorrection.entries.firstOrNull { it.id == id }
                ?: error("Unknown correction id $id (Табл. 2.3 0..15)")
            add(
                PreparedCommand(
                    NewtonCommandBuilder.outputSetCorrection(correction),
                    "Тип поправок: ${correction.label}",
                ),
            )
        }
        patch.surveyMaskDeg?.let { deg ->
            add(PreparedCommand(NewtonCommandBuilder.surveySetMask(deg), "Маска возвышения $deg°"))
        }
        patch.input?.let { input -> add(toInputCommand(input)) }
        patch.bluetoothReceiver?.let { on ->
            add(PreparedCommand(NewtonCommandBuilder.bluetoothSet(on), if (on) "BT приёмника: вкл" else "BT приёмника: выкл"))
        }
        patch.geoid?.let { geoid ->
            val code = when (geoid) {
                GeoidSetting.WGS84 -> Geoid.WGS84
                GeoidSetting.EGM86 -> Geoid.EGM86
                GeoidSetting.USER -> Geoid.USER
            }
            add(PreparedCommand(NewtonCommandBuilder.coordsystemSetGeoid(code), "Геоид: ${code.code}"))
        }
        patch.undulationM?.let { value ->
            add(
                PreparedCommand(
                    NewtonCommandBuilder.coordsystemSetUndulation(value),
                    "Аномалия: ${"%.3f".format(value)} м",
                ),
            )
        }
        patch.ppp?.let { add(toPppCommand(it)) }
        patch.gsm?.let { add(toGsmCommand(it)) }
        patch.bluetoothBridge?.let { add(toBluetoothBridgeCommand(it)) }
        patch.outputMessages?.let { msgs ->
            add(PreparedCommand(NewtonCommandBuilder.outputClearMessage(), "Очистить сообщения"))
            for (msg in msgs) add(toOutputMessageCommand(msg))
        }
        patch.outputStreams?.let { streams ->
            add(PreparedCommand(NewtonCommandBuilder.outputClearStream(), "Очистить потоки"))
            for (stream in streams) add(toOutputStreamCommand(stream))
        }
        // Final `system save` is appended by the use case, not here — that lets
        // the use case insert pre-save validation steps if needed (e.g. dry-run mode).
    }

    private fun toInputCommand(config: InputConfig): PreparedCommand = when (config) {
        is InputConfig.Bluetooth -> PreparedCommand(
            NewtonCommandBuilder.inputSetBluetooth(),
            "Источник коррекций: Bluetooth",
        )
        is InputConfig.TcpClient -> PreparedCommand(
            NewtonCommandBuilder.inputSetTcpClient(config.host, config.port),
            "Источник: TCP-клиент ${config.host}:${config.port}",
        )
        is InputConfig.TcpServer -> PreparedCommand(
            NewtonCommandBuilder.inputSetTcpServer(config.port),
            "Источник: TCP-сервер :${config.port}",
        )
        is InputConfig.NtripClient -> PreparedCommand(
            NewtonCommandBuilder.inputSetNtripClient(
                config.host,
                config.port,
                config.endpoint,
                config.login,
                config.password,
            ),
            "Источник: NTRIP ${config.host}/${config.endpoint}",
        )
        is InputConfig.Com -> {
            val port = comPortOrFail(config.index)
            PreparedCommand(
                NewtonCommandBuilder.inputSetCom(port, config.baudrate),
                "Источник: COM${port.index} @ ${config.baudrate}",
            )
        }
        is InputConfig.Uhf -> PreparedCommand(
            NewtonCommandBuilder.inputSetUhf(
                frequencyMHz = config.frequencyMHz,
                protocol = uhfProtocolOrFail(config.protocol),
                baudrate = config.baudrate,
            ),
            "Источник: УКВ ${config.frequencyMHz} МГц, ${config.protocol} @ ${config.baudrate}",
        )
    }

    private fun toPppCommand(setting: PppSetting): PreparedCommand {
        val type = PppType.entries.firstOrNull { it.code == setting.type }
            ?: error("Unknown PPP type '${setting.type}' (Табл. 2.7)")
        return if (type == PppType.SBAS) {
            // SBAS variant requires the SBAS_SYSTEM keyword.
            val sbas = setting.sbasSystem
                ?: error("PPP SBAS требует указания системы (egnos/waas/msas/gagan)")
            val sbasEnum = SbasSystem.entries.firstOrNull { it.code == sbas }
                ?: error("Unknown SBAS system '$sbas' (Табл. 2.8)")
            PreparedCommand(
                NewtonCommandBuilder.pppSbas(setting.enabled, sbasEnum),
                "PPP SBAS ${if (setting.enabled) "вкл" else "выкл"} (${sbasEnum.code})",
            )
        } else {
            PreparedCommand(
                NewtonCommandBuilder.pppSet(type, setting.enabled),
                "PPP ${type.code} ${if (setting.enabled) "вкл" else "выкл"}",
            )
        }
    }

    private fun toOutputMessageCommand(msg: OutputMessageConfig): PreparedCommand {
        val source = NewtonSource.entries.firstOrNull { it.code == msg.source }
            ?: error("Unknown source code '${msg.source}'")
        val type = NewtonMessageType.entries.firstOrNull { it.code == msg.type }
            ?: error("Unknown message type '${msg.type}'")
        val rate = NewtonRate.entries.firstOrNull { it.code == msg.rate }
            ?: error("Unknown rate '${msg.rate}'")
        val format = NewtonFormat.entries.firstOrNull { it.code == msg.format }
            ?: error("Unknown format '${msg.format}'")
        return PreparedCommand(
            NewtonCommandBuilder.outputAddMessage(source, type, rate, format),
            "Сообщение ${type.code} ${rate.code} (${source.code})",
        )
    }

    private fun toOutputStreamCommand(stream: OutputStreamConfig): PreparedCommand {
        val sources = stream.sources.map { code ->
            NewtonSource.entries.firstOrNull { it.code == code } ?: error("Unknown source '$code'")
        }
        val srcLabel = sources.joinToString(NewtonCommandBuilder.SRC_SEP.toString()) { it.code }
        return when (val target = stream.target) {
            is StreamTarget.Bluetooth -> PreparedCommand(
                NewtonCommandBuilder.outputAddStreamBluetooth(sources),
                "Поток $srcLabel → Bluetooth",
            )
            is StreamTarget.TcpClient -> PreparedCommand(
                NewtonCommandBuilder.outputAddStreamTcpClient(sources, target.host, target.port),
                "Поток $srcLabel → TCP ${target.host}:${target.port}",
            )
            is StreamTarget.TcpServer -> PreparedCommand(
                NewtonCommandBuilder.outputAddStreamTcpServer(sources, target.port),
                "Поток $srcLabel → TCP-сервер :${target.port}",
            )
            is StreamTarget.GsmTcpClient -> PreparedCommand(
                NewtonCommandBuilder.outputAddStreamGsmTcpClient(sources, target.host, target.port),
                "Поток $srcLabel → GSM TCP ${target.host}:${target.port}",
            )
            is StreamTarget.Com -> {
                val port = comPortOrFail(target.index)
                PreparedCommand(
                    NewtonCommandBuilder.outputAddStreamCom(sources, port, target.baudrate),
                    "Поток $srcLabel → COM${port.index} @ ${target.baudrate}",
                )
            }
            is StreamTarget.Uhf -> PreparedCommand(
                NewtonCommandBuilder.outputAddStreamUhf(
                    sources = sources,
                    frequencyMHz = target.frequencyMHz,
                    protocol = uhfProtocolOrFail(target.protocol),
                    baudrate = target.baudrate,
                    power = uhfPowerOrFail(target.power),
                ),
                "Поток $srcLabel → УКВ ${target.frequencyMHz} МГц",
            )
        }
    }

    private fun toGsmCommand(setting: GsmSetting): PreparedCommand = PreparedCommand(
        NewtonCommandBuilder.gsmSet(
            on = setting.enabled,
            apn1 = setting.apn1,
            apn2 = setting.apn2,
        ),
        if (setting.enabled) {
            "GSM вкл (${setting.apn1}${setting.apn2?.let { ", $it" } ?: ""})"
        } else {
            "GSM выкл"
        },
    )

    private fun toBluetoothBridgeCommand(setting: BluetoothBridgeSetting): PreparedCommand =
        when (val t = setting.target) {
            null -> PreparedCommand(
                NewtonCommandBuilder.bluetoothBridge(setting.enabled),
                if (setting.enabled) "BT bridge вкл" else "BT bridge выкл",
            )
            is BluetoothBridgeTarget.TcpServer -> PreparedCommand(
                NewtonCommandBuilder.bluetoothBridgeTcpServer(setting.enabled, t.port),
                "BT bridge ${if (setting.enabled) "вкл" else "выкл"} → TCP-сервер :${t.port}",
            )
            is BluetoothBridgeTarget.TcpClient -> PreparedCommand(
                NewtonCommandBuilder.bluetoothBridgeTcpClient(setting.enabled, t.host, t.port),
                "BT bridge ${if (setting.enabled) "вкл" else "выкл"} → TCP ${t.host}:${t.port}",
            )
            is BluetoothBridgeTarget.Com -> {
                val port = comPortOrFail(t.index)
                PreparedCommand(
                    NewtonCommandBuilder.bluetoothBridgeCom(setting.enabled, port, t.baudrate),
                    "BT bridge ${if (setting.enabled) "вкл" else "выкл"} → COM${port.index}",
                )
            }
            is BluetoothBridgeTarget.Uhf -> PreparedCommand(
                NewtonCommandBuilder.bluetoothBridgeUhf(
                    on = setting.enabled,
                    frequencyMHz = t.frequencyMHz,
                    protocol = uhfProtocolOrFail(t.protocol),
                    baudrate = t.baudrate,
                    power = uhfPowerOrFail(t.power),
                ),
                "BT bridge ${if (setting.enabled) "вкл" else "выкл"} → УКВ ${t.frequencyMHz} МГц",
            )
        }

    private fun comPortOrFail(index: Int): ComPort = ComPort.entries.firstOrNull { it.index == index }
        ?: error("Только COM1 и COM2 поддерживаются приёмником (получено $index)")

    private fun uhfProtocolOrFail(code: String): UhfProtocol =
        UhfProtocol.entries.firstOrNull { it.code == code }
            ?: error("Неизвестный УКВ-протокол '$code' (Табл. 2.4)")

    private fun uhfPowerOrFail(code: String): UhfPower =
        UhfPower.entries.firstOrNull { it.code == code }
            ?: error("Неизвестная УКВ-мощность '$code' (Табл. 2.5)")
}
